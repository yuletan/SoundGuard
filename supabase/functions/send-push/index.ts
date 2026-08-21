// supabase/functions/send-push/index.ts
//
// Drains queued FCM push notifications so caregiver alerts reach a phone even
// when the caregiver's app/phone is closed.
//
// Triggered by:
//   - the incidents_enqueue_notification DB trigger via net.http_post (when
//     app.push_endpoint / app.push_secret DB settings are set), OR
//   - the optional pg_cron backstop (every 30s), OR
//   - a Supabase DB webhook on notifications insert.
//
// Required Supabase secrets (Project Settings > Edge Functions > Secrets):
//   FIREBASE_SERVICE_ACCOUNT  -> the FULL service-account JSON (as a string)
//   PUSH_SHARED_SECRET       -> the bearer token matching app.push_secret
//
// Env vars automatically injected by Supabase Edge Functions:
//   SUPABASE_URL, SUPABASE_SERVICE_ROLE_KEY

import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

interface ServiceAccount {
  private_key: string;
  client_email: string;
  project_id: string;
  token_uri: string;
}

const SUPABASE_URL = Deno.env.get("SUPABASE_URL") ?? "";
const SERVICE_ROLE_KEY = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "";
const PUSH_SHARED_SECRET = Deno.env.get("PUSH_SHARED_SECRET") ?? "";

let cachedSa: ServiceAccount | null = null;
function serviceAccount(): ServiceAccount {
  if (cachedSa) return cachedSa;
  const raw = Deno.env.get("FIREBASE_SERVICE_ACCOUNT");
  if (!raw) throw new Error("FIREBASE_SERVICE_ACCOUNT secret is not set");
  const parsed = JSON.parse(raw) as ServiceAccount;
  if (!parsed.private_key || !parsed.client_email || !parsed.project_id) {
    throw new Error("FIREBASE_SERVICE_ACCOUNT JSON is missing required fields");
  }
  cachedSa = { ...parsed, token_uri: parsed.token_uri || "https://oauth2.googleapis.com/token" };
  return cachedSa;
}

// --- base64url helpers ---
function b64url(input: Uint8Array | ArrayBuffer): string {
  const bytes = input instanceof Uint8Array ? input : new Uint8Array(input);
  let bin = "";
  for (const b of bytes) bin += String.fromCharCode(b);
  return btoa(bin).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}
function strToBytes(s: string): Uint8Array {
  return new TextEncoder().encode(s);
}
function pemToPkcs8Der(pem: string): ArrayBuffer {
  const body = pem
    .replace(/-----BEGIN PRIVATE KEY-----/g, "")
    .replace(/-----END PRIVATE KEY-----/g, "")
    .replace(/\s+/g, "");
  const bin = atob(body);
  const bytes = new Uint8Array(bin.length);
  for (let i = 0; i < bin.length; i++) bytes[i] = bin.charCodeAt(i);
  return bytes.buffer;
}

let cachedAccessToken: { token: string; expires: number } | null = null;

async function getAccessToken(): Promise<string> {
  if (cachedAccessToken && Date.now() < cachedAccessToken.expires - 60_000) {
    return cachedAccessToken.token;
  }
  const sa = serviceAccount();
  const now = Math.floor(Date.now() / 1000);
  const header = { alg: "RS256", typ: "JWT" };
  const payload = {
    iss: sa.client_email,
    scope: "https://www.googleapis.com/auth/firebase.messaging",
    aud: sa.token_uri,
    iat: now,
    exp: now + 3600,
  };
  const unsigned =
    b64url(strToBytes(JSON.stringify(header))) +
    "." +
    b64url(strToBytes(JSON.stringify(payload)));

  const keyData = pemToPkcs8Der(sa.private_key);
  const cryptoKey = await crypto.subtle.importKey(
    "pkcs8",
    keyData,
    { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
    false,
    ["sign"],
  );
  const sig = await crypto.subtle.sign("RSASSA-PKCS1-v1_5", cryptoKey, strToBytes(unsigned));
  const jwt = unsigned + "." + b64url(sig);

  const tokenRes = await fetch(sa.token_uri, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: `grant_type=urn:ietf:params:oauth:grant-type:jwt-bearer&assertion=${jwt}`,
  });
  if (!tokenRes.ok) {
    const t = await tokenRes.text();
    throw new Error(`FCM OAuth token fetch failed (${tokenRes.status}): ${t}`);
  }
  const tok = (await tokenRes.json()) as { access_token: string; expires_in: number };
  cachedAccessToken = {
    token: tok.access_token,
    expires: Date.now() + tok.expires_in * 1000,
  };
  return cachedAccessToken.token;
}

async function sendFcm(
  projectId: string,
  accessToken: string,
  fcmToken: string,
  title: string,
  body: string,
  data: Record<string, string>,
): Promise<boolean> {
  const res = await fetch(
    `https://fcm.googleapis.com/v1/projects/${projectId}/messages:send`,
    {
      method: "POST",
      headers: {
        Authorization: `Bearer ${accessToken}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        message: {
          token: fcmToken,
          android: { priority: "high" },
          notification: { title, body },
          data,
        },
      }),
    },
  );
  return res.ok;
}

Deno.serve(async (req) => {
  // Optional bearer auth (mirrors app.push_secret). Skip if secret unset.
  if (PUSH_SHARED_SECRET) {
    const auth = req.headers.get("Authorization") ?? "";
    if (auth !== `Bearer ${PUSH_SHARED_SECRET}`) {
      return new Response("unauthorized", { status: 401 });
    }
  }

  if (!SUPABASE_URL || !SERVICE_ROLE_KEY) {
    return new Response("Supabase env not configured", { status: 500 });
  }

  let serviceReady = true;
  let saError = "";
  try {
    serviceAccount();
  } catch (e) {
    serviceReady = false;
    saError = (e as Error).message;
  }

  const supabase = createClient(SUPABASE_URL, SERVICE_ROLE_KEY);

  // If the service account isn't configured yet, still mark queued pushes so
  // they don't pile up forever? No — leave them queued so they send once the
  // secret is added. Just report.
  if (!serviceReady) {
    return Response.json({ ok: false, skipped: true, error: saError });
  }

  // Drain up to 50 queued push notifications.
  const { data: pending, error } = await supabase
    .from("notifications")
    .select("id, incident_id, caregiver_id")
    .eq("channel", "push")
    .eq("status", "queued")
    .order("created_at", { ascending: true })
    .limit(50);

  if (error) {
    return Response.json({ ok: false, error: error.message }, { status: 500 });
  }
  if (!pending || pending.length === 0) {
    return Response.json({ ok: true, drained: 0 });
  }

  const sa = serviceAccount();
  let accessToken = "";
  try {
    accessToken = await getAccessToken();
  } catch (e) {
    return Response.json({ ok: false, error: (e as Error).message }, { status: 500 });
  }

  let sent = 0;
  let failed = 0;

  for (const n of pending) {
    // Pull incident + beneficiary name + this caregiver's tokens.
    const { data: incident } = await supabase
      .from("incidents")
      .select("label, severity, confidence, beneficiary_id")
      .eq("id", n.incident_id)
      .maybeSingle();

    let beneficiaryName = "your beneficiary";
    if (incident?.beneficiary_id) {
      const { data: prof } = await supabase
        .from("profiles")
        .select("full_name, email")
        .eq("id", incident.beneficiary_id)
        .maybeSingle();
      beneficiaryName =
        prof?.full_name?.trim() || prof?.email?.trim() || "your beneficiary";
    }

    const label = (incident?.label || "Emergency sound").trim();
    const confidence = incident?.confidence ?? 0;
    const title = `High-risk alert — ${beneficiaryName}`;
    const body = `${label} detected at ${beneficiaryName}'s place · ${
      Math.round(confidence * 100)
    }% confidence. Open to check in.`;

    const { data: tokens } = await supabase
      .from("device_push_tokens")
      .select("token")
      .eq("user_id", n.caregiver_id);

    const tokenList = (tokens ?? []).map((t: { token: string }) => t.token);
    if (tokenList.length === 0) {
      // No device registered — mark failed so we don't retry forever.
      await supabase
        .from("notifications")
        .update({ status: "failed", sent_at: new Date().toISOString() })
        .eq("id", n.id);
      failed++;
      continue;
    }

    const data = {
      incident_id: n.incident_id,
      beneficiary_id: incident?.beneficiary_id ?? "",
      sound_label: label,
      severity: incident?.severity ?? "high",
      type: "caregiver_alert",
    };

    let anySent = false;
    for (const tok of tokenList) {
      const ok = await sendFcm(sa.project_id, accessToken, tok, title, body, data);
      if (ok) anySent = true;
    }

    await supabase
      .from("notifications")
      .update({
        status: anySent ? "sent" : "failed",
        sent_at: new Date().toISOString(),
      })
      .eq("id", n.id);

    if (anySent) sent++;
    else failed++;
  }

  return Response.json({ ok: true, drained: pending.length, sent, failed });
});
