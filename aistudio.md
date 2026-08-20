# SoundGuard AI Studio Frontend Redesign Handoff

> Generated 2026-08-19 11:55:52. This file contains the complete source of every file required for the redesign handoff.

## Implementation Brief

Rebuild the SoundGuard Android frontend based on `soundguard-redesign-mockups.html` and keep all existing Supabase actions properly connected.

### Required Screens

- Beneficiary dashboard: listening hero card, caregivers list, and developer/test tools accordion.
- Caregiver dashboard: six-character pairing code input and monitored beneficiaries with live status chips.
- Incident chat: countdown banner, incident card, confidence score, photo preview, Call, and Request photo actions.
- Settings: profile, grouped permission toggles, auto-approve camera toggle, and sign out.
- Role selection/setup: two large selectable card targets.

### Design Requirements

- Use the mockup design tokens: monochrome Ink palette, background/surface colors, and isolated Danger, Warning, and Success colors.
- Preserve existing state, callbacks, navigation, Supabase integration, validation, and loading/error behavior unless the redesign requires an equivalent UI adaptation.
- Use reusable Compose components for badges, toggles, role cards, the listening hero, accordion, and rounded action buttons.

## Included Source Files

1. `soundguard-redesign-mockups.html`
2. `app/src/main/java/com/yuletan/soundguard/SoundGuardTheme.kt`
3. `app/src/main/java/com/yuletan/soundguard/UiComponents.kt`
4. `app/src/main/java/com/yuletan/soundguard/MainActivity.kt`
5. `app/src/main/java/com/yuletan/soundguard/ChatScreen.kt`
6. `app/src/main/java/com/yuletan/soundguard/ChatListScreen.kt`
7. `app/src/main/java/com/yuletan/soundguard/IncidentStateMachine.kt`
8. `app/src/main/java/com/yuletan/soundguard/ChatMessage.kt`
9. `app/src/main/java/com/yuletan/soundguard/ChatRepository.kt`
10. `app/src/main/java/com/yuletan/soundguard/ProfileClient.kt`
11. `app/src/main/java/com/yuletan/soundguard/CareClient.kt`

## Complete Source

### `soundguard-redesign-mockups.html`

```html
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8" />
<meta name="viewport" content="width=device-width, initial-scale=1.0" />
<title>SoundGuard — Redesign Direction</title>
<style>
  :root{
    --ink-900:#111111; --ink-700:#404040; --ink-500:#767676; --ink-300:#c2c2c2; --ink-100:#e8e8e6;
    --surface:#ffffff; --bg:#f6f6f4; --surface-variant:#efefed;
    --danger:#c62828; --danger-surface:#fdecea;
    --warning:#a15c00; --warning-surface:#fff4e1;
    --success:#2e7d32; --success-surface:#eaf5ea;
  }
  *{box-sizing:border-box;}
  body{
    margin:0; background:#e9e9e6; color:var(--ink-900);
    font-family:Roboto,-apple-system,BlinkMacSystemFont,"Segoe UI",Helvetica,Arial,sans-serif;
    padding:48px 24px 100px;
  }
  button, input, label{ font-family:inherit; }
  @media (prefers-reduced-motion: reduce){
    *,*::before,*::after{ transition:none !important; animation:none !important; }
  }
  button:focus-visible, summary:focus-visible, label:focus-visible, input:focus-visible{
    outline:2px solid var(--ink-900); outline-offset:2px;
  }

  .page-head{max-width:1180px;margin:0 auto 40px;}
  .page-head h1{font-size:26px;margin:0 0 8px;letter-spacing:-.01em;}
  .page-head p{color:var(--ink-700);font-size:14.5px;line-height:1.55;max-width:680px;margin:0 0 20px;}
  .legend{display:flex;flex-wrap:wrap;gap:8px;}
  .swatch{display:flex;align-items:center;gap:7px;background:var(--surface);border:1px solid var(--ink-100);
    padding:5px 12px 5px 5px;border-radius:999px;font-size:11.5px;color:var(--ink-700);}
  .swatch i{width:16px;height:16px;border-radius:50%;display:block;flex-shrink:0;}

  .frames{max-width:1180px;margin:0 auto;display:flex;flex-wrap:wrap;gap:40px;}
  .frame-block{width:296px;}
  .frame-num{display:inline-flex;align-items:center;justify-content:center;width:20px;height:20px;
    border-radius:50%;background:var(--ink-900);color:#fff;font-size:11px;font-weight:700;margin-right:8px;}
  .frame-title{font-size:13.5px;font-weight:700;margin:0 0 2px;display:flex;align-items:center;}
  .frame-sub{font-size:12px;color:var(--ink-500);margin:0 0 12px 28px;}

  .phone{width:296px;height:610px;background:var(--ink-900);border-radius:36px;padding:9px;
    box-shadow:0 24px 48px -18px rgba(0,0,0,.4);}
  .screen{width:100%;height:100%;background:var(--surface);border-radius:27px;overflow:hidden;
    display:flex;flex-direction:column;position:relative;}
  .statusbar{height:26px;flex-shrink:0;display:flex;align-items:center;justify-content:space-between;
    padding:0 18px;font-size:10.5px;font-weight:700;color:var(--ink-900);}
  .topbar{flex-shrink:0;display:flex;align-items:center;gap:10px;padding:8px 14px 12px;
    border-bottom:1px solid var(--ink-100);}
  .avatar{width:34px;height:34px;border-radius:50%;background:var(--ink-900);color:#fff;
    display:flex;align-items:center;justify-content:center;font-weight:700;font-size:13px;flex-shrink:0;}
  .topbar-title{font-size:14px;font-weight:700;line-height:1.15;}
  .topbar-sub{font-size:11px;color:var(--ink-500);}
  .icon-btn{margin-left:auto;width:30px;height:30px;border-radius:50%;display:flex;align-items:center;
    justify-content:center;color:var(--ink-700);flex-shrink:0;background:transparent;border:none;padding:0;}
  .icon-btn svg{width:17px;height:17px;}
  .back-btn{width:28px;height:28px;flex-shrink:0;color:var(--ink-900);background:transparent;border:none;padding:0;}
  .back-btn svg{width:19px;height:19px;}

  .content{flex:1;overflow:hidden;padding:12px;display:flex;flex-direction:column;gap:10px;background:var(--bg);}

  .card{background:var(--surface);border:1px solid var(--ink-100);border-radius:18px;padding:14px;}
  .card-title{margin:0 0 2px;font-size:13px;font-weight:700;}
  .card-desc{margin:0;font-size:11.5px;color:var(--ink-500);line-height:1.4;}

  .btn{border-radius:999px;padding:9px 16px;font-size:12.5px;font-weight:700;border:none;
    display:inline-flex;align-items:center;justify-content:center;gap:6px;cursor:pointer;}
  .btn svg{width:15px;height:15px;}
  .btn-primary{background:var(--ink-900);color:#fff;}
  .btn-outline{background:transparent;border:1.4px solid var(--ink-900);color:var(--ink-900);}
  .btn-outline-muted{background:transparent;border:1.4px solid var(--ink-300);color:var(--ink-700);}
  .btn-full{width:100%;}
  .btn-row{display:flex;gap:8px;}
  .btn-row .btn{flex:1;}

  .row{display:flex;align-items:center;gap:10px;}
  .between{justify-content:space-between;}

  .chip{font-size:10px;font-weight:800;letter-spacing:.02em;padding:3px 9px;border-radius:999px;
    display:inline-block;text-transform:uppercase;}
  .chip-warning{background:var(--warning-surface);color:var(--warning);}
  .chip-danger{background:var(--danger-surface);color:var(--danger);}
  .chip-success{background:var(--success-surface);color:var(--success);}
  .chip-neutral{background:var(--surface-variant);color:var(--ink-700);}

  .list-row{display:flex;align-items:center;gap:10px;padding:9px 0;}
  .list-row + .list-row{border-top:1px solid var(--ink-100);}
  .list-row .avatar{width:32px;height:32px;font-size:12px;}
  .list-row-text{flex:1;min-width:0;}
  .list-row-name{font-size:12.5px;font-weight:700;}
  .list-row-sub{font-size:11px;color:var(--ink-500);}
  .row-actions{display:flex;gap:4px;flex-shrink:0;}
  .mini-icon{width:28px;height:28px;border-radius:50%;display:flex;align-items:center;justify-content:center;
    color:var(--ink-700);flex-shrink:0;background:transparent;border:none;}
  .mini-icon svg{width:15px;height:15px;}

  .ring{width:56px;height:56px;border-radius:50%;border:2.5px solid var(--ink-900);
    display:flex;align-items:center;justify-content:center;flex-shrink:0;}
  .ring svg{width:24px;height:24px;color:var(--ink-900);}

  details.accordion{background:var(--surface);border:1px solid var(--ink-100);border-radius:18px;
    padding:0;overflow:hidden;}
  details.accordion summary{list-style:none;padding:13px 14px;font-size:12.5px;font-weight:700;
    display:flex;align-items:center;cursor:pointer;color:var(--ink-700);}
  details.accordion summary::-webkit-details-marker{display:none;}
  details.accordion summary .chev{margin-left:auto;transition:transform .15s;color:var(--ink-500);}
  details.accordion summary .chev svg{width:15px;height:15px;}
  details.accordion[open] summary .chev{transform:rotate(180deg);}
  .accordion-body{padding:0 14px 14px;display:flex;flex-direction:column;gap:8px;}
  .test-btn{display:flex;align-items:center;justify-content:space-between;border:1.4px solid var(--ink-300);
    border-radius:12px;padding:9px 12px;font-size:12px;font-weight:600;color:var(--ink-700);}

  .switch{position:relative;display:inline-block;width:38px;height:22px;flex-shrink:0;}
  .switch input{position:absolute;opacity:0;width:1px;height:1px;}
  .switch .track{position:absolute;inset:0;background:var(--ink-300);border-radius:999px;transition:.15s;}
  .switch .track::after{content:"";position:absolute;top:2px;left:2px;width:18px;height:18px;
    background:#fff;border-radius:50%;transition:.15s;box-shadow:0 1px 2px rgba(0,0,0,.25);}
  .switch input:checked + .track{background:var(--ink-900);}
  .switch input:checked + .track::after{left:18px;}
  .switch input:focus-visible + .track{outline:2px solid var(--ink-900);outline-offset:2px;}

  .settings-row{display:flex;align-items:center;gap:11px;padding:10px 0;}
  .settings-row + .settings-row{border-top:1px solid var(--ink-100);}
  .settings-row .s-icon{width:30px;height:30px;border-radius:50%;background:var(--surface-variant);
    display:flex;align-items:center;justify-content:center;color:var(--ink-700);flex-shrink:0;}
  .settings-row .s-icon svg{width:15px;height:15px;}
  .settings-row-text{flex:1;min-width:0;}
  .settings-row-label{font-size:12.5px;font-weight:600;}
  .settings-row-val{font-size:11px;color:var(--ink-500);}
  .settings-row-desc{font-size:10.5px;color:var(--ink-500);margin-top:2px;line-height:1.4;}
  .chev-icon{color:var(--ink-300);}
  .chev-icon svg{width:15px;height:15px;}
  .section-label{font-size:10.5px;font-weight:800;letter-spacing:.06em;color:var(--ink-500);
    text-transform:uppercase;margin:6px 2px 0;}

  .banner{display:flex;align-items:center;gap:8px;padding:9px 12px;border-radius:12px;
    background:var(--danger-surface);color:var(--danger);font-size:11.5px;font-weight:700;}
  .banner .dot{width:7px;height:7px;border-radius:50%;background:var(--danger);flex-shrink:0;}

  .bubble{background:var(--surface);border:1px solid var(--ink-100);border-left:3px solid var(--ink-300);
    border-radius:14px;padding:10px 12px;}
  .bubble.b-danger{border-left-color:var(--danger);}
  .bubble.b-success{border-left-color:var(--success);}
  .bubble-meta{display:flex;align-items:center;gap:6px;margin-bottom:4px;flex-wrap:wrap;}
  .bubble-title{font-size:12.5px;font-weight:700;margin:2px 0 1px;}
  .bubble-sub{font-size:11px;color:var(--ink-500);}
  .bubble-time{font-size:10px;color:var(--ink-300);margin-left:auto;}
  .photo-thumb{width:40px;height:40px;border-radius:10px;flex-shrink:0;
    background:linear-gradient(135deg,var(--ink-300),var(--ink-100));
    display:flex;align-items:center;justify-content:center;color:#fff;}
  .photo-thumb svg{width:16px;height:16px;}

  .bottomnav{flex-shrink:0;display:flex;border-top:1px solid var(--ink-100);background:var(--surface);}
  .nav-item{flex:1;display:flex;flex-direction:column;align-items:center;gap:2px;padding:9px 0 8px;
    color:var(--ink-300);}
  .nav-item.active{color:var(--ink-900);}
  .nav-item svg{width:19px;height:19px;}
  .nav-item span{font-size:9.5px;font-weight:700;}

  .field{border:1.4px solid var(--ink-300);border-radius:12px;padding:10px 12px;}
  .field-label{font-size:9.5px;color:var(--ink-500);font-weight:700;text-transform:uppercase;letter-spacing:.04em;}
  .field-placeholder{font-size:12.5px;color:var(--ink-300);margin-top:2px;}

  .role-content{padding:26px 18px;}
  .role-h1{font-size:18px;font-weight:800;margin:0 0 4px;text-align:center;}
  .role-p{font-size:12px;color:var(--ink-500);text-align:center;margin:0 0 22px;}
  input[type=radio]{position:absolute;opacity:0;width:1px;height:1px;}
  .role-card{display:block;border:1.6px solid var(--ink-300);border-radius:18px;padding:16px;margin-bottom:12px;cursor:pointer;}
  .role-card .r-icon{width:36px;height:36px;border-radius:50%;border:1.6px solid var(--ink-300);
    display:flex;align-items:center;justify-content:center;margin-bottom:10px;}
  .role-card .r-icon svg{width:18px;height:18px;}
  .role-card .r-title{font-size:13.5px;font-weight:700;margin-bottom:3px;}
  .role-card .r-desc{font-size:11px;color:var(--ink-500);line-height:1.4;}
  input:checked + .role-card{background:var(--ink-900);border-color:var(--ink-900);}
  input:checked + .role-card .r-icon{border-color:rgba(255,255,255,.4);}
  input:checked + .role-card .r-icon svg{color:#fff;}
  input:checked + .role-card .r-title{color:#fff;}
  input:checked + .role-card .r-desc{color:#cfcfcf;}
</style>
</head>
<body>

<div class="page-head">
  <h1>SoundGuard — redesign direction</h1>
  <p>Monochrome for everything, with three colors reserved strictly for incident state. Nothing decorative is ever red, amber, or green — so when one of those colors shows up, it means something. Try the pieces below: the accordion expands, the switches toggle, the role cards select.</p>
  <div class="legend">
    <div class="swatch"><i style="background:#111111"></i>Ink 900 — text, primary buttons</div>
    <div class="swatch"><i style="background:#767676"></i>Ink 500 — secondary text</div>
    <div class="swatch"><i style="background:#f6f6f4;border:1px solid #ddd"></i>Background</div>
    <div class="swatch"><i style="background:#c62828"></i>Danger — active / escalated only</div>
    <div class="swatch"><i style="background:#a15c00"></i>Warning — pending only</div>
    <div class="swatch"><i style="background:#2e7d32"></i>Success — resolved only</div>
  </div>
</div>

<div class="frames">

  <div class="frame-block">
    <p class="frame-title"><span class="frame-num">1</span>Beneficiary dashboard</p>
    <p class="frame-sub">One hero status, one caregiver list, dev tools tucked away</p>
    <div class="phone"><div class="screen">
      <div class="statusbar"><span>9:41</span><span>●●●</span></div>
      <div class="topbar">
        <div class="avatar">J</div>
        <div><div class="topbar-title">James</div><div class="topbar-sub">Beneficiary</div></div>
        <button class="icon-btn" aria-label="Settings"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"><line x1="4" y1="7" x2="20" y2="7"/><circle cx="9" cy="7" r="2"/><line x1="4" y1="12" x2="20" y2="12"/><circle cx="15" cy="12" r="2"/><line x1="4" y1="17" x2="20" y2="17"/><circle cx="11" cy="17" r="2"/></svg></button>
      </div>
      <div class="content">
        <div class="card">
          <div class="row" style="margin-bottom:10px;">
            <div class="ring"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"><rect x="9" y="3" width="6" height="10" rx="3"/><path d="M6 11a6 6 0 0012 0"/><line x1="12" y1="17" x2="12" y2="21"/><line x1="9" y1="21" x2="15" y2="21"/></svg></div>
            <div>
              <div class="card-title">Listening</div>
              <div class="card-desc">All quiet right now · <u>Details</u></div>
            </div>
          </div>
          <button class="btn btn-outline-muted btn-full">Stop monitoring</button>
        </div>

        <div class="card">
          <div class="row between" style="margin-bottom:4px;">
            <div class="card-title">Your caregivers</div>
            <div class="card-desc" style="color:var(--ink-900);font-weight:700;">+ Add</div>
          </div>
          <div class="list-row">
            <div class="avatar">Y</div>
            <div class="list-row-text">
              <div class="list-row-name">Yo <span class="chip chip-neutral" style="margin-left:4px;">Primary</span></div>
              <div class="list-row-sub">Tap to open chat</div>
            </div>
            <div class="row-actions">
              <button class="mini-icon" aria-label="Call"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"><path d="M6 3c1 2 1.5 3.5 1 4.5-.5 1-2 1.5-2 2.5 0 2.5 4.5 7 7 7 1 0 1.5-1.5 2.5-2 1-.5 2.5 0 4.5 1 .3 2-1 4-3 4-7 0-14-7-14-14 0-2 2-3.3 4-3z"/></svg></button>
              <button class="mini-icon" aria-label="More options"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="5" r="1.4"/><circle cx="12" cy="12" r="1.4"/><circle cx="12" cy="19" r="1.4"/></svg></button>
            </div>
          </div>
        </div>

        <details class="accordion">
          <summary>Developer &amp; test tools <span class="chev"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"><path d="M6 9l6 6 6-6"/></svg></span></summary>
          <div class="accordion-body">
            <div class="test-btn">Simulate glass break <span class="chip chip-neutral">Test</span></div>
            <div class="test-btn">Simulate smoke alarm <span class="chip chip-neutral">Test</span></div>
          </div>
        </details>
      </div>
      <div class="bottomnav">
        <div class="nav-item active"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M4 11L12 4l8 7M6 10v9h12v-9"/></svg><span>Home</span></div>
        <div class="nav-item"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M4 5h16v11H8l-4 4V5z"/></svg><span>Chat</span></div>
        <div class="nav-item"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><circle cx="9" cy="9" r="3"/><path d="M4 20c0-3 2-5 5-5s5 2 5 5"/><circle cx="17" cy="10" r="2.3"/><path d="M15.5 20c0-2.3 1-3.8 2.8-4.3"/></svg><span>Caregivers</span></div>
        <div class="nav-item"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><line x1="4" y1="7" x2="20" y2="7"/><circle cx="9" cy="7" r="2"/><line x1="4" y1="12" x2="20" y2="12"/><circle cx="15" cy="12" r="2"/><line x1="4" y1="17" x2="20" y2="17"/><circle cx="11" cy="17" r="2"/></svg><span>Settings</span></div>
      </div>
    </div></div>
  </div>

  <div class="frame-block">
    <p class="frame-title"><span class="frame-num">2</span>Caregiver dashboard</p>
    <p class="frame-sub">Status is visible before you even open a chat</p>
    <div class="phone"><div class="screen">
      <div class="statusbar"><span>9:41</span><span>●●●</span></div>
      <div class="topbar">
        <div class="avatar">Y</div>
        <div><div class="topbar-title">Yo</div><div class="topbar-sub">Caregiver</div></div>
        <button class="icon-btn" aria-label="Settings"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"><line x1="4" y1="7" x2="20" y2="7"/><circle cx="9" cy="7" r="2"/><line x1="4" y1="12" x2="20" y2="12"/><circle cx="15" cy="12" r="2"/><line x1="4" y1="17" x2="20" y2="17"/><circle cx="11" cy="17" r="2"/></svg></button>
      </div>
      <div class="content">
        <div class="card">
          <div class="card-title" style="margin-bottom:8px;">Connect a beneficiary</div>
          <div class="field" style="margin-bottom:8px;">
            <div class="field-label">6-character code</div>
            <div class="field-placeholder">— — — — — —</div>
          </div>
          <button class="btn btn-primary btn-full" style="margin-bottom:8px;">Connect</button>
          <div class="card-desc" style="text-align:center;text-decoration:underline;">Scan QR instead</div>
        </div>

        <div class="card">
          <div class="card-title" style="margin-bottom:4px;">People you monitor</div>
          <div class="list-row">
            <div class="avatar">J</div>
            <div class="list-row-text">
              <div class="list-row-name">James</div>
              <div class="list-row-sub"><span class="chip chip-warning">Awaiting response</span></div>
            </div>
            <div class="row-actions">
              <button class="mini-icon" aria-label="Call"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"><path d="M6 3c1 2 1.5 3.5 1 4.5-.5 1-2 1.5-2 2.5 0 2.5 4.5 7 7 7 1 0 1.5-1.5 2.5-2 1-.5 2.5 0 4.5 1 .3 2-1 4-3 4-7 0-14-7-14-14 0-2 2-3.3 4-3z"/></svg></button>
              <button class="mini-icon" aria-label="More options"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="5" r="1.4"/><circle cx="12" cy="12" r="1.4"/><circle cx="12" cy="19" r="1.4"/></svg></button>
            </div>
          </div>
        </div>
      </div>
      <div class="bottomnav">
        <div class="nav-item active"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M4 11L12 4l8 7M6 10v9h12v-9"/></svg><span>Home</span></div>
        <div class="nav-item"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M4 5h16v11H8l-4 4V5z"/></svg><span>Chat</span></div>
        <div class="nav-item"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><circle cx="9" cy="9" r="3"/><path d="M4 20c0-3 2-5 5-5s5 2 5 5"/><circle cx="17" cy="10" r="2.3"/><path d="M15.5 20c0-2.3 1-3.8 2.8-4.3"/></svg><span>People</span></div>
        <div class="nav-item"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><line x1="4" y1="7" x2="20" y2="7"/><circle cx="9" cy="7" r="2"/><line x1="4" y1="12" x2="20" y2="12"/><circle cx="15" cy="12" r="2"/><line x1="4" y1="17" x2="20" y2="17"/><circle cx="11" cy="17" r="2"/></svg><span>Settings</span></div>
      </div>
    </div></div>
  </div>

  <div class="frame-block">
    <p class="frame-title"><span class="frame-num">3</span>Chat</p>
    <p class="frame-sub">Status chips instead of raw state, color only where it means something</p>
    <div class="phone"><div class="screen">
      <div class="statusbar"><span>9:41</span><span>●●●</span></div>
      <div class="topbar">
        <button class="back-btn" aria-label="Back"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M15 6l-6 6 6 6"/></svg></button>
        <div class="avatar">J</div>
        <div><div class="topbar-title">James</div><div class="topbar-sub">Active 2m ago</div></div>
        <button class="icon-btn" aria-label="Request photo"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="7" width="18" height="13" rx="2"/><path d="M8 7l1.5-2h5L16 7"/><circle cx="12" cy="13.5" r="3.3"/></svg></button>
        <button class="icon-btn" aria-label="Call"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"><path d="M6 3c1 2 1.5 3.5 1 4.5-.5 1-2 1.5-2 2.5 0 2.5 4.5 7 7 7 1 0 1.5-1.5 2.5-2 1-.5 2.5 0 4.5 1 .3 2-1 4-3 4-7 0-14-7-14-14 0-2 2-3.3 4-3z"/></svg></button>
      </div>
      <div class="content">
        <div class="banner"><span class="dot"></span>Active incident · escalates in 1:42</div>

        <div class="bubble b-danger">
          <div class="bubble-meta"><span class="chip chip-danger">Escalated</span><span class="bubble-time">9:16 AM</span></div>
          <div class="bubble-title">Glass break detected</div>
          <div class="bubble-sub">Confidence 91%</div>
        </div>

        <div class="bubble" style="display:flex;align-items:center;gap:10px;">
          <div class="photo-thumb"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="7" width="18" height="13" rx="2"/><circle cx="12" cy="13.5" r="3.3"/></svg></div>
          <div><div class="bubble-title" style="margin:0;">Verification photo</div><div class="bubble-sub">9:29 AM</div></div>
        </div>

        <div class="bubble b-success">
          <div class="bubble-meta"><span class="chip chip-success">Resolved</span><span class="chip chip-neutral">Test</span><span class="bubble-time">5:46 PM</span></div>
          <div class="bubble-title">Smoke alarm</div>
          <div class="bubble-sub">Confidence 94%</div>
        </div>
      </div>
      <div style="padding:10px 12px 14px;border-top:1px solid var(--ink-100);flex-shrink:0;">
        <div class="btn-row">
          <button class="btn btn-outline">Call</button>
          <button class="btn btn-primary">Request photo</button>
        </div>
      </div>
    </div></div>
  </div>

  <div class="frame-block">
    <p class="frame-title"><span class="frame-num">4</span>Settings</p>
    <p class="frame-sub">Real toggle states, sign-out isolated at the bottom</p>
    <div class="phone"><div class="screen">
      <div class="statusbar"><span>9:41</span><span>●●●</span></div>
      <div class="topbar">
        <button class="back-btn" aria-label="Back"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M15 6l-6 6 6 6"/></svg></button>
        <div class="topbar-title">Settings</div>
      </div>
      <div class="content" style="gap:2px;">
        <div class="section-label">Profile</div>
        <div class="card" style="padding:2px 14px;">
          <div class="settings-row">
            <div class="settings-row-text"><div class="settings-row-label">Full name</div><div class="settings-row-val">James</div></div>
            <div class="chev-icon"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M9 6l6 6-6 6"/></svg></div>
          </div>
          <div class="settings-row">
            <div class="settings-row-text"><div class="settings-row-label">Emergency phone</div><div class="settings-row-val">+65 8768 0000</div></div>
            <div class="chev-icon"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M9 6l6 6-6 6"/></svg></div>
          </div>
        </div>

        <div class="section-label">Permissions</div>
        <div class="card" style="padding:2px 14px;">
          <div class="settings-row">
            <div class="s-icon"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"><rect x="9" y="3" width="6" height="10" rx="3"/><path d="M6 11a6 6 0 0012 0"/></svg></div>
            <div class="settings-row-text"><div class="settings-row-label">Microphone</div></div>
            <label class="switch"><input type="checkbox" checked><span class="track"></span></label>
          </div>
          <div class="settings-row">
            <div class="s-icon"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"><path d="M12 3a5 5 0 00-5 5v3.5L5 15h14l-2-3.5V8a5 5 0 00-5-5z"/><path d="M10 18a2 2 0 004 0"/></svg></div>
            <div class="settings-row-text"><div class="settings-row-label">Notifications</div></div>
            <label class="switch"><input type="checkbox" checked><span class="track"></span></label>
          </div>
        </div>

        <div class="section-label">Safety</div>
        <div class="card" style="padding:10px 14px;">
          <div class="row between">
            <div class="settings-row-text">
              <div class="settings-row-label">Auto-approve camera requests</div>
              <div class="settings-row-desc">Skips your confirmation during an active incident.</div>
            </div>
            <label class="switch"><input type="checkbox"><span class="track"></span></label>
          </div>
        </div>

        <div style="margin-top:auto;padding-top:10px;border-top:1px solid var(--ink-100);">
          <div class="card-desc" style="text-align:center;margin-bottom:8px;">Switch to caregiver mode</div>
          <div style="text-align:center;font-size:12.5px;font-weight:700;color:var(--danger);">Sign out</div>
        </div>
      </div>
    </div></div>
  </div>

  <div class="frame-block">
    <p class="frame-title"><span class="frame-num">5</span>Role selection</p>
    <p class="frame-sub">Two big tap targets, not two small radio buttons</p>
    <div class="phone"><div class="screen">
      <div class="statusbar"><span>9:41</span><span>●●●</span></div>
      <div class="content role-content" style="padding-top:38px;">
        <div class="role-h1">Who's this for?</div>
        <div class="role-p">You can invite the other side to connect once you're set up.</div>

        <input type="radio" name="role" id="r1" checked>
        <label for="r1" class="role-card">
          <div class="r-icon"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"><path d="M12 21s-7-4.5-9-9c-1.4-3 1-6 4-6 2 0 3.5 1.3 5 3 1.5-1.7 3-3 5-3 3 0 5.4 3 4 6-2 4.5-9 9-9 9z"/></svg></div>
          <div class="r-title">I want to be monitored</div>
          <div class="r-desc">Emergency sounds get detected and sent to family who can check on you.</div>
        </label>

        <input type="radio" name="role" id="r2">
        <label for="r2" class="role-card">
          <div class="r-icon"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"><circle cx="9" cy="8" r="3"/><path d="M3 20c0-3.3 2.7-6 6-6s6 2.7 6 6"/><circle cx="18" cy="9" r="2.4"/><path d="M16.2 20c0-2.6 1.1-4.4 3.3-5"/></svg></div>
          <div class="r-title">I'm a caregiver</div>
          <div class="r-desc">Get notified if someone you care for may be in danger.</div>
        </label>

        <button class="btn btn-primary btn-full" style="margin-top:6px;">Continue</button>
      </div>
    </div></div>
  </div>

</div>
</body>
</html>
```

### `app/src/main/java/com/yuletan/soundguard/SoundGuardTheme.kt`

```kotlin
package com.yuletan.soundguard

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * SoundGuard palette — monochrome-first, with a small, strictly-reserved
 * semantic set for incident severity.
 *
 * Rule of thumb: if a color isn't describing a real incident state
 * (via IncidentStatus below), it should be one of the Ink/Surface tokens.
 * Never use Danger/Warning/Success decoratively, for branding, or on
 * test/demo buttons — that's what makes them mean something when they do show up.
 */

// ---- Neutral scale — ~95% of the UI lives here ----
val Ink900 = Color(0xFF111111)          // primary text, primary button fill
val Ink700 = Color(0xFF404040)          // secondary text
val Ink500 = Color(0xFF767676)          // tertiary text, inactive icons
val Ink300 = Color(0xFFC2C2C2)          // borders, dividers, disabled state
val Ink100 = Color(0xFFE8E8E6)          // hairline dividers
val SurfaceWhite = Color(0xFFFFFFFF)    // cards
val BackgroundGray = Color(0xFFF6F6F4)  // screen background
val SurfaceVariant = Color(0xFFEFEFED)  // chips, subtle fills

// ---- Semantic — reserved for IncidentStatus only, nothing else ----
val DangerRed = Color(0xFFC62828)
val DangerSurface = Color(0xFFFDECEA)
val DangerSurfaceDark = Color(0xFF381414)
val DangerRedDark = Color(0xFFF87171)

val WarningAmber = Color(0xFFA15C00)
val WarningSurface = Color(0xFFFFF4E1)
val WarningSurfaceDark = Color(0xFF3B2915)
val WarningAmberDark = Color(0xFFFBBF24)

val SuccessGreen = Color(0xFF2E7D32)
val SuccessSurface = Color(0xFFEAF5EA)
val SuccessSurfaceDark = Color(0xFF14301D)
val SuccessGreenDark = Color(0xFF4ADE80)

// ---- Dark theme neutrals ----
val DarkBackground = Color(0xFF121212)
val DarkSurface = Color(0xFF1E1E1E)
val DarkSurfaceVariant = Color(0xFF2A2A2A)
val DarkInk900 = Color(0xFFF5F5F4)
val DarkInk700 = Color(0xFFD4D4D4)
val DarkInk500 = Color(0xFF9E9E9E)
val DarkInk300 = Color(0xFF4B4B4B)
val DarkInk100 = Color(0xFF2E2E2E)

val LightColors = lightColorScheme(
    primary = Ink900,
    onPrimary = Color.White,
    background = BackgroundGray,
    onBackground = Ink900,
    surface = SurfaceWhite,
    onSurface = Ink900,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = Ink700,
    outline = Ink300,
    outlineVariant = Ink100,
    error = DangerRed,
    errorContainer = DangerSurface,
    onErrorContainer = DangerRed,
    primaryContainer = SurfaceVariant,
    onPrimaryContainer = Ink900,
    secondaryContainer = SurfaceVariant,
    onSecondaryContainer = Ink700,
)

val DarkColors = darkColorScheme(
    primary = DarkInk900,
    onPrimary = Color(0xFF111111),
    background = DarkBackground,
    onBackground = DarkInk900,
    surface = DarkSurface,
    onSurface = DarkInk900,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkInk700,
    outline = DarkInk300,
    outlineVariant = DarkInk100,
    error = DangerRedDark,
    errorContainer = DangerSurfaceDark,
    onErrorContainer = DangerRedDark,
    primaryContainer = DarkSurfaceVariant,
    onPrimaryContainer = DarkInk900,
    secondaryContainer = DarkSurfaceVariant,
    onSecondaryContainer = DarkInk700,
)

private val SoundGuardTypography = Typography().let {
    it.copy(
        headlineLarge = it.headlineLarge.copy(fontFamily = FontFamily.SansSerif, fontSize = 28.sp, fontWeight = FontWeight.Bold),
        headlineMedium = it.headlineMedium.copy(fontFamily = FontFamily.SansSerif, fontSize = 24.sp, fontWeight = FontWeight.Bold),
        headlineSmall = it.headlineSmall.copy(fontFamily = FontFamily.SansSerif, fontSize = 20.sp, fontWeight = FontWeight.Bold),
        titleLarge = it.titleLarge.copy(fontFamily = FontFamily.SansSerif, fontSize = 18.sp, fontWeight = FontWeight.Bold),
        titleMedium = it.titleMedium.copy(fontFamily = FontFamily.SansSerif, fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
        bodyLarge = it.bodyLarge.copy(fontFamily = FontFamily.SansSerif, fontSize = 16.sp),
        bodyMedium = it.bodyMedium.copy(fontFamily = FontFamily.SansSerif, fontSize = 14.sp),
        bodySmall = it.bodySmall.copy(fontFamily = FontFamily.SansSerif, fontSize = 12.sp),
        labelLarge = it.labelLarge.copy(fontFamily = FontFamily.SansSerif, fontSize = 14.sp, fontWeight = FontWeight.Bold),
        labelMedium = it.labelMedium.copy(fontFamily = FontFamily.SansSerif, fontSize = 12.sp, fontWeight = FontWeight.Medium),
        labelSmall = it.labelSmall.copy(fontFamily = FontFamily.SansSerif, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold),
    )
}

val LocalDarkTheme = staticCompositionLocalOf { false }

@Composable
fun SoundGuardTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors

    CompositionLocalProvider(LocalDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = colors,
            typography = SoundGuardTypography,
            content = content,
        )
    }
}

val ColorScheme.success: Color
    get() = if (this == DarkColors) SuccessGreenDark else SuccessGreen

val ColorScheme.successTint: Color
    get() = if (this == DarkColors) SuccessSurfaceDark else SuccessSurface

val ColorScheme.onSuccessTint: Color
    get() = if (this == DarkColors) SuccessGreenDark else SuccessGreen

val ColorScheme.warning: Color
    get() = if (this == DarkColors) WarningAmberDark else WarningAmber

val ColorScheme.warningTint: Color
    get() = if (this == DarkColors) WarningSurfaceDark else WarningSurface

val ColorScheme.onWarningTint: Color
    get() = if (this == DarkColors) WarningAmberDark else WarningAmber

val ColorScheme.danger: Color
    get() = if (this == DarkColors) DangerRedDark else DangerRed

val ColorScheme.dangerTint: Color
    get() = if (this == DarkColors) DangerSurfaceDark else DangerSurface

val ColorScheme.onDangerTint: Color
    get() = if (this == DarkColors) DangerRedDark else DangerRed

val ColorScheme.neutralTint: Color
    get() = if (this == DarkColors) DarkSurfaceVariant else SurfaceVariant

val ColorScheme.onNeutralTint: Color
    get() = if (this == DarkColors) DarkInk700 else Ink700

val ColorScheme.ink700: Color
    get() = if (this == DarkColors) DarkInk700 else Ink700

val ColorScheme.ink500: Color
    get() = if (this == DarkColors) DarkInk500 else Ink500

val ColorScheme.ink300: Color
    get() = if (this == DarkColors) DarkInk300 else Ink300

val ColorScheme.ink100: Color
    get() = if (this == DarkColors) DarkInk100 else Ink100

// Helper functions for incident status
fun formatIncidentLabel(status: String): String = when (status.lowercase()) {
    "waiting_user", "waitinguser", "detected" -> "Awaiting response"
    "caregiver_notified", "caregivernotified" -> "Caregiver notified"
    "escalated" -> "Escalated"
    "caregiver_acknowledged", "acknowledged" -> "Acknowledged"
    "resolved" -> "Resolved"
    "false_alarm", "falsealarm" -> "False alarm"
    else -> status.replace('_', ' ').replaceFirstChar { it.uppercase() }
}
```

### `app/src/main/java/com/yuletan/soundguard/UiComponents.kt`

```kotlin
package com.yuletan.soundguard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class IncidentStatusTone { Success, Warning, Danger, Neutral }

@Composable
fun StatusChip(
    label: String,
    tone: IncidentStatusTone = IncidentStatusTone.Neutral,
    modifier: Modifier = Modifier
) {
    val (background, foreground) = when (tone) {
        IncidentStatusTone.Success -> MaterialTheme.colorScheme.successTint to MaterialTheme.colorScheme.onSuccessTint
        IncidentStatusTone.Warning -> MaterialTheme.colorScheme.warningTint to MaterialTheme.colorScheme.onWarningTint
        IncidentStatusTone.Danger -> MaterialTheme.colorScheme.dangerTint to MaterialTheme.colorScheme.onDangerTint
        IncidentStatusTone.Neutral -> MaterialTheme.colorScheme.neutralTint to MaterialTheme.colorScheme.onNeutralTint
    }
    Box(
        modifier = modifier
            .background(background, RoundedCornerShape(999.dp))
            .padding(horizontal = 9.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label.uppercase(),
            color = foreground,
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.4.sp,
        )
    }
}

@Composable
fun AvatarCircle(
    text: String,
    modifier: Modifier = Modifier,
    sizeDp: Int = 34,
    backgroundColor: Color = MaterialTheme.colorScheme.primary,
    textColor: Color = MaterialTheme.colorScheme.onPrimary,
) {
    Box(
        modifier = modifier
            .size(sizeDp.dp)
            .background(backgroundColor, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text.take(1).uppercase(),
            color = textColor,
            fontWeight = FontWeight.Bold,
            fontSize = (sizeDp * 0.42).sp,
        )
    }
}

@Composable
fun SoundGuardSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        modifier = modifier,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = MaterialTheme.colorScheme.primary,
            uncheckedThumbColor = Color.White,
            uncheckedTrackColor = MaterialTheme.colorScheme.outline,
            uncheckedBorderColor = Color.Transparent,
        ),
    )
}

@Composable
fun CountdownRing(remainingMs: Long, totalMs: Long, modifier: Modifier = Modifier) {
    val progress = (remainingMs.toFloat() / totalMs.coerceAtLeast(1L)).coerceIn(0f, 1f)
    Box(
        modifier = modifier.size(64.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            progress = { progress },
            modifier = Modifier.size(64.dp),
            color = MaterialTheme.colorScheme.danger,
            trackColor = MaterialTheme.colorScheme.outlineVariant,
            strokeWidth = 3.dp,
        )
        Text(
            "${remainingMs / 60_000}:${((remainingMs / 1_000) % 60).toString().padStart(2, '0')}",
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
fun ConfidenceBar(confidence: Float, label: String = "Confidence") {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.ink500)
        LinearProgressIndicator(
            progress = { confidence.coerceIn(0f, 1f) },
            modifier = Modifier.weight(1f).height(4.dp),
            color = if (confidence >= .75f) MaterialTheme.colorScheme.danger else MaterialTheme.colorScheme.warning,
            trackColor = MaterialTheme.colorScheme.outlineVariant,
        )
        Text("${(confidence * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun OtpCodeInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    length: Int = 6,
) {
    BasicTextField(
        value = value,
        onValueChange = { onValueChange(it.filter(Char::isLetterOrDigit).uppercase().take(length)) },
        modifier = modifier,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
        decorationBox = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.4.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "6-CHARACTER CODE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.ink500,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.6.sp,
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    repeat(length) { index ->
                        val char = value.getOrNull(index)?.toString() ?: "—"
                        Text(
                            text = char,
                            fontSize = 18.sp,
                            fontWeight = if (index < value.length) FontWeight.Bold else FontWeight.Normal,
                            fontFamily = FontFamily.Monospace,
                            color = if (index < value.length) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.ink300,
                        )
                    }
                }
            }
        },
    )
}

@Composable
fun CollapsibleSection(
    title: String,
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = false,
    content: @Composable () -> Unit,
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "chev_rotation")

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(18.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp))
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { expanded = !expanded },
                    )
                    .padding(horizontal = 14.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Icon(
                    imageVector = Icons.Outlined.KeyboardArrowDown,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.ink500,
                    modifier = Modifier.rotate(rotation).size(20.dp),
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                Column(modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp)) {
                    content()
                }
            }
        }
    }
}

@Composable
fun IncidentBanner(
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.dangerTint, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(MaterialTheme.colorScheme.danger, CircleShape)
        )
        Text(
            text = text,
            color = MaterialTheme.colorScheme.danger,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

enum class SoundGuardTab { Home, Chat, People, Settings }

@Composable
fun SoundGuardBottomNav(
    selectedTab: SoundGuardTab,
    onTabSelected: (SoundGuardTab) -> Unit,
    peopleTabLabel: String = "Caregivers",
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NavItem(
                icon = Icons.Outlined.Home,
                label = "Home",
                selected = selectedTab == SoundGuardTab.Home,
                onClick = { onTabSelected(SoundGuardTab.Home) }
            )
            NavItem(
                icon = Icons.Outlined.ChatBubbleOutline,
                label = "Chat",
                selected = selectedTab == SoundGuardTab.Chat,
                onClick = { onTabSelected(SoundGuardTab.Chat) }
            )
            NavItem(
                icon = Icons.Outlined.People,
                label = peopleTabLabel,
                selected = selectedTab == SoundGuardTab.People,
                onClick = { onTabSelected(SoundGuardTab.People) }
            )
            NavItem(
                icon = Icons.Outlined.Tune,
                label = "Settings",
                selected = selectedTab == SoundGuardTab.Settings,
                onClick = { onTabSelected(SoundGuardTab.Settings) }
            )
        }
    }
}

@Composable
private fun NavItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.ink300
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp, vertical = 6.dp),
    ) {
        Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(2.dp))
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = color)
    }
}
```

### `app/src/main/java/com/yuletan/soundguard/MainActivity.kt`

```kotlin
package com.yuletan.soundguard

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class AppScreen {
    Loading,
    Login,
    RoleSelection,
    Setup,
    CameraTest,
    Terms,
    BeneficiaryDashboard,
    CaregiverDashboard,
    Chat,
    Settings,
}

class MainActivity : ComponentActivity() {
    private var screen by mutableStateOf(AppScreen.Loading)
    private var selectedRole by mutableStateOf<String?>(null)
    private var fullName by mutableStateOf("")
    private var phone by mutableStateOf("")
    private var monitoringConsent by mutableStateOf(false)
    private var autoApproveCameraRequests by mutableStateOf(true)
    private var termsAccepted by mutableStateOf(false)
    private var microphoneGranted by mutableStateOf(false)
    private var notificationsGranted by mutableStateOf(false)
    private var cameraReady by mutableStateOf(false)
    private var email by mutableStateOf("")
    private var otp by mutableStateOf("")
    private var otpSent by mutableStateOf(false)
    private var otpCooldownSeconds by mutableStateOf(0)
    private var authBusy by mutableStateOf(false)
    private var authMessage by mutableStateOf<String?>(null)
    private var setupMessage by mutableStateOf<String?>(null)

    // Dark Mode Testing Toggle State (persisted in preferences)
    private var darkModeEnabled by mutableStateOf<Boolean?>(null)

    // Dashboard State
    private var connectedCaregivers by mutableStateOf<List<CaregiverMember>>(emptyList())
    private var monitoredBeneficiaries by mutableStateOf<List<MonitoredBeneficiary>>(emptyList())
    private var caregiverNotifications by mutableStateOf<List<CaregiverNotification>>(emptyList())
    private var generatedPairingCode by mutableStateOf<String?>(null)
    private var dashboardLoading by mutableStateOf(false)
    private var dashboardMessage by mutableStateOf<String?>(null)
    private var demoCaregiverLinked by mutableStateOf(false)
    private var demoLabUnlocked by mutableStateOf(false)
    private var returnToPreviewAfterCamera by mutableStateOf(false)
    private var demoPhotoRequested by mutableStateOf(false)
    private var demoPhotoDecision by mutableStateOf<String?>(null)
    private var demoPhotoRequestedAt by mutableStateOf<Long?>(null)
    private var demoPhotoDecisionAt by mutableStateOf<Long?>(null)
    private var demoPhotoPath by mutableStateOf<String?>(null)
    private var demoSnapshotRequest by mutableStateOf<SnapshotRequest?>(null)
    private var demoSnapshotMessage by mutableStateOf<String?>(null)
    private var pendingSnapshotRequest by mutableStateOf<PendingSnapshotRequest?>(null)
    private var loadedSessionUserId: String? = null
    private var showResetDataDialog by mutableStateOf(false)

    // Chat State
    private var chatMessages by mutableStateOf<List<ChatMessage>>(emptyList())
    private var selectedChatPartner by mutableStateOf<ChatPreview?>(null)
    private var chatLoading by mutableStateOf(false)

    // Role Switch Dialog States
    private var showBlockedSwitchDialog by mutableStateOf(false)
    private var blockedActiveConnectionsCount by mutableStateOf(0)
    private var showConfirmSwitchDialog by mutableStateOf(false)

    private val authClient by lazy { AuthClient(this) }
    private val profileClient by lazy { ProfileClient(this) }
    private val careClient by lazy { CareClient(this) }
    private val notificationClient by lazy { NotificationClient(this) }
    private val chatRepository by lazy { ChatRepository(this) }

    private val cameraPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) screen = AppScreen.CameraTest
    }

    private val setupPermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        checkPermissionsState()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Load dark mode preference
        val prefs = getSharedPreferences("soundguard_settings", Context.MODE_PRIVATE)
        if (prefs.contains("dark_mode")) {
            darkModeEnabled = prefs.getBoolean("dark_mode", false)
        }

        checkPermissionsState()

        setContent {
            val liveAudioState by AudioMonitoringService.audioState.collectAsState()
            val systemInDark = isSystemInDarkTheme()
            val isDark = darkModeEnabled ?: systemInDark

            SoundGuardTheme(darkTheme = isDark) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    when (screen) {
                        AppScreen.Loading -> LoadingScreen()
                        AppScreen.Login -> LoginScreen(
                            email = email,
                            otp = otp,
                            otpSent = otpSent,
                            otpCooldownSeconds = otpCooldownSeconds,
                            busy = authBusy,
                            message = authMessage,
                            onEmailChange = { email = it },
                            onOtpChange = { otp = it },
                            onSendOtp = { sendOtp() },
                            onVerifyOtp = { verifyOtp() },
                            onGoogleLogin = {
                                startActivity(Intent(Intent.ACTION_VIEW, authClient.googleAuthorizationUri()))
                            },
                        )
                        AppScreen.RoleSelection -> RoleSelection(
                            initialRole = selectedRole ?: "Beneficiary",
                            onRoleSelected = { role ->
                                selectedRole = role
                                screen = AppScreen.Setup
                            }
                        )
                        AppScreen.Setup -> SetupScreen(
                            role = selectedRole,
                            fullName = fullName,
                            phone = phone,
                            consent = monitoringConsent,
                            autoApproveCameraRequests = autoApproveCameraRequests,
                            termsAccepted = termsAccepted,
                            microphoneGranted = microphoneGranted,
                            notificationsGranted = notificationsGranted,
                            cameraReady = cameraReady,
                            setupMessage = setupMessage,
                            onFullNameChange = { fullName = it },
                            onPhoneChange = { phone = it },
                            onConsentChange = { monitoringConsent = it },
                            onAutoApproveCameraRequestsChange = { autoApproveCameraRequests = it },
                            onTermsChange = { termsAccepted = it },
                            onOpenTerms = { screen = AppScreen.Terms },
                            onRequestPermissions = { requestAppPermissions() },
                            onCameraTest = { cameraPermission.launch(Manifest.permission.CAMERA) },
                            onBack = { screen = AppScreen.RoleSelection },
                            onConfirm = { confirmSetup() },
                        )
                        AppScreen.CameraTest -> CameraTestScreen(
                            onBack = {
                                screen = if (returnToPreviewAfterCamera) AppScreen.BeneficiaryDashboard else AppScreen.Setup
                                returnToPreviewAfterCamera = false
                            },
                            onFinished = { capturedPath ->
                                cameraReady = true
                                if (returnToPreviewAfterCamera) {
                                    demoPhotoPath = capturedPath
                                    demoSnapshotRequest?.let { request ->
                                        val savedPath = capturedPath ?: return@let
                                        lifecycleScope.launch {
                                            SnapshotClient(this@MainActivity)
                                                .uploadSnapshot(request, java.io.File(savedPath))
                                                .onSuccess { demoSnapshotMessage = "Photo uploaded to secure storage." }
                                                .onFailure { demoSnapshotMessage = "Photo upload failed: ${it.message}" }
                                        }
                                    }
                                }
                                screen = if (returnToPreviewAfterCamera) AppScreen.BeneficiaryDashboard else AppScreen.Setup
                                returnToPreviewAfterCamera = false
                            },
                        )
                        AppScreen.Terms -> TermsScreen(
                            onBack = {
                                screen = if (selectedRole != null && connectedCaregivers.isNotEmpty()) {
                                    AppScreen.Settings
                                } else {
                                    AppScreen.Setup
                                }
                            },
                            onAccept = {
                                termsAccepted = true
                                screen = AppScreen.Setup
                            },
                        )
                        AppScreen.BeneficiaryDashboard -> BeneficiaryDashboard(
                            fullName = fullName,
                            email = email,
                            audioState = liveAudioState,
                            onToggleMonitoring = {
                                if (liveAudioState.isMonitoring) {
                                    AudioMonitoringService.stop(this@MainActivity)
                                } else {
                                    if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                                        requestAppPermissions()
                                    } else {
                                        AudioMonitoringService.start(this@MainActivity)
                                    }
                                }
                            },
                            onSimulateSound = { category, label, confidence, isEmergency ->
                                if (!liveAudioState.isMonitoring) {
                                    AudioMonitoringService.start(this@MainActivity)
                                }
                                AudioMonitoringService.simulateSound(category, label, confidence, isEmergency)
                            },
                            caregivers = connectedCaregivers,
                            pairingCode = generatedPairingCode,
                            loading = dashboardLoading,
                            message = dashboardMessage,
                            onGenerateCode = { generatePairingCode() },
                            onCopyCode = { code -> copyToClipboard(code) },
                            onShareCode = { code -> sharePairingCode(code) },
                            onSetPrimary = { connectionId, caregiverId -> setPrimaryCaregiver(connectionId, caregiverId) },
                            onRemoveCaregiver = { connectionId -> removeCaregiver(connectionId) },
                            onCall = { targetPhone -> callPhoneNumber(targetPhone) },
                            onOpenCaregiverChat = { partner ->
                                selectedChatPartner = partner
                                screen = AppScreen.Chat
                                loadChatMessages(partner.partnerId)
                            },
                            onIncidentResponse = { response -> AudioMonitoringService.respondToIncident(response) },
                            onLinkDemoCaregiver = { demoCaregiverLinked = true },
                            onOpenSettings = { screen = AppScreen.Settings },
                            onRefresh = { refreshBeneficiaryData() },
                        )
                        AppScreen.CaregiverDashboard -> CaregiverDashboard(
                            fullName = fullName,
                            email = email,
                            beneficiaries = monitoredBeneficiaries,
                            loading = dashboardLoading,
                            message = dashboardMessage,
                            onConnectBeneficiary = { code -> pairBeneficiary(code) },
                            onRemoveBeneficiary = { connectionId -> removeBeneficiary(connectionId) },
                            onCall = { targetPhone -> callPhoneNumber(targetPhone) },
                            onOpenBeneficiaryChat = { partner ->
                                selectedChatPartner = partner
                                screen = AppScreen.Chat
                                loadChatMessages(partner.partnerId)
                            },
                            onOpenSettings = { screen = AppScreen.Settings },
                            onRefresh = { refreshCaregiverData() },
                        )
                        AppScreen.Chat -> {
                            val activeIncident = liveAudioState.activeIncident
                            val bannerText = if (liveAudioState.isEmergency && activeIncident != null) {
                                "Active incident · ${activeIncident.soundLabel}"
                            } else null

                            ChatScreen(
                                partnerName = selectedChatPartner?.partnerName ?: "Chat",
                                partnerPhone = selectedChatPartner?.partnerPhone.orEmpty(),
                                messages = chatMessages,
                                loading = chatLoading,
                                isCaregiverView = selectedRole.equals("caregiver", ignoreCase = true),
                                snapshotMessage = demoSnapshotMessage,
                                activeIncidentBannerText = bannerText,
                                onRequestPhoto = {
                                    if (selectedRole.equals("caregiver", ignoreCase = true)) requestDemoSnapshotAndOpenCamera(false)
                                    else demoSnapshotMessage = "Photo requests are made by caregivers."
                                },
                                onCall = { callPhoneNumber(selectedChatPartner?.partnerPhone.orEmpty()) },
                                onBack = {
                                    screen = if (selectedRole.equals("caregiver", ignoreCase = true))
                                        AppScreen.CaregiverDashboard else AppScreen.BeneficiaryDashboard
                                },
                                onRefresh = {
                                    selectedChatPartner?.let { partner ->
                                        loadChatMessages(partner.partnerId)
                                    }
                                },
                            )
                        }
                        AppScreen.Settings -> SettingsScreen(
                            fullName = fullName,
                            email = email,
                            phone = phone,
                            currentRole = selectedRole ?: "Beneficiary",
                            microphoneGranted = microphoneGranted,
                            notificationsGranted = notificationsGranted,
                            autoApproveCameraRequests = autoApproveCameraRequests,
                            darkModeEnabled = darkModeEnabled ?: isSystemInDarkTheme(),
                            onDarkModeToggle = { enabled ->
                                darkModeEnabled = enabled
                                getSharedPreferences("soundguard_settings", Context.MODE_PRIVATE)
                                    .edit()
                                    .putBoolean("dark_mode", enabled)
                                    .apply()
                            },
                            onRequestPermissions = { requestAppPermissions() },
                            onFullNameChange = {
                                fullName = it
                                updateProfileSilently()
                            },
                            onPhoneChange = {
                                phone = it
                                updateProfileSilently()
                            },
                            onAutoApproveCameraRequestsChange = { value ->
                                autoApproveCameraRequests = value
                                lifecycleScope.launch {
                                    profileClient.saveBeneficiarySettings(
                                        monitoringConsent = monitoringConsent,
                                        autoApproveCameraRequests = autoApproveCameraRequests,
                                    ).onFailure { dashboardMessage = it.message }
                                }
                            },
                            onRequestSwitchRole = { handleRoleSwitchRequest() },
                            onOpenTerms = { screen = AppScreen.Terms },
                            onCameraTest = { cameraPermission.launch(Manifest.permission.CAMERA) },
                            onBack = {
                                screen = if (selectedRole.equals("caregiver", ignoreCase = true)) {
                                    AppScreen.CaregiverDashboard
                                } else {
                                    AppScreen.BeneficiaryDashboard
                                }
                            },
                            onSignOut = { signOut() },
                            onResetAllData = { showResetDataDialog = true },
                        )
                    }

                    pendingSnapshotRequest?.let { request ->
                        AlertDialog(
                            onDismissRequest = { pendingSnapshotRequest = null },
                            title = { Text("Verification photo request", fontWeight = FontWeight.Bold) },
                            text = { Text("Your caregiver requested a verification photo during an active incident. Approve this request to allow the photo check.") },
                            confirmButton = {
                                Button(onClick = { decideSnapshotRequest(request.id, true) }) { Text("Approve") }
                            },
                            dismissButton = {
                                TextButton(onClick = { decideSnapshotRequest(request.id, false) }) { Text("Decline") }
                            },
                        )
                    }

                    if (showResetDataDialog) {
                        AlertDialog(
                            onDismissRequest = { showResetDataDialog = false },
                            title = { Text("Delete all account data?", fontWeight = FontWeight.Bold) },
                            text = { Text("This permanently deletes your connections, incidents, notifications, snapshots, device tokens, settings, and profile setup. You will return to role selection.") },
                            confirmButton = {
                                Button(
                                    onClick = { showResetDataDialog = false; resetAllAccountData() },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                ) {
                                    Text("Delete all data")
                                }
                            },
                            dismissButton = { TextButton(onClick = { showResetDataDialog = false }) { Text("Cancel") } },
                        )
                    }

                    if (showBlockedSwitchDialog) {
                        AlertDialog(
                            onDismissRequest = { showBlockedSwitchDialog = false },
                            title = { Text("Cannot Switch Role", fontWeight = FontWeight.Bold) },
                            text = {
                                Text(
                                    "You currently have $blockedActiveConnectionsCount active care connection(s). " +
                                        "To protect ongoing safety monitoring, you must remove all connections before switching your account role.",
                                )
                            },
                            confirmButton = {
                                Button(onClick = { showBlockedSwitchDialog = false }) {
                                    Text("Got It")
                                }
                            },
                        )
                    }

                    if (showConfirmSwitchDialog) {
                        val targetRole = if (selectedRole.equals("caregiver", ignoreCase = true)) "Beneficiary" else "Caregiver"
                        AlertDialog(
                            onDismissRequest = { showConfirmSwitchDialog = false },
                            title = { Text("Switch Role to $targetRole?", fontWeight = FontWeight.Bold) },
                            text = {
                                Text(
                                    "Your current role will be changed to $targetRole. You will be redirected to complete the readiness setup for this role.",
                                )
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        showConfirmSwitchDialog = false
                                        executeRoleSwitch()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                ) {
                                    Text("Yes, Switch Role")
                                }
                            },
                            dismissButton = {
                                OutlinedButton(onClick = { showConfirmSwitchDialog = false }) {
                                    Text("Cancel")
                                }
                            },
                        )
                    }
                }
            }
        }

        checkExistingSession()
        PushTokenRegistrar.register(this)
        handleOAuthIntent(intent)
        handleChatDeepLink(intent)
    }

    override fun onResume() {
        super.onResume()
        checkPermissionsState()
    }

    private fun checkPermissionsState() {
        microphoneGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED
        notificationsGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleOAuthIntent(intent)
        handleChatDeepLink(intent)
    }

    private fun checkExistingSession() {
        lifecycleScope.launch {
            val token = try { authClient.getToken() } catch (_: Exception) { null }
            if (token.isNullOrBlank()) {
                resetAccountState()
                screen = AppScreen.Login
                return@launch
            }

            profileClient.fetchMyProfile()
                .onSuccess { profile ->
                    if (profile != null) {
                        val currentUserId = authClient.userId()
                        if (loadedSessionUserId != null && loadedSessionUserId != currentUserId) {
                            resetAccountState()
                        }
                        loadedSessionUserId = currentUserId
                        PushTokenRegistrar.register(this@MainActivity)
                        fullName = profile.fullName
                        email = profile.email
                        phone = profile.phone
                        if (profile.role.equals("beneficiary", ignoreCase = true)) {
                            profileClient.fetchBeneficiarySettings()
                                .onSuccess { autoApproveCameraRequests = it.autoApproveCameraRequests }
                        }
                        selectedRole = profile.role
                            .takeUnless { it.equals("null", ignoreCase = true) || it.isBlank() }
                            ?.replaceFirstChar { it.uppercase() }

                        if (profile.setupCompletedAt != null) {
                            if (profile.role.equals("caregiver", ignoreCase = true)) {
                                screen = AppScreen.CaregiverDashboard
                                refreshCaregiverData()
                            } else {
                                screen = AppScreen.BeneficiaryDashboard
                                refreshBeneficiaryData()
                            }
                        } else if (profile.role.isNotBlank()) {
                            screen = AppScreen.Setup
                        } else {
                            screen = AppScreen.RoleSelection
                        }
                    } else {
                        screen = AppScreen.RoleSelection
                    }
                }
                .onFailure {
                    screen = AppScreen.Login
                }
        }
    }

    private fun handleOAuthIntent(intent: Intent?) {
        val callback = intent?.data ?: return
        if (callback.scheme != "soundguard" || callback.host != "auth") return
        lifecycleScope.launch {
            authClient.handleGoogleCallback(callback)
                .onSuccess {
                    checkExistingSession()
                }
                .onFailure { authMessage = it.message ?: "Google sign-in failed." }
        }
    }

    private fun handleChatDeepLink(intent: Intent?) {
        val beneficiaryId = intent?.getStringExtra("beneficiary_id") ?: return
        if (screen != AppScreen.CaregiverDashboard && screen != AppScreen.BeneficiaryDashboard) return
        lifecycleScope.launch {
            val partnerName = monitoredBeneficiaries.firstOrNull { it.beneficiaryId == beneficiaryId }?.name
                ?: connectedCaregivers.firstOrNull { it.caregiverId == beneficiaryId }?.name
                ?: "Chat"
            val partnerPhone = monitoredBeneficiaries.firstOrNull { it.beneficiaryId == beneficiaryId }?.phone
                ?: connectedCaregivers.firstOrNull { it.caregiverId == beneficiaryId }?.phone
                ?: ""
            selectedChatPartner = ChatPreview(
                partnerId = beneficiaryId,
                partnerName = partnerName,
                partnerPhone = partnerPhone,
                lastMessage = "",
                lastTimestamp = System.currentTimeMillis(),
                unreadCount = 0,
            )
            screen = AppScreen.Chat
            loadChatMessages(beneficiaryId)
        }
    }

    private fun sendOtp() {
        if (!Validation.isEmailValid(email)) {
            authMessage = "Enter a valid email address."
            return
        }
        authBusy = true
        authMessage = null
        lifecycleScope.launch {
            authClient.sendOtp(email)
                .onSuccess {
                    otpSent = true
                    authMessage = "Check your email for the one-time code."
                    lifecycleScope.launch {
                        for (seconds in 60 downTo 1) {
                            otpCooldownSeconds = seconds
                            delay(1_000)
                        }
                        otpCooldownSeconds = 0
                    }
                }
                .onFailure {
                    val detail = it.message.orEmpty()
                    authMessage = if (detail.contains("rate", ignoreCase = true)) {
                        "Email rate limit reached. Wait before requesting another code."
                    } else {
                        detail.ifBlank { "Could not send OTP." }
                    }
                }
            authBusy = false
        }
    }

    private fun verifyOtp() {
        if (otp.trim().length < 6) {
            authMessage = "Enter the verification code from your email."
            return
        }
        authBusy = true
        authMessage = null
        lifecycleScope.launch {
            authClient.verifyOtp(email, otp)
                .onSuccess {
                    checkExistingSession()
                }
                .onFailure { authMessage = it.message ?: "Could not verify OTP." }
            authBusy = false
        }
    }

    private fun confirmSetup() {
        selectedRole?.let { role ->
            setupMessage = null
            lifecycleScope.launch {
                profileClient.saveProfile(email, fullName, phone, role)
                    .onSuccess {
                        if (role.equals("beneficiary", ignoreCase = true)) {
                            profileClient.saveBeneficiarySettings(
                                monitoringConsent = monitoringConsent,
                                autoApproveCameraRequests = autoApproveCameraRequests,
                            ).onFailure { setupMessage = it.message ?: "Could not save monitoring consent." }
                        }
                        if (setupMessage == null) {
                            if (role.equals("caregiver", ignoreCase = true)) {
                                screen = AppScreen.CaregiverDashboard
                                refreshCaregiverData()
                            } else {
                                screen = AppScreen.BeneficiaryDashboard
                                refreshBeneficiaryData()
                            }
                        }
                    }
                    .onFailure { setupMessage = it.message ?: "Could not save profile." }
            }
        }
    }

    private fun updateProfileSilently() {
        val role = selectedRole ?: return
        lifecycleScope.launch {
            profileClient.saveProfile(email, fullName, phone, role)
        }
    }

    private fun handleRoleSwitchRequest() {
        dashboardLoading = true
        lifecycleScope.launch {
            careClient.countActiveConnections()
                .onSuccess { activeCount ->
                    dashboardLoading = false
                    if (activeCount > 0) {
                        blockedActiveConnectionsCount = activeCount
                        showBlockedSwitchDialog = true
                    } else {
                        showConfirmSwitchDialog = true
                    }
                }
                .onFailure {
                    dashboardLoading = false
                    showConfirmSwitchDialog = true
                }
        }
    }

    private fun executeRoleSwitch() {
        lifecycleScope.launch {
            profileClient.resetRole()
                .onSuccess {
                    selectedRole = null
                    screen = AppScreen.RoleSelection
                    Toast.makeText(this@MainActivity, "Role reset. Please select your new role.", Toast.LENGTH_SHORT).show()
                }
                .onFailure {
                    selectedRole = null
                    screen = AppScreen.RoleSelection
                }
        }
    }

    private fun requestAppPermissions() {
        val permissions = buildList {
            add(Manifest.permission.RECORD_AUDIO)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        setupPermissions.launch(permissions.toTypedArray())
    }

    private fun refreshBeneficiaryData() {
        dashboardLoading = true
        dashboardMessage = null
        lifecycleScope.launch {
            careClient.fetchCaregiversForBeneficiary()
                .onSuccess { list -> connectedCaregivers = list }
                .onFailure { err -> dashboardMessage = err.message }
            SnapshotClient(this@MainActivity).fetchPendingForBeneficiary()
                .onSuccess { request -> pendingSnapshotRequest = request }
            dashboardLoading = false
        }
    }

    private fun refreshCaregiverData() {
        dashboardLoading = true
        dashboardMessage = null
        lifecycleScope.launch {
            careClient.fetchBeneficiariesForCaregiver()
                .onSuccess { list -> monitoredBeneficiaries = list }
                .onFailure { err -> dashboardMessage = err.message }
            notificationClient.fetchMine()
                .onSuccess { list -> caregiverNotifications = list }
            dashboardLoading = false
        }
    }

    private fun loadChatMessages(partnerId: String) {
        chatLoading = true
        chatMessages = emptyList()
        lifecycleScope.launch {
            chatRepository.buildChatMessages(partnerId, selectedRole.equals("caregiver", ignoreCase = true))
                .onSuccess { chatMessages = it }
                .onFailure { dashboardMessage = it.message }
            chatLoading = false
        }
    }

    private fun decideSnapshotRequest(id: String, approved: Boolean) {
        lifecycleScope.launch {
            val client = SnapshotClient(this@MainActivity)
            if (approved) {
                client.decidePendingRequest(id, true)
                    .onSuccess {
                        client.fetchRequest(id)
                            .onSuccess { request ->
                                demoSnapshotRequest = request
                                demoPhotoRequested = true
                                demoPhotoDecision = "approved"
                                demoPhotoRequestedAt = System.currentTimeMillis()
                                demoPhotoDecisionAt = System.currentTimeMillis()
                                pendingSnapshotRequest = null
                                openCameraForBeneficiarySelfie()
                            }
                            .onFailure {
                                Toast.makeText(this@MainActivity, "Could not load request: ${it.message}", Toast.LENGTH_LONG).show()
                            }
                    }
                    .onFailure {
                        Toast.makeText(this@MainActivity, "Could not save decision: ${it.message}", Toast.LENGTH_LONG).show()
                    }
            } else {
                client.decidePendingRequest(id, false)
                    .onSuccess {
                        pendingSnapshotRequest = null
                        refreshBeneficiaryData()
                    }
                    .onFailure {
                        Toast.makeText(this@MainActivity, "Could not save decision: ${it.message}", Toast.LENGTH_LONG).show()
                    }
            }
        }
    }

    private fun openCameraForBeneficiarySelfie() {
        returnToPreviewAfterCamera = true
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            screen = AppScreen.CameraTest
        } else {
            cameraPermission.launch(Manifest.permission.CAMERA)
        }
    }

    private fun requestDemoSnapshotAndOpenCamera(forceApproval: Boolean) {
        val liveIncidentId = AudioMonitoringService.audioState.value.backendIncidentId
        val beneficiaryId = selectedChatPartner?.partnerId
        if (beneficiaryId == null) {
            demoSnapshotMessage = "No chat partner selected."
            return
        }
        if (liveIncidentId != null) {
            requestSnapshotForIncident(liveIncidentId, beneficiaryId, forceApproval)
        } else {
            demoSnapshotMessage = "Looking up latest incident..."
            lifecycleScope.launch {
                IncidentClient(this@MainActivity).fetchIncidentsForBeneficiary(beneficiaryId)
                    .onSuccess { incidents ->
                        val latest = incidents.lastOrNull()
                        if (latest != null) {
                            requestSnapshotForIncident(latest.id, beneficiaryId, forceApproval)
                        } else {
                            demoSnapshotMessage = "No incidents found. Trigger an alert first."
                        }
                    }
                    .onFailure { demoSnapshotMessage = "Failed to look up incidents: ${it.message}" }
            }
        }
    }

    private fun requestSnapshotForIncident(incidentId: String, beneficiaryId: String, forceApproval: Boolean) {
        lifecycleScope.launch {
            SnapshotClient(this@MainActivity)
                .requestSnapshot(incidentId, beneficiaryId)
                .onSuccess { request ->
                    demoSnapshotRequest = request
                    demoPhotoRequested = true
                    demoPhotoDecision = if (forceApproval || request.approvalStatus == "approved") "approved" else null
                    demoPhotoRequestedAt = demoPhotoRequestedAt ?: System.currentTimeMillis()
                    demoPhotoDecisionAt = System.currentTimeMillis()
                    if (forceApproval || request.approvalStatus == "approved") {
                        demoSnapshotMessage = "Photo request approved. Waiting for beneficiary to capture the photo."
                    } else {
                        demoSnapshotMessage = "Photo request is waiting for beneficiary approval."
                    }
                }
                .onFailure { demoSnapshotMessage = "Photo request failed: ${it.message}" }
        }
    }

    private fun generatePairingCode() {
        dashboardLoading = true
        dashboardMessage = null
        lifecycleScope.launch {
            careClient.createPairingCode()
                .onSuccess { code -> generatedPairingCode = code }
                .onFailure { err -> dashboardMessage = err.message ?: "Failed to create pairing code." }
            dashboardLoading = false
        }
    }

    private fun pairBeneficiary(code: String) {
        dashboardLoading = true
        dashboardMessage = null
        lifecycleScope.launch {
            careClient.acceptPairingCode(code)
                .onSuccess { beneficiaryName ->
                    Toast.makeText(this@MainActivity, "Connected to $beneficiaryName!", Toast.LENGTH_SHORT).show()
                    refreshCaregiverData()
                }
                .onFailure { err -> dashboardMessage = err.message ?: "Failed to pair with code." }
            dashboardLoading = false
        }
    }

    private fun setPrimaryCaregiver(connectionId: String, caregiverId: String) {
        val userId = authClient.userId() ?: return
        dashboardLoading = true
        lifecycleScope.launch {
            careClient.setPrimaryCaregiver(userId, connectionId)
                .onSuccess { refreshBeneficiaryData() }
                .onFailure { dashboardMessage = it.message }
            dashboardLoading = false
        }
    }

    private fun removeCaregiver(connectionId: String) {
        dashboardLoading = true
        lifecycleScope.launch {
            careClient.removeCareConnection(connectionId)
                .onSuccess { refreshBeneficiaryData() }
                .onFailure { dashboardMessage = it.message }
            dashboardLoading = false
        }
    }

    private fun removeBeneficiary(connectionId: String) {
        dashboardLoading = true
        lifecycleScope.launch {
            careClient.removeCareConnection(connectionId)
                .onSuccess { refreshCaregiverData() }
                .onFailure { dashboardMessage = it.message }
            dashboardLoading = false
        }
    }

    private fun callPhoneNumber(targetPhone: String) {
        if (targetPhone.isBlank()) {
            Toast.makeText(this, "No phone number available.", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${targetPhone.trim()}"))
            startActivity(dialIntent)
        } catch (_: Exception) {
            Toast.makeText(this, "No phone app available.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("SoundGuard Pairing Code", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "Pairing code copied to clipboard!", Toast.LENGTH_SHORT).show()
    }

    private fun sharePairingCode(text: String) {
        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "Use this SoundGuard pairing code: $text")
        }, "Share pairing code"))
    }

    private fun signOut() {
        AudioMonitoringService.stop(this)
        resetAccountState()
        authClient.signOut()
        screen = AppScreen.Login
    }

    private fun resetAllAccountData() {
        lifecycleScope.launch {
            profileClient.resetAllAccountData()
                .onSuccess {
                    resetAccountState()
                    screen = AppScreen.RoleSelection
                    Toast.makeText(this@MainActivity, "All account data deleted. Choose your role again.", Toast.LENGTH_LONG).show()
                }
                .onFailure { Toast.makeText(this@MainActivity, "Could not delete account data: ${it.message}", Toast.LENGTH_LONG).show() }
        }
    }

    private fun resetAccountState() {
        demoPhotoPath?.let { path -> runCatching { java.io.File(path).delete() } }
        AudioMonitoringService.resetSessionState()
        loadedSessionUserId = null
        email = ""
        otp = ""
        otpSent = false
        fullName = ""
        phone = ""
        selectedRole = null
        monitoringConsent = false
        termsAccepted = false
        connectedCaregivers = emptyList()
        monitoredBeneficiaries = emptyList()
        caregiverNotifications = emptyList()
        generatedPairingCode = null
        demoCaregiverLinked = false
        demoPhotoRequested = false
        demoPhotoDecision = null
        demoPhotoRequestedAt = null
        demoPhotoDecisionAt = null
        demoPhotoPath = null
        demoSnapshotRequest = null
        demoSnapshotMessage = null
        autoApproveCameraRequests = true
        chatMessages = emptyList()
        selectedChatPartner = null
    }
}

// -------------------------------------------------------------------------------------------------
// UI SCREENS
// -------------------------------------------------------------------------------------------------

@Composable
private fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun TopBarHeader(
    name: String,
    roleSubtitle: String,
    onOpenSettings: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AvatarCircle(text = name.ifBlank { "User" }, sizeDp = 34)
            Column {
                Text(
                    text = name.ifBlank { "User" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = roleSubtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.ink500,
                )
            }
        }
        IconButton(
            onClick = onOpenSettings,
            modifier = Modifier.size(34.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Tune,
                contentDescription = "Settings",
                tint = MaterialTheme.colorScheme.ink700,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun LoginScreen(
    email: String,
    otp: String,
    otpSent: Boolean,
    otpCooldownSeconds: Int,
    busy: Boolean,
    message: String?,
    onEmailChange: (String) -> Unit,
    onOtpChange: (String) -> Unit,
    onSendOtp: () -> Unit,
    onVerifyOtp: () -> Unit,
    onGoogleLogin: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("SoundGuard", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(6.dp))
        Text(
            "Privacy-first sound monitoring & caregiver alerts",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.ink500,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(36.dp))

        // Outlined Google Sign In Button (preserving brand mark text)
        OutlinedButton(
            onClick = onGoogleLogin,
            enabled = !busy,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(999.dp),
            border = androidx.compose.foundation.BorderStroke(1.4.dp, MaterialTheme.colorScheme.outline),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
        ) {
            Text("Continue with Google", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }

        Spacer(Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HorizontalDivider(Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
            Text(
                " OR WITH EMAIL ",
                modifier = Modifier.padding(horizontal = 10.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.ink500,
            )
            HorizontalDivider(Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
        }
        Spacer(Modifier.height(20.dp))

        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            label = { Text("Email address") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
        )

        Spacer(Modifier.height(12.dp))

        if (otpSent) {
            OutlinedTextField(
                value = otp,
                onValueChange = onOtpChange,
                label = { Text("6-digit verification code") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onVerifyOtp,
                enabled = !busy && otp.trim().isNotEmpty(),
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Text("Verify Code & Sign In", fontWeight = FontWeight.Bold)
            }
        } else {
            Button(
                onClick = onSendOtp,
                enabled = !busy && email.trim().isNotEmpty(),
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Text(if (otpCooldownSeconds > 0) "Resend in ${otpCooldownSeconds}s" else "Send OTP Code", fontWeight = FontWeight.Bold)
            }
        }

        message?.let {
            Spacer(Modifier.height(16.dp))
            Text(
                it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
        }
    }
}

// -------------------------------------------------------------------------------------------------
// ROLE SELECTION (FRAME 5)
// -------------------------------------------------------------------------------------------------

@Composable
private fun RoleSelection(
    initialRole: String = "Beneficiary",
    onRoleSelected: (String) -> Unit
) {
    var selected by remember { mutableStateOf(initialRole) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .statusBarsPadding()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(28.dp))
        Text(
            text = "Who's this for?",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "You can invite the other side to connect once you're set up.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.ink500,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(28.dp))

        // Role Card 1: Beneficiary (Monitored)
        RoleSelectionCard(
            title = "I want to be monitored",
            desc = "Emergency sounds get detected and sent to family who can check on you.",
            icon = Icons.Outlined.FavoriteBorder,
            selected = selected.equals("Beneficiary", ignoreCase = true),
            onClick = { selected = "Beneficiary" },
        )

        Spacer(Modifier.height(12.dp))

        // Role Card 2: Caregiver
        RoleSelectionCard(
            title = "I'm a caregiver",
            desc = "Get notified if someone you care for may be in danger.",
            icon = Icons.Outlined.People,
            selected = selected.equals("Caregiver", ignoreCase = true),
            onClick = { selected = "Caregiver" },
        )

        Spacer(Modifier.weight(1f))

        Button(
            onClick = { onRoleSelected(selected) },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(999.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            Text("Continue", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }

        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun RoleSelectionCard(
    title: String,
    desc: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val containerBg = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val descColor = if (selected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.ink500
    val iconBorder = if (selected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(containerBg, RoundedCornerShape(18.dp))
            .border(
                1.6.dp,
                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                RoundedCornerShape(18.dp)
            )
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .border(1.6.dp, iconBorder, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = contentColor,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall,
                color = descColor,
                lineHeight = 16.sp,
            )
        }
    }
}

// -------------------------------------------------------------------------------------------------
// SETUP SCREEN
// -------------------------------------------------------------------------------------------------

@Composable
private fun SetupScreen(
    role: String?,
    fullName: String,
    phone: String,
    consent: Boolean,
    autoApproveCameraRequests: Boolean,
    termsAccepted: Boolean,
    microphoneGranted: Boolean,
    notificationsGranted: Boolean,
    cameraReady: Boolean,
    setupMessage: String?,
    onFullNameChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onConsentChange: (Boolean) -> Unit,
    onAutoApproveCameraRequestsChange: (Boolean) -> Unit,
    onTermsChange: (Boolean) -> Unit,
    onOpenTerms: () -> Unit,
    onRequestPermissions: () -> Unit,
    onCameraTest: () -> Unit,
    onBack: () -> Unit,
    onConfirm: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState()),
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Outlined.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
        }
        Spacer(Modifier.height(8.dp))

        Text(
            text = "${role ?: "User"} Setup",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = "Complete your profile and safety permissions.",
            modifier = Modifier.padding(top = 2.dp, bottom = 16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.ink500,
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                OutlinedTextField(
                    value = fullName,
                    onValueChange = onFullNameChange,
                    label = { Text("Full name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = phone,
                    onValueChange = onPhoneChange,
                    label = { Text("Emergency phone number") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                )
                Text(
                    text = if (Validation.isPhoneValid(phone)) "✓ Valid international phone number"
                    else "Format: +65 81234567, +1 5551234567",
                    modifier = Modifier.padding(top = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (Validation.isPhoneValid(phone)) MaterialTheme.colorScheme.success else MaterialTheme.colorScheme.ink500,
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        if (role == "Beneficiary") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onConsentChange(!consent) },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = consent, onCheckedChange = onConsentChange)
                        Spacer(Modifier.width(8.dp))
                        Text("I consent to local audio monitoring & emergency alerts", style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onAutoApproveCameraRequestsChange(!autoApproveCameraRequests) },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = autoApproveCameraRequests, onCheckedChange = onAutoApproveCameraRequestsChange)
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("Auto-approve caregiver photo requests", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text("Skips your confirmation during an active incident.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.ink500)
                        }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("Permissions & Readiness", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                ChecklistItem(isDone = microphoneGranted, title = "Microphone Permission")
                ChecklistItem(isDone = notificationsGranted, title = "Notification Permission")
                ChecklistItem(isDone = cameraReady, title = "Camera Readiness")
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onRequestPermissions,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(999.dp),
                    ) {
                        Text("Permissions", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = onCameraTest,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(999.dp),
                    ) {
                        Text("Test Camera", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth().clickable { onTermsChange(!termsAccepted) },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = termsAccepted, onCheckedChange = onTermsChange)
            Spacer(Modifier.width(8.dp))
            Text("I agree to the Terms of Use", style = MaterialTheme.typography.bodyMedium)
        }
        TextButton(onClick = onOpenTerms) {
            Text("Read Terms of Use & camera privacy rules", color = MaterialTheme.colorScheme.ink700)
        }

        Spacer(Modifier.height(16.dp))

        val ready = fullName.isNotBlank() &&
            Validation.isPhoneValid(phone) &&
            termsAccepted &&
            (role != "Beneficiary" || consent) &&
            microphoneGranted &&
            notificationsGranted &&
            cameraReady

        Button(
            onClick = onConfirm,
            enabled = ready,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(999.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            Text("Complete Setup & Open Dashboard", fontWeight = FontWeight.Bold)
        }

        setupMessage?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ChecklistItem(isDone: Boolean, title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 3.dp),
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .background(
                    if (isDone) MaterialTheme.colorScheme.primary else Color.Transparent,
                    CircleShape
                )
                .border(
                    1.4.dp,
                    if (isDone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    CircleShape
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (isDone) {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(12.dp),
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun TermsScreen(onBack: () -> Unit, onAccept: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState()),
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Outlined.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
        }
        Spacer(Modifier.height(12.dp))
        Text("Terms of Use & Privacy", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        Text(
            "SoundGuard is a safety support prototype, not a medical device or certified emergency response system. " +
                "It may miss sounds or produce false alerts. Keep the device powered, online, and positioned safely.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.ink700,
        )
        Spacer(Modifier.height(14.dp))
        Text(
            "Camera privacy: Connected caregivers may request a verification snapshot during an active incident. " +
                "Snapshots are access-controlled and automatically deleted after 10 minutes. You can revoke this in Settings.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.ink700,
        )
        Spacer(Modifier.height(14.dp))
        Text(
            "Emergency Notice: SoundGuard does not automatically contact police or ambulance services. " +
                "If an emergency occurs, caregivers and beneficiaries must contact local emergency services.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.ink700,
        )
        Spacer(Modifier.height(28.dp))
        Button(
            onClick = onAccept,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(999.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            Text("I Understand and Agree", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun CameraTestScreen(onBack: () -> Unit, onFinished: (String?) -> Unit) {
    var capturedPath by remember { mutableStateOf<String?>(null) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Outlined.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
            }
            Spacer(Modifier.width(8.dp))
            Text(
                "Camera Readiness Test",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
        CameraPreview(
            modifier = Modifier.weight(1f),
            onPhotoCaptured = { capturedPath = it },
        )
        if (capturedPath != null) {
            Text(
                "✓ Photo captured locally for verification",
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.success,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
            )
        }
        Button(
            onClick = { capturedPath?.let(onFinished) },
            enabled = capturedPath != null,
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(999.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            Text(if (capturedPath == null) "Capture a photo first" else "Finish Camera Test", fontWeight = FontWeight.Bold)
        }
    }
}

// -------------------------------------------------------------------------------------------------
// BENEFICIARY DASHBOARD (FRAME 1)
// -------------------------------------------------------------------------------------------------

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun BeneficiaryDashboard(
    fullName: String,
    email: String,
    audioState: LiveAudioState,
    onToggleMonitoring: () -> Unit,
    onIncidentResponse: (BeneficiaryResponse) -> Unit,
    onSimulateSound: (category: String, label: String, confidence: Float, isEmergency: Boolean) -> Unit,
    caregivers: List<CaregiverMember>,
    pairingCode: String?,
    loading: Boolean,
    message: String?,
    onGenerateCode: () -> Unit,
    onCopyCode: (String) -> Unit,
    onShareCode: (String) -> Unit,
    onSetPrimary: (String, String) -> Unit,
    onRemoveCaregiver: (String) -> Unit,
    onCall: (String) -> Unit,
    onOpenCaregiverChat: (ChatPreview) -> Unit,
    onLinkDemoCaregiver: () -> Unit,
    onOpenSettings: () -> Unit,
    onRefresh: () -> Unit,
) {
    var showPairingSheet by remember { mutableStateOf(false) }
    var showDetailsSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        onRefresh()
        while (true) {
            delay(15_000L)
            onRefresh()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        PullToRefreshBox(
            isRefreshing = loading,
            onRefresh = onRefresh,
            modifier = Modifier.weight(1f).fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                TopBarHeader(
                    name = fullName.ifBlank { "James" },
                    roleSubtitle = "Beneficiary",
                    onOpenSettings = onOpenSettings,
                )

                val incident = audioState.activeIncident
                if (incident?.status == IncidentStatus.WaitingUser && incident.nextDeadlineAt != null) {
                    var remaining by remember { mutableStateOf(IncidentStateMachine.TWO_MINUTES_MS) }
                    LaunchedEffect(incident.nextDeadlineAt) {
                        while (true) {
                            remaining = (incident.nextDeadlineAt - System.currentTimeMillis()).coerceAtLeast(0L)
                            if (remaining == 0L) break
                            delay(1_000L)
                        }
                    }
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.dangerTint),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.danger.copy(alpha = 0.3f)),
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                CountdownRing(remaining, IncidentStateMachine.TWO_MINUTES_MS)
                                Column {
                                    Text("Sound detected", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.danger)
                                    Text("Are you okay?", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                                    Text(incident.soundLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.ink500)
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { onIncidentResponse(BeneficiaryResponse.Safe) },
                                    modifier = Modifier.weight(1f).height(42.dp),
                                    shape = RoundedCornerShape(999.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                ) {
                                    Text("I'm safe", fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(
                                    onClick = { onIncidentResponse(BeneficiaryResponse.NeedHelp) },
                                    modifier = Modifier.weight(1f).height(42.dp),
                                    shape = RoundedCornerShape(999.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.danger),
                                    border = androidx.compose.foundation.BorderStroke(1.4.dp, MaterialTheme.colorScheme.danger),
                                ) {
                                    Text("Send help", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // 1. Hero Status Card matching Mockup Frame 1
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .border(2.5.dp, MaterialTheme.colorScheme.primary, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Mic,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp),
                                )
                            }
                            Spacer(Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = if (audioState.isEmergency) "Sound detected" else if (audioState.isMonitoring) "Listening" else "Monitoring paused",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (audioState.isEmergency) "Caregivers alerted" else if (audioState.isMonitoring) "All quiet right now · " else "Tap to start · ",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.ink500,
                                    )
                                    Text(
                                        text = "Details",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.clickable { showDetailsSheet = true },
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        OutlinedButton(
                            onClick = onToggleMonitoring,
                            modifier = Modifier.fillMaxWidth().height(42.dp),
                            shape = RoundedCornerShape(999.dp),
                            border = androidx.compose.foundation.BorderStroke(1.4.dp, MaterialTheme.colorScheme.outline),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                        ) {
                            Text(
                                text = if (audioState.isMonitoring) "Stop monitoring" else "Start monitoring",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // 2. Your Caregivers Card matching Mockup Frame 1
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Your caregivers",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "+ Add",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.clickable {
                                    if (pairingCode == null) onGenerateCode()
                                    showPairingSheet = true
                                },
                            )
                        }

                        if (caregivers.isEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "No caregivers linked yet. Tap + Add above to generate a 6-character connect code.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.ink500,
                            )
                        } else {
                            caregivers.forEach { caregiver ->
                                Spacer(Modifier.height(8.dp))
                                BeneficiaryCaregiverRow(
                                    caregiver = caregiver,
                                    onSetPrimary = { onSetPrimary(caregiver.connectionId, caregiver.caregiverId) },
                                    onRemove = { onRemoveCaregiver(caregiver.connectionId) },
                                    onCall = { onCall(caregiver.phone) },
                                    onOpenChat = {
                                        onOpenCaregiverChat(
                                            ChatPreview(
                                                partnerId = caregiver.caregiverId,
                                                partnerName = caregiver.name,
                                                partnerPhone = caregiver.phone,
                                                lastMessage = "",
                                                lastTimestamp = System.currentTimeMillis(),
                                                unreadCount = 0,
                                            )
                                        )
                                    },
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // 3. Developer & Test Tools (Collapsible Accordion) matching Mockup Frame 1
                CollapsibleSection(
                    title = "Developer & test tools",
                    initiallyExpanded = false,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        TestToolButton(
                            title = "Simulate glass break",
                            onClick = { onSimulateSound("emergency", "Glass break", 0.91f, true) },
                        )
                        TestToolButton(
                            title = "Simulate smoke alarm",
                            onClick = { onSimulateSound("emergency", "Smoke alarm", 0.94f, true) },
                        )
                    }
                }

                message?.let {
                    Spacer(Modifier.height(12.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                Spacer(Modifier.height(16.dp))
            }
        }

        // Bottom Navigation Bar
        SoundGuardBottomNav(
            selectedTab = SoundGuardTab.Home,
            onTabSelected = { tab ->
                when (tab) {
                    SoundGuardTab.Home -> {}
                    SoundGuardTab.Chat -> {
                        caregivers.firstOrNull()?.let { caregiver ->
                            onOpenCaregiverChat(
                                ChatPreview(caregiver.caregiverId, caregiver.name, caregiver.phone, "", System.currentTimeMillis(), 0)
                            )
                        } ?: run {
                            onOpenSettings()
                        }
                    }
                    SoundGuardTab.People -> {
                        if (pairingCode == null) onGenerateCode()
                        showPairingSheet = true
                    }
                    SoundGuardTab.Settings -> onOpenSettings()
                }
            },
            peopleTabLabel = "Caregivers",
        )
    }

    // Pairing Code Sheet
    if (showPairingSheet) {
        ModalBottomSheet(
            onDismissRequest = { showPairingSheet = false },
            sheetState = rememberModalBottomSheetState(),
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Connect a caregiver", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text("Give this 6-character code to your caregiver to link apps:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.ink500)

                Spacer(Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp))
                        .padding(horizontal = 28.dp, vertical = 14.dp),
                ) {
                    Text(
                        text = pairingCode ?: "••••••",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 4.sp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { pairingCode?.let(onCopyCode) },
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    ) {
                        Text("Copy Code", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = { pairingCode?.let(onShareCode) },
                        shape = RoundedCornerShape(999.dp),
                        border = androidx.compose.foundation.BorderStroke(1.4.dp, MaterialTheme.colorScheme.outline),
                    ) {
                        Text("Share", fontWeight = FontWeight.Bold)
                    }
                }
                TextButton(onClick = onGenerateCode) {
                    Text("Generate new code", color = MaterialTheme.colorScheme.ink700)
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    // Details Modal Sheet
    if (showDetailsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showDetailsSheet = false },
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .navigationBarsPadding()
            ) {
                Text("Live Audio Classification Details", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                Text("Classifier: Local TensorFlow Lite Audio Model", style = MaterialTheme.typography.bodyMedium)
                Text("Status: ${if (audioState.isMonitoring) "Actively processing continuous audio buffer" else "Idle"}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.ink500)
                Spacer(Modifier.height(8.dp))
                ConfidenceBar(audioState.activeIncident?.confidence ?: 0.0f, "Live Confidence")
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { showDetailsSheet = false },
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                ) {
                    Text("Close Details", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun TestToolButton(
    title: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.4.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.ink700,
        )
        StatusChip(label = "TEST", tone = IncidentStatusTone.Neutral)
    }
}

@Composable
private fun BeneficiaryCaregiverRow(
    caregiver: CaregiverMember,
    onSetPrimary: () -> Unit,
    onRemove: () -> Unit,
    onCall: () -> Unit,
    onOpenChat: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenChat)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AvatarCircle(text = caregiver.name, sizeDp = 32)
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = caregiver.name,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (caregiver.isPrimary) {
                    StatusChip(label = "PRIMARY", tone = IncidentStatusTone.Neutral)
                }
            }
            Text("Tap to open chat", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.ink500)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
            if (caregiver.phone.isNotBlank()) {
                IconButton(onClick = onCall, modifier = Modifier.size(30.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.Call,
                        contentDescription = "Call",
                        tint = MaterialTheme.colorScheme.ink700,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            Box {
                IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(30.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.MoreVert,
                        contentDescription = "Options",
                        tint = MaterialTheme.colorScheme.ink700,
                        modifier = Modifier.size(16.dp),
                    )
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    if (!caregiver.isPrimary) {
                        DropdownMenuItem(
                            text = { Text("Set as primary") },
                            onClick = { menuExpanded = false; onSetPrimary() },
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Remove caregiver", color = MaterialTheme.colorScheme.error) },
                        onClick = { menuExpanded = false; onRemove() },
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// CAREGIVER DASHBOARD (FRAME 2)
// -------------------------------------------------------------------------------------------------

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun CaregiverDashboard(
    fullName: String,
    email: String,
    beneficiaries: List<MonitoredBeneficiary>,
    loading: Boolean,
    message: String?,
    onConnectBeneficiary: (String) -> Unit,
    onRemoveBeneficiary: (String) -> Unit,
    onCall: (String) -> Unit,
    onOpenBeneficiaryChat: (ChatPreview) -> Unit,
    onOpenSettings: () -> Unit,
    onRefresh: () -> Unit,
) {
    var codeInput by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        onRefresh()
        while (true) {
            delay(15_000L)
            onRefresh()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        PullToRefreshBox(
            isRefreshing = loading,
            onRefresh = onRefresh,
            modifier = Modifier.weight(1f).fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                TopBarHeader(
                    name = fullName.ifBlank { "Yo" },
                    roleSubtitle = "Caregiver",
                    onOpenSettings = onOpenSettings,
                )

                // 1. Connect a Beneficiary Card matching Mockup Frame 2
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Connect a beneficiary",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.height(10.dp))

                        OtpCodeInput(
                            value = codeInput,
                            onValueChange = { codeInput = it },
                            modifier = Modifier.fillMaxWidth(),
                        )

                        Spacer(Modifier.height(10.dp))

                        Button(
                            onClick = {
                                if (codeInput.length == 6) {
                                    onConnectBeneficiary(codeInput)
                                    codeInput = ""
                                }
                            },
                            enabled = !loading && codeInput.length == 6,
                            modifier = Modifier.fillMaxWidth().height(44.dp),
                            shape = RoundedCornerShape(999.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                        ) {
                            Text("Connect", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        Spacer(Modifier.height(8.dp))

                        Text(
                            text = "Scan QR instead",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.ink500,
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .clickable { /* QR Scanner placeholder */ },
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                // 2. People You Monitor Card matching Mockup Frame 2
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "People you monitor",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )

                        if (beneficiaries.isEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "You have not connected to any beneficiaries yet. Enter a 6-character code above.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.ink500,
                            )
                        } else {
                            beneficiaries.forEach { beneficiary ->
                                Spacer(Modifier.height(8.dp))
                                CaregiverBeneficiaryRow(
                                    beneficiary = beneficiary,
                                    onRemove = { onRemoveBeneficiary(beneficiary.connectionId) },
                                    onCall = { onCall(beneficiary.phone) },
                                    onOpen = {
                                        onOpenBeneficiaryChat(
                                            ChatPreview(
                                                partnerId = beneficiary.beneficiaryId,
                                                partnerName = beneficiary.name,
                                                partnerPhone = beneficiary.phone,
                                                lastMessage = "",
                                                lastTimestamp = System.currentTimeMillis(),
                                                unreadCount = 0,
                                            )
                                        )
                                    },
                                )
                            }
                        }
                    }
                }

                message?.let {
                    Spacer(Modifier.height(12.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                Spacer(Modifier.height(16.dp))
            }
        }

        // Bottom Navigation Bar
        SoundGuardBottomNav(
            selectedTab = SoundGuardTab.Home,
            onTabSelected = { tab ->
                when (tab) {
                    SoundGuardTab.Home -> {}
                    SoundGuardTab.Chat -> {
                        beneficiaries.firstOrNull()?.let { b ->
                            onOpenBeneficiaryChat(
                                ChatPreview(b.beneficiaryId, b.name, b.phone, "", System.currentTimeMillis(), 0)
                            )
                        } ?: run {
                            onOpenSettings()
                        }
                    }
                    SoundGuardTab.People -> {}
                    SoundGuardTab.Settings -> onOpenSettings()
                }
            },
            peopleTabLabel = "People",
        )
    }
}

@Composable
private fun CaregiverBeneficiaryRow(
    beneficiary: MonitoredBeneficiary,
    onRemove: () -> Unit,
    onCall: () -> Unit,
    onOpen: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AvatarCircle(text = beneficiary.name, sizeDp = 32)
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = beneficiary.name,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(2.dp))
            StatusChip(
                label = if (beneficiary.isPrimary) "ALL QUIET" else "AWAITING RESPONSE",
                tone = if (beneficiary.isPrimary) IncidentStatusTone.Success else IncidentStatusTone.Warning,
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
            if (beneficiary.phone.isNotBlank()) {
                IconButton(onClick = onCall, modifier = Modifier.size(30.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.Call,
                        contentDescription = "Call",
                        tint = MaterialTheme.colorScheme.ink700,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            Box {
                IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(30.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.MoreVert,
                        contentDescription = "Options",
                        tint = MaterialTheme.colorScheme.ink700,
                        modifier = Modifier.size(16.dp),
                    )
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text("Remove beneficiary", color = MaterialTheme.colorScheme.error) },
                        onClick = { menuExpanded = false; onRemove() },
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// SETTINGS SCREEN (FRAME 4 + DARK MODE TEST TOGGLE)
// -------------------------------------------------------------------------------------------------

@Composable
private fun SettingsScreen(
    fullName: String,
    email: String,
    phone: String,
    currentRole: String,
    microphoneGranted: Boolean,
    notificationsGranted: Boolean,
    autoApproveCameraRequests: Boolean,
    darkModeEnabled: Boolean,
    onDarkModeToggle: (Boolean) -> Unit,
    onRequestPermissions: () -> Unit,
    onFullNameChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onAutoApproveCameraRequestsChange: (Boolean) -> Unit,
    onRequestSwitchRole: () -> Unit,
    onOpenTerms: () -> Unit,
    onCameraTest: () -> Unit,
    onBack: () -> Unit,
    onSignOut: () -> Unit,
    onResetAllData: () -> Unit,
) {
    var editNameDialog by remember { mutableStateOf(false) }
    var editPhoneDialog by remember { mutableStateOf(false) }
    var tempName by remember { mutableStateOf(fullName) }
    var tempPhone by remember { mutableStateOf(phone) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState()),
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Outlined.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Settings",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)

        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {

            // 1. PROFILE SECTION matching Mockup Frame 4
            Text(
                text = "PROFILE",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.ink500,
                letterSpacing = 0.8.sp,
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)) {
                    // Full name row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { tempName = fullName; editNameDialog = true }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text("Full name", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text(fullName.ifBlank { "Not set" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.ink500)
                        }
                        Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.ink300, modifier = Modifier.size(18.dp))
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
                    // Emergency phone row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { tempPhone = phone; editPhoneDialog = true }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text("Emergency phone", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text(phone.ifBlank { "Not set" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.ink500)
                        }
                        Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.ink300, modifier = Modifier.size(18.dp))
                    }
                }
            }

            // 2. APPEARANCE (Dark mode / Light mode testing switch)
            Text(
                text = "APPEARANCE",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.ink500,
                letterSpacing = 0.8.sp,
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Outlined.DarkMode, contentDescription = null, tint = MaterialTheme.colorScheme.ink700, modifier = Modifier.size(16.dp))
                        }
                        Column {
                            Text("Dark Mode", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text(if (darkModeEnabled) "Enabled (High-contrast dark)" else "Disabled (Monochrome light)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.ink500)
                        }
                    }
                    SoundGuardSwitch(
                        checked = darkModeEnabled,
                        onCheckedChange = onDarkModeToggle,
                    )
                }
            }

            // 3. PERMISSIONS SECTION matching Mockup Frame 4
            Text(
                text = "PERMISSIONS",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.ink500,
                letterSpacing = 0.8.sp,
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)) {
                    // Microphone
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Outlined.Mic, contentDescription = null, tint = MaterialTheme.colorScheme.ink700, modifier = Modifier.size(16.dp))
                            }
                            Text("Microphone", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        }
                        SoundGuardSwitch(
                            checked = microphoneGranted,
                            onCheckedChange = { onRequestPermissions() },
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
                    // Notifications
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Outlined.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.ink700, modifier = Modifier.size(16.dp))
                            }
                            Text("Notifications", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        }
                        SoundGuardSwitch(
                            checked = notificationsGranted,
                            onCheckedChange = { onRequestPermissions() },
                        )
                    }
                }
            }

            // 4. SAFETY SECTION matching Mockup Frame 4
            Text(
                text = "SAFETY",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.ink500,
                letterSpacing = 0.8.sp,
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Auto-approve camera requests", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text("Skips your confirmation during an active incident.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.ink500)
                    }
                    Spacer(Modifier.width(8.dp))
                    SoundGuardSwitch(
                        checked = autoApproveCameraRequests,
                        onCheckedChange = onAutoApproveCameraRequestsChange,
                    )
                }
            }

            // 5. DIAGNOSTICS & PRIVACY
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onCameraTest)
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("Camera readiness test", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.ink300, modifier = Modifier.size(18.dp))
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onOpenTerms)
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("Terms & Privacy Notice", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.ink300, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // 6. DANGER ZONE & SIGN OUT matching Mockup Frame 4
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
            Spacer(Modifier.height(8.dp))

            val targetRole = if (currentRole.equals("caregiver", ignoreCase = true)) "beneficiary" else "caregiver"
            Text(
                text = "Switch to $targetRole mode",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.ink700,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onRequestSwitchRole)
                    .padding(vertical = 6.dp),
            )

            Text(
                text = "Delete all account data",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.ink500,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onResetAllData)
                    .padding(vertical = 6.dp),
            )

            Text(
                text = "Sign out",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.danger,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onSignOut)
                    .padding(vertical = 8.dp),
            )
        }
    }

    // Name Edit Dialog
    if (editNameDialog) {
        AlertDialog(
            onDismissRequest = { editNameDialog = false },
            title = { Text("Edit Full Name", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = tempName,
                    onValueChange = { tempName = it },
                    label = { Text("Full Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                Button(onClick = {
                    onFullNameChange(tempName)
                    editNameDialog = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { editNameDialog = false }) { Text("Cancel") }
            },
        )
    }

    // Phone Edit Dialog
    if (editPhoneDialog) {
        AlertDialog(
            onDismissRequest = { editPhoneDialog = false },
            title = { Text("Edit Emergency Phone", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = tempPhone,
                    onValueChange = { tempPhone = it },
                    label = { Text("Phone Number") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                Button(onClick = {
                    onPhoneChange(tempPhone)
                    editPhoneDialog = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { editPhoneDialog = false }) { Text("Cancel") }
            },
        )
    }
}
```

### `app/src/main/java/com/yuletan/soundguard/ChatScreen.kt`

```kotlin
package com.yuletan.soundguard

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    partnerName: String,
    partnerPhone: String,
    messages: List<ChatMessage>,
    loading: Boolean,
    isCaregiverView: Boolean,
    snapshotMessage: String?,
    activeIncidentBannerText: String? = null,
    onRequestPhoto: () -> Unit,
    onCall: () -> Unit,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
) {
    var showPhotoDetail by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        while (true) {
            delay(10_000L)
            onRefresh()
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Top Bar matching Frame 3
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Outlined.ArrowBack, contentDescription = "Go back", tint = MaterialTheme.colorScheme.onSurface)
            }
            Spacer(Modifier.width(4.dp))
            AvatarCircle(text = partnerName.ifBlank { "User" }, sizeDp = 34)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = partnerName.ifBlank { "Chat" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = if (partnerPhone.isNotBlank()) partnerPhone else "Active recently",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.ink500,
                )
            }

            IconButton(onClick = onRequestPhoto, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Outlined.CameraAlt, contentDescription = "Request photo", tint = MaterialTheme.colorScheme.ink700)
            }
            IconButton(onClick = onCall, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Outlined.Call, contentDescription = "Call", tint = MaterialTheme.colorScheme.ink700)
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)

        // Active Incident Banner if applicable
        if (activeIncidentBannerText != null) {
            Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                IncidentBanner(text = activeIncidentBannerText)
            }
        }

        if (loading && messages.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else if (messages.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                    Text("No messages yet", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.ink500)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Incidents and safety alerts will appear here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.ink500,
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                state = listState,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(messages, key = { "${it::class.simpleName}_${it.timestamp}" }) { message ->
                    when (message) {
                        is ChatMessage.Incident -> IncidentBubble(
                            message = message,
                            onPhotoClick = { showPhotoDetail = message.snapshotUrl },
                        )
                        is ChatMessage.PhotoRequest -> PhotoRequestBubble(
                            message = message,
                            onPhotoClick = { showPhotoDetail = message.photoUrl },
                        )
                        is ChatMessage.Acknowledgment -> AcknowledgmentBubble(message = message)
                    }
                }
            }
        }

        if (snapshotMessage != null) {
            Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (snapshotMessage.contains("failed", ignoreCase = true))
                            MaterialTheme.colorScheme.dangerTint
                        else MaterialTheme.colorScheme.surfaceVariant
                    ),
                ) {
                    Text(
                        snapshotMessage,
                        modifier = Modifier.padding(10.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (snapshotMessage.contains("failed", ignoreCase = true))
                            MaterialTheme.colorScheme.danger
                        else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }

        // Bottom Action Bar matching Frame 3
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = onCall,
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(999.dp),
                    border = androidx.compose.foundation.BorderStroke(1.4.dp, MaterialTheme.colorScheme.outline),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                ) {
                    Text("Call", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Button(
                    onClick = onRequestPhoto,
                    modifier = Modifier.weight(1.3f).height(44.dp),
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Text("Request photo", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }

    if (showPhotoDetail != null) {
        AlertDialog(
            onDismissRequest = { showPhotoDetail = null },
            title = { Text("Verification photo", fontWeight = FontWeight.Bold) },
            text = {
                showPhotoDetail?.let { url ->
                    val context = LocalContext.current
                    var bitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
                    LaunchedEffect(url) {
                        withContext(kotlinx.coroutines.Dispatchers.IO) {
                            try {
                                val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                                connection.connectTimeout = 10_000
                                connection.readTimeout = 15_000
                                val tmpFile = java.io.File(context.cacheDir, "chat_photo_${System.currentTimeMillis()}.jpg")
                                connection.inputStream.use { input -> tmpFile.outputStream().use { output -> input.copyTo(output) } }
                                bitmap = BitmapFactory.decodeFile(tmpFile.absolutePath)
                                tmpFile.delete()
                            } catch (_: Exception) {}
                        }
                    }
                    bitmap?.let { bmp ->
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Verification photo",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(360.dp)
                                .clip(RoundedCornerShape(12.dp)),
                        )
                    } ?: Box(modifier = Modifier.fillMaxWidth().height(260.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPhotoDetail = null }) {
                    Text("Close", fontWeight = FontWeight.Bold)
                }
            },
        )
    }
}

@Composable
private fun IncidentBubble(
    message: ChatMessage.Incident,
    onPhotoClick: () -> Unit,
) {
    val tone = statusTone(message.status)
    val leftAccentColor = when (tone) {
        IncidentStatusTone.Danger -> MaterialTheme.colorScheme.danger
        IncidentStatusTone.Warning -> MaterialTheme.colorScheme.warning
        IncidentStatusTone.Success -> MaterialTheme.colorScheme.success
        IncidentStatusTone.Neutral -> MaterialTheme.colorScheme.outline
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
    ) {
        // Left colored accent border
        Box(
            modifier = Modifier
                .width(4.dp)
                .matchParentSize()
                .background(leftAccentColor, RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp))
        )

        Column(modifier = Modifier.padding(start = 14.dp, top = 10.dp, end = 12.dp, bottom = 10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    StatusChip(label = formatIncidentLabel(message.status), tone = tone)
                    if (message.label.contains("test", ignoreCase = true) || message.id.contains("sim", ignoreCase = true)) {
                        StatusChip(label = "TEST", tone = IncidentStatusTone.Neutral)
                    }
                }
                Text(
                    text = formatChatBubbleTime(message.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.ink300,
                )
            }

            Spacer(Modifier.height(4.dp))
            Text(
                text = message.label,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Confidence ${(message.confidence * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.ink500,
            )

            if (message.snapshotUrl != null) {
                Spacer(Modifier.height(8.dp))
                val context = LocalContext.current
                var bitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
                LaunchedEffect(message.snapshotUrl) {
                    withContext(kotlinx.coroutines.Dispatchers.IO) {
                        try {
                            val connection = java.net.URL(message.snapshotUrl).openConnection() as java.net.HttpURLConnection
                            connection.connectTimeout = 10_000
                            connection.readTimeout = 15_000
                            val tmpFile = java.io.File(context.cacheDir, "bubble_${message.id}.jpg")
                            connection.inputStream.use { input -> tmpFile.outputStream().use { output -> input.copyTo(output) } }
                            bitmap = BitmapFactory.decodeFile(tmpFile.absolutePath)
                            tmpFile.delete()
                        } catch (_: Exception) {}
                    }
                }
                bitmap?.let { bmp ->
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "Verification photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(onClick = onPhotoClick),
                    )
                }
            }
        }
    }
}

private fun statusTone(status: String): IncidentStatusTone = when (status.lowercase()) {
    "resolved", "acknowledged", "caregiver_acknowledged" -> IncidentStatusTone.Success
    "escalated", "high" -> IncidentStatusTone.Danger
    "false_alarm", "falsealarm" -> IncidentStatusTone.Neutral
    else -> IncidentStatusTone.Warning
}

@Composable
private fun PhotoRequestBubble(message: ChatMessage.PhotoRequest, onPhotoClick: () -> Unit) {
    // A signed URL is the authoritative signal that an image is available; backend
    // status casing and approval state must not hide an already uploaded photo.
    val hasPhoto = !message.photoUrl.isNullOrBlank()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
            .clickable(enabled = hasPhoto, onClick = onPhotoClick)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (hasPhoto) {
                RemotePhotoThumbnail(url = message.photoUrl!!, onClick = onPhotoClick)
            } else {
                Box(modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                    Icon(imageVector = Icons.Outlined.Image, contentDescription = null, tint = MaterialTheme.colorScheme.ink500, modifier = Modifier.size(20.dp))
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (hasPhoto) "Verification photo" else "Photo request: ${message.status}",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = formatChatBubbleTime(message.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.ink500,
                )
            }
        }
    }
}

@Composable
private fun RemotePhotoThumbnail(url: String, onClick: () -> Unit) {
    var bitmap by remember(url) { mutableStateOf<android.graphics.Bitmap?>(null) }
    var failed by remember(url) { mutableStateOf(false) }
    LaunchedEffect(url) {
        val decoded = withContext(kotlinx.coroutines.Dispatchers.IO) {
            runCatching {
                val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                connection.connectTimeout = 10_000
                connection.readTimeout = 15_000
                connection.instanceFollowRedirects = true
                connection.inputStream.use(BitmapFactory::decodeStream)
            }.getOrNull()
        }
        bitmap = decoded
        failed = decoded == null
    }
    Box(modifier = Modifier.size(64.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceVariant).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        when {
            bitmap != null -> Image(bitmap = bitmap!!.asImageBitmap(), contentDescription = "Verification photo", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            failed -> Icon(Icons.Outlined.Image, contentDescription = "Photo could not be loaded", tint = MaterialTheme.colorScheme.ink500, modifier = Modifier.size(24.dp))
            else -> CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        }
    }
}

@Composable
private fun AcknowledgmentBubble(message: ChatMessage.Acknowledgment) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "✓ ${message.byName} acknowledged the alert",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "· ${formatChatBubbleTime(message.timestamp)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.ink500,
            )
        }
    }
}

private fun formatChatBubbleTime(timestamp: Long): String {
    return SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(timestamp))
}
```

### `app/src/main/java/com/yuletan/soundguard/ChatListScreen.kt`

```kotlin
package com.yuletan.soundguard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatListScreen(
    title: String,
    subtitle: String,
    previews: List<ChatPreview>,
    loading: Boolean,
    onOpenChat: (ChatPreview) -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Outlined.ArrowBack, contentDescription = "Go back", tint = MaterialTheme.colorScheme.onSurface)
            }
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.ink500,
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)

        if (loading && previews.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else if (previews.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                    Text(
                        "No conversations yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.ink500,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Connected beneficiaries and caregivers will appear here.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.ink500,
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                items(previews, key = { it.partnerId }) { preview ->
                    ChatListRow(preview = preview, onClick = { onOpenChat(preview) })
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        thickness = 1.dp,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatListRow(
    preview: ChatPreview,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AvatarCircle(text = preview.partnerName, sizeDp = 40)

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = preview.partnerName,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = formatChatListTime(preview.lastTimestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.ink300,
                )
            }
            Spacer(Modifier.height(2.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = preview.lastMessage.ifBlank { "Tap to open conversation" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.ink500,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (preview.unreadCount > 0) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = if (preview.unreadCount > 9) "9+" else "${preview.unreadCount}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                        )
                    }
                }
            }
        }
    }
}

private fun formatChatListTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    val days = diff / (24 * 60 * 60 * 1000)

    return when {
        days == 0L -> SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(timestamp))
        days == 1L -> "Yesterday"
        days < 7L -> SimpleDateFormat("EEE", Locale.getDefault()).format(Date(timestamp))
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp))
    }
}
```

### `app/src/main/java/com/yuletan/soundguard/IncidentStateMachine.kt`

```kotlin
package com.yuletan.soundguard

import java.util.UUID

enum class IncidentStatus {
    Detected,
    WaitingUser,
    CaregiverNotified,
    CaregiverAcknowledged,
    Resolved,
    FalseAlarm,
    Escalated,
}

enum class BeneficiaryResponse {
    Safe,
    NeedHelp,
}

data class IncidentRecord(
    val id: String,
    val soundLabel: String,
    val severity: SoundSeverity,
    val confidence: Float,
    val status: IncidentStatus,
    val startedAt: Long,
    val userResponse: BeneficiaryResponse? = null,
    val caregiverIndex: Int? = null,
    val nextDeadlineAt: Long? = null,
    val resolvedAt: Long? = null,
)

/**
 * Local incident contract shared by audio monitoring and the eventual backend
 * notification worker. Time is supplied by callers so transitions are testable.
 */
class IncidentStateMachine(
    private val beneficiaryWindowMs: Long = TWO_MINUTES_MS,
    private val caregiverWindowMs: Long = TWO_MINUTES_MS,
    private val maxHistory: Int = 50,
) {
    companion object {
        const val TWO_MINUTES_MS = 120_000L
        private val TERMINAL_STATUSES = setOf(
            IncidentStatus.CaregiverAcknowledged,
            IncidentStatus.Resolved,
            IncidentStatus.FalseAlarm,
        )
    }

    private var active: IncidentRecord? = null
    private val history = ArrayDeque<IncidentRecord>()
    private var caregiverCount = 0

    fun setCaregiverCount(count: Int) {
        caregiverCount = count.coerceAtLeast(0)
    }

    fun activeIncident(): IncidentRecord? = active

    fun history(): List<IncidentRecord> = history.toList()

    fun reset() {
        active = null
        history.clear()
    }

    fun detect(
        soundLabel: String,
        severity: SoundSeverity,
        confidence: Float,
        now: Long,
    ): IncidentRecord? {
        if (severity == SoundSeverity.None) return null
        if (active != null && active!!.status !in TERMINAL_STATUSES) return active

        val incident = IncidentRecord(
            id = UUID.randomUUID().toString(),
            soundLabel = soundLabel,
            severity = severity,
            confidence = confidence.coerceIn(0f, 1f),
            status = if (severity == SoundSeverity.High) IncidentStatus.WaitingUser else IncidentStatus.Detected,
            startedAt = now,
            nextDeadlineAt = if (severity == SoundSeverity.High) now + beneficiaryWindowMs else null,
        )
        if (severity == SoundSeverity.Low) {
            history.addLast(incident)
            while (history.size > maxHistory) history.removeFirst()
            active = null
            return incident
        }
        active = incident
        return incident
    }

    fun respondAsBeneficiary(response: BeneficiaryResponse, now: Long): IncidentRecord? {
        val incident = active ?: return null
        if (incident.status != IncidentStatus.WaitingUser) return incident
        return update(
            incident.copy(
                status = if (response == BeneficiaryResponse.Safe) IncidentStatus.FalseAlarm else IncidentStatus.CaregiverNotified,
                userResponse = response,
                caregiverIndex = if (response == BeneficiaryResponse.NeedHelp) firstCaregiverOrNull() else null,
                nextDeadlineAt = if (response == BeneficiaryResponse.NeedHelp) caregiverDeadline(now) else null,
                resolvedAt = if (response == BeneficiaryResponse.Safe) now else null,
            ),
            now,
        )
    }

    fun acknowledgeCaregiver(now: Long): IncidentRecord? {
        val incident = active ?: return null
        if (incident.status !in setOf(IncidentStatus.CaregiverNotified, IncidentStatus.Escalated)) return incident
        return update(incident.copy(status = IncidentStatus.CaregiverAcknowledged, nextDeadlineAt = null, resolvedAt = now), now)
    }

    /** Advances expired response windows and returns the current incident. */
    fun advance(now: Long): IncidentRecord? {
        val incident = active ?: return null
        if (incident.nextDeadlineAt == null || now < incident.nextDeadlineAt) return incident

        return when (incident.status) {
            IncidentStatus.WaitingUser -> {
                if (caregiverCount == 0) {
                    update(incident.copy(status = IncidentStatus.Escalated, nextDeadlineAt = null), now)
                } else {
                    update(
                        incident.copy(
                            status = IncidentStatus.CaregiverNotified,
                            caregiverIndex = 0,
                            nextDeadlineAt = now + caregiverWindowMs,
                        ),
                        now,
                    )
                }
            }
            IncidentStatus.CaregiverNotified,
            IncidentStatus.Escalated -> {
                val next = (incident.caregiverIndex ?: -1) + 1
                if (next < caregiverCount) {
                    update(incident.copy(status = IncidentStatus.Escalated, caregiverIndex = next, nextDeadlineAt = now + caregiverWindowMs), now)
                } else {
                    update(incident.copy(status = IncidentStatus.Escalated, nextDeadlineAt = null), now)
                }
            }
            else -> incident
        }
    }

    private fun firstCaregiverOrNull(): Int? = if (caregiverCount > 0) 0 else null

    private fun caregiverDeadline(now: Long): Long? = if (caregiverCount > 0) now + caregiverWindowMs else null

    private fun update(next: IncidentRecord, now: Long): IncidentRecord {
        active = next
        if (next.status in TERMINAL_STATUSES) {
            history.addLast(next)
            while (history.size > maxHistory) history.removeFirst()
            if (next.status != IncidentStatus.CaregiverAcknowledged) active = next
        }
        return next
    }

}
```

### `app/src/main/java/com/yuletan/soundguard/ChatMessage.kt`

```kotlin
package com.yuletan.soundguard

sealed class ChatMessage {
    abstract val timestamp: Long

    data class Incident(
        val id: String,
        val label: String,
        val severity: String,
        val confidence: Float,
        val status: String,
        override val timestamp: Long,
        val snapshotUrl: String? = null,
    ) : ChatMessage()

    data class PhotoRequest(
        val incidentId: String,
        val status: String,
        override val timestamp: Long,
        val photoUrl: String? = null,
    ) : ChatMessage()

    data class Acknowledgment(
        override val timestamp: Long,
        val byName: String,
    ) : ChatMessage()
}
```

### `app/src/main/java/com/yuletan/soundguard/ChatRepository.kt`

```kotlin
package com.yuletan.soundguard

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ChatPreview(
    val partnerId: String,
    val partnerName: String,
    val partnerPhone: String,
    val lastMessage: String,
    val lastTimestamp: Long,
    val unreadCount: Int,
)

class ChatRepository(context: Context) {
    private val notificationClient = NotificationClient(context)
    private val incidentClient = IncidentClient(context)
    private val snapshotClient = SnapshotClient(context)
    private val careClient = CareClient(context)

    suspend fun buildChatMessages(partnerId: String, isCaregiverView: Boolean): Result<List<ChatMessage>> = withContext(Dispatchers.IO) {
        runCatching {
            val messages = mutableListOf<ChatMessage>()

            if (isCaregiverView) {
                val notifications = notificationClient.fetchForConnection(partnerId).getOrNull().orEmpty()
                for (notification in notifications) {
                    val ts = parseIsoTimestamp(notification.incidentStartedAt)
                        ?: parseIsoTimestamp(notification.createdAt)
                        ?: continue
                    messages.add(
                        ChatMessage.Incident(
                            id = notification.incidentId,
                            label = notification.soundLabel.ifBlank { "Alert" },
                            severity = notification.severity,
                            confidence = notification.confidence,
                            status = notification.status,
                            timestamp = ts,
                        )
                    )
                    if (notification.status == "acknowledged") {
                        messages.add(
                            ChatMessage.Acknowledgment(timestamp = ts + 1000, byName = "Caregiver")
                        )
                    }
                }

                val latestIncidentId = notifications.lastOrNull()?.incidentId
                if (latestIncidentId != null) {
                    val snapshots = snapshotClient.fetchSnapshotsForIncident(latestIncidentId).getOrNull().orEmpty()
                    val bestSnapshot = pickBestSnapshot(snapshots)
                    if (bestSnapshot != null) {
                        val expiresTs = bestSnapshot.expiresAt?.let { parseIsoTimestamp(it) }
                        val snapshotTs = expiresTs?.minus(600_000) ?: System.currentTimeMillis()
                        val isExpired = expiresTs != null && System.currentTimeMillis() > expiresTs
                        messages.add(
                            ChatMessage.PhotoRequest(
                                incidentId = latestIncidentId,
                                status = bestSnapshot.uploadStatus ?: bestSnapshot.approvalStatus ?: "requested",
                                timestamp = snapshotTs,
                                photoUrl = if (!isExpired) bestSnapshot.signedUrl else null,
                            )
                        )
                    }
                }
            } else {
                val incidents = incidentClient.fetchOwnIncidents().getOrNull().orEmpty()
                for (incident in incidents) {
                    val ts = parseIsoTimestamp(incident.startedAt) ?: continue
                    messages.add(
                        ChatMessage.Incident(
                            id = incident.id,
                            label = incident.label,
                            severity = incident.severity,
                            confidence = incident.confidence,
                            status = incident.status,
                            timestamp = ts,
                        )
                    )
                }

                val latestIncidentId = incidents.lastOrNull()?.id
                if (latestIncidentId != null) {
                    val snapshots = snapshotClient.fetchSnapshotsForIncident(latestIncidentId).getOrNull().orEmpty()
                    val bestSnapshot = pickBestSnapshot(snapshots)
                    if (bestSnapshot != null) {
                        val expiresTs = bestSnapshot.expiresAt?.let { parseIsoTimestamp(it) }
                        val snapshotTs = expiresTs?.minus(600_000) ?: System.currentTimeMillis()
                        val isExpired = expiresTs != null && System.currentTimeMillis() > expiresTs
                        messages.add(
                            ChatMessage.PhotoRequest(
                                incidentId = latestIncidentId,
                                status = bestSnapshot.uploadStatus ?: bestSnapshot.approvalStatus ?: "requested",
                                timestamp = snapshotTs,
                                photoUrl = if (!isExpired) bestSnapshot.signedUrl else null,
                            )
                        )
                    }
                }
            }

            messages.sortedBy { it.timestamp }
        }
    }

    suspend fun buildChatListForCaregiver(): Result<List<ChatPreview>> = withContext(Dispatchers.IO) {
        runCatching {
            val beneficiaries = careClient.fetchBeneficiariesForCaregiver().getOrNull().orEmpty()
            val previews = mutableListOf<ChatPreview>()

            for (beneficiary in beneficiaries) {
                val notifications = notificationClient.fetchForConnection(beneficiary.beneficiaryId).getOrNull().orEmpty()
                val lastNotification = notifications.lastOrNull()
                val ts = if (lastNotification != null) parseIsoTimestamp(lastNotification.createdAt) ?: System.currentTimeMillis() else System.currentTimeMillis()
                val unread = notifications.count { it.status != "acknowledged" }
                val lastMsg = if (lastNotification != null) {
                    "${lastNotification.soundLabel.ifBlank { "Alert" }} • ${lastNotification.severity}"
                } else {
                    "No incidents yet"
                }
                previews.add(
                    ChatPreview(
                        partnerId = beneficiary.beneficiaryId,
                        partnerName = beneficiary.name,
                        partnerPhone = beneficiary.phone,
                        lastMessage = lastMsg,
                        lastTimestamp = ts,
                        unreadCount = unread,
                    )
                )
            }

            previews.sortedByDescending { it.lastTimestamp }
        }
    }

    suspend fun buildChatListForBeneficiary(): Result<List<ChatPreview>> = withContext(Dispatchers.IO) {
        runCatching {
            val caregivers = careClient.fetchCaregiversForBeneficiary().getOrNull().orEmpty()
            val previews = mutableListOf<ChatPreview>()

            for (caregiver in caregivers) {
                val notifications = notificationClient.fetchForBeneficiary().getOrNull().orEmpty()
                val caregiverNotifications = notifications.filter { true }
                val lastNotification = caregiverNotifications.lastOrNull()
                val ts = if (lastNotification != null) parseIsoTimestamp(lastNotification.createdAt) ?: System.currentTimeMillis() else System.currentTimeMillis()
                val unread = caregiverNotifications.count { it.status != "acknowledged" }
                val lastMsg = if (lastNotification != null) {
                    "${lastNotification.soundLabel.ifBlank { "Alert" }} • ${lastNotification.severity}"
                } else {
                    "No incidents yet"
                }
                previews.add(
                    ChatPreview(
                        partnerId = caregiver.caregiverId,
                        partnerName = caregiver.name,
                        partnerPhone = caregiver.phone,
                        lastMessage = lastMsg,
                        lastTimestamp = ts,
                        unreadCount = unread,
                    )
                )
            }

            previews.sortedByDescending { it.lastTimestamp }
        }
    }

    private fun pickBestSnapshot(snapshots: List<SnapshotClient.SnapshotWithUrl>): SnapshotClient.SnapshotWithUrl? {
        if (snapshots.isEmpty()) return null
        val statusRank = mapOf(
            "uploaded" to 3,
            "viewed" to 2,
            "approved" to 2,
            "requested" to 1,
            "pending" to 1,
            "declined" to 0,
            "failed" to 0,
            "expired" to 0,
        )
        return snapshots.maxByOrNull { snapshot ->
            val uploadRank = statusRank[snapshot.uploadStatus] ?: 0
            val approvalRank = statusRank[snapshot.approvalStatus] ?: 0
            uploadRank * 10 + approvalRank
        }
    }

    private fun parseIsoTimestamp(iso: String): Long? {
        return try {
            java.time.Instant.parse(iso).toEpochMilli()
        } catch (_: Exception) {
            null
        }
    }
}
```

### `app/src/main/java/com/yuletan/soundguard/ProfileClient.kt`

```kotlin
package com.yuletan.soundguard

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class UserProfile(
    val id: String,
    val email: String,
    val fullName: String,
    val phone: String,
    val role: String,
    val setupCompletedAt: String?,
)

data class BeneficiarySettings(
    val autoApproveCameraRequests: Boolean,
)

class ProfileClient(context: Context) {
    private val authClient = AuthClient(context)

    suspend fun fetchMyProfile(): Result<UserProfile?> = withContext(Dispatchers.IO) {
        runCatching {
            val token = authClient.getToken()
            val userId = authClient.userId() ?: return@runCatching null
            if (BuildConfig.SUPABASE_URL.isBlank() || BuildConfig.SUPABASE_ANON_KEY.isBlank()) {
                error("Supabase configuration is missing in local.properties.")
            }

            val endpoint = BuildConfig.SUPABASE_URL.trimEnd('/') +
                "/rest/v1/profiles?id=eq.$userId&select=id,email,full_name,phone,role,setup_completed_at"
            val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15_000
                readTimeout = 20_000
                setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY)
                setRequestProperty("Authorization", "Bearer $token")
            }

            try {
                val responseCode = connection.responseCode
                val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
                val responseBody = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
                if (responseCode !in 200..299) {
                    error("Profile fetch failed with HTTP $responseCode: $responseBody")
                }
                val jsonArray = org.json.JSONArray(responseBody)
                if (jsonArray.length() == 0) return@runCatching null
                val obj = jsonArray.getJSONObject(0)
                UserProfile(
                    id = obj.optString("id", userId),
                    email = obj.optString("email", ""),
                    fullName = obj.optString("full_name", ""),
                    phone = obj.optString("phone", ""),
                    role = obj.optString("role", "")
                        .takeUnless { it.equals("null", ignoreCase = true) }
                        .orEmpty(),
                    setupCompletedAt = obj.optString("setup_completed_at").takeIf { it.isNotBlank() },
                )
            } finally {
                connection.disconnect()
            }
        }
    }

    suspend fun saveProfile(
        email: String,
        fullName: String,
        phone: String,
        role: String,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val token = authClient.getToken()
            val userId = authClient.userId() ?: error("No authenticated user was found.")
            if (BuildConfig.SUPABASE_URL.isBlank() || BuildConfig.SUPABASE_ANON_KEY.isBlank()) {
                error("Supabase configuration is missing in local.properties.")
            }

            val endpoint = BuildConfig.SUPABASE_URL.trimEnd('/') + "/rest/v1/profiles"
            val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15_000
                readTimeout = 20_000
                setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY)
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Prefer", "resolution=merge-duplicates,return=minimal")
                doOutput = true
            }
            val nowIso = java.time.Instant.now().toString()
            val body = JSONObject().apply {
                put("id", userId)
                put("email", email.trim())
                put("full_name", fullName.trim())
                put("phone", phone.trim())
                put("role", role.lowercase())
                put("setup_completed_at", nowIso)
            }
            try {
                connection.outputStream.use { it.write(body.toString().toByteArray()) }
                val responseCode = connection.responseCode
                if (responseCode !in 200..299) {
                    val response = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                    error("Profile save failed with HTTP $responseCode: ${response.ifBlank { "empty response" }}")
                }
            } finally {
                connection.disconnect()
            }
        }
    }

    suspend fun saveBeneficiarySettings(
        monitoringConsent: Boolean,
        shareWithCaregiver: Boolean = false,
        cameraRequestsConsent: Boolean = false,
        autoApproveCameraRequests: Boolean = true,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val token = authClient.getToken()
            val userId = authClient.userId() ?: error("No authenticated user was found.")
            if (BuildConfig.SUPABASE_URL.isBlank() || BuildConfig.SUPABASE_ANON_KEY.isBlank()) {
                error("Supabase configuration is missing in local.properties.")
            }
            val endpoint = BuildConfig.SUPABASE_URL.trimEnd('/') + "/rest/v1/beneficiary_settings"
            val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15_000
                readTimeout = 20_000
                setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY)
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Prefer", "resolution=merge-duplicates,return=minimal")
                doOutput = true
            }
            val body = JSONObject().apply {
                put("user_id", userId)
                put("consent_monitoring", monitoringConsent)
                put("consent_share_with_caregiver", shareWithCaregiver)
                put("consent_camera_requests", cameraRequestsConsent)
                put("auto_approve_camera_requests", autoApproveCameraRequests)
                put("updated_at", java.time.Instant.now().toString())
            }
            try {
                connection.outputStream.use { it.write(body.toString().toByteArray()) }
                val responseCode = connection.responseCode
                if (responseCode !in 200..299) {
                    val response = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                    error("Beneficiary settings save failed with HTTP $responseCode: ${response.ifBlank { "empty response" }}")
                }
            } finally {
                connection.disconnect()
            }
        }
    }

    suspend fun fetchBeneficiarySettings(): Result<BeneficiarySettings> = withContext(Dispatchers.IO) {
        runCatching {
            val token = authClient.getToken()
            val userId = authClient.userId() ?: error("No authenticated user was found.")
            val endpoint = BuildConfig.SUPABASE_URL.trimEnd('/') +
                "/rest/v1/beneficiary_settings?user_id=eq.$userId&select=auto_approve_camera_requests"
            val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15_000
                readTimeout = 20_000
                setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY)
                setRequestProperty("Authorization", "Bearer $token")
            }
            try {
                val code = connection.responseCode
                val response = (if (code in 200..299) connection.inputStream else connection.errorStream)
                    ?.bufferedReader()?.use { it.readText() }.orEmpty()
                if (code !in 200..299) error("Beneficiary settings fetch failed with HTTP $code: $response")
                val row = org.json.JSONArray(response).optJSONObject(0)
                BeneficiarySettings(row?.optBoolean("auto_approve_camera_requests", true) ?: true)
            } finally {
                connection.disconnect()
            }
        }
    }

    suspend fun resetRole(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val token = authClient.getToken()
            val userId = authClient.userId() ?: error("No authenticated user was found.")
            val endpoint = BuildConfig.SUPABASE_URL.trimEnd('/') + "/rest/v1/profiles?id=eq.$userId"
            val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15_000
                readTimeout = 20_000
                setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY)
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("X-HTTP-Method-Override", "PATCH")
                doOutput = true
            }
            val body = JSONObject().apply {
                put("role", JSONObject.NULL)
                put("setup_completed_at", JSONObject.NULL)
            }
            try {
                connection.outputStream.use { it.write(body.toString().toByteArray()) }
                val responseCode = connection.responseCode
                if (responseCode !in 200..299) {
                    val response = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                    error("Role reset failed with HTTP $responseCode: $response")
                }
            } finally {
                connection.disconnect()
            }
        }
    }

    suspend fun resetAllAccountData(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val token = authClient.getToken()
            val endpoint = BuildConfig.SUPABASE_URL.trimEnd('/') + "/rest/v1/rpc/reset_my_account_data"
            val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15_000
                readTimeout = 30_000
                setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY)
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
            }
            try {
                connection.outputStream.use { it.write("{}".toByteArray()) }
                val responseCode = connection.responseCode
                if (responseCode !in 200..299) {
                    val response = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                    error("Account reset failed with HTTP $responseCode: $response")
                }
            } finally {
                connection.disconnect()
            }
        }
    }
}
```

### `app/src/main/java/com/yuletan/soundguard/CareClient.kt`

```kotlin
package com.yuletan.soundguard

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class CaregiverMember(
    val connectionId: String,
    val caregiverId: String,
    val name: String,
    val phone: String,
    val email: String,
    val isPrimary: Boolean,
    val escalationOrder: Int,
    val status: String,
)

data class MonitoredBeneficiary(
    val connectionId: String,
    val beneficiaryId: String,
    val name: String,
    val phone: String,
    val email: String,
    val isPrimary: Boolean,
    val status: String,
)

class CareClient(context: Context) {
    companion object {
        private const val TAG = "CareClient"
    }

    private val authClient = AuthClient(context)

    suspend fun linkDemoJames(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val token = authClient.getToken()
            val endpoint = BuildConfig.SUPABASE_URL.trimEnd('/') + "/rest/v1/rpc/link_demo_james"
            val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15_000
                readTimeout = 20_000
                setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY)
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
            }
            try {
                connection.outputStream.use { it.write("{}".toByteArray()) }
                val responseCode = connection.responseCode
                if (responseCode !in 200..299) {
                    val response = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                    error("Demo caregiver link failed with HTTP $responseCode: $response")
                }
            } finally {
                connection.disconnect()
            }
        }
    }

    suspend fun createPairingCode(): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val token = authClient.getToken()
            val endpoint = BuildConfig.SUPABASE_URL.trimEnd('/') + "/rest/v1/rpc/create_pairing_code"
            val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15_000
                readTimeout = 20_000
                setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY)
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
            }

            try {
                connection.outputStream.use { it.write("{}".toByteArray()) }
                val responseCode = connection.responseCode
                val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
                val responseBody = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
                if (responseCode !in 200..299) {
                    error("Failed to generate code (HTTP $responseCode): $responseBody")
                }
                // Supabase RPC returns plain text or JSON string like "ABC123" or "\"ABC123\""
                responseBody.trim().trim('"')
            } finally {
                connection.disconnect()
            }
        }
    }

    suspend fun acceptPairingCode(code: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val token = authClient.getToken()
            val endpoint = BuildConfig.SUPABASE_URL.trimEnd('/') + "/rest/v1/rpc/accept_pairing_code"
            val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15_000
                readTimeout = 20_000
                setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY)
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
            }

            val body = JSONObject().apply {
                put("p_code", code.trim().uppercase())
            }

            try {
                connection.outputStream.use { it.write(body.toString().toByteArray()) }
                val responseCode = connection.responseCode
                val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
                val responseBody = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
                if (responseCode !in 200..299) {
                    val message = runCatching { JSONObject(responseBody).optString("message") }.getOrNull()
                        ?: responseBody
                    error(message.ifBlank { "Invalid or expired pairing code" })
                }
                val obj = JSONObject(responseBody)
                obj.optString("beneficiary_name", "Beneficiary")
            } finally {
                connection.disconnect()
            }
        }
    }

    suspend fun fetchCaregiversForBeneficiary(): Result<List<CaregiverMember>> = withContext(Dispatchers.IO) {
        runCatching {
            val token = authClient.getToken()
            val userId = authClient.userId() ?: error("No user found.")

            // Fetch care connections
            val endpoint = BuildConfig.SUPABASE_URL.trimEnd('/') +
                "/rest/v1/care_connections?beneficiary_id=eq.$userId&select=id,caregiver_id,is_primary,escalation_order,status&order=escalation_order.asc"
            val rawConnections = executeGet(endpoint, token)
            val connArray = JSONArray(rawConnections)
            if (connArray.length() == 0) return@runCatching emptyList()

            // Fetch profile info for each caregiver
            val caregiverIds = mutableListOf<String>()
            for (i in 0 until connArray.length()) {
                caregiverIds.add(connArray.getJSONObject(i).getString("caregiver_id"))
            }

            val profilesMap = fetchProfilesByIds(caregiverIds, token)

            val result = mutableListOf<CaregiverMember>()
            for (i in 0 until connArray.length()) {
                val conn = connArray.getJSONObject(i)
                val cId = conn.getString("caregiver_id")
                val profile = profilesMap[cId]
                result.add(
                    CaregiverMember(
                        connectionId = conn.getString("id"),
                        caregiverId = cId,
                        name = profile?.optString("full_name")?.ifBlank { null }
                            ?: profile?.optString("email")
                            ?: "Caregiver",
                        phone = profile?.optString("phone").orEmpty(),
                        email = profile?.optString("email").orEmpty(),
                        isPrimary = conn.optBoolean("is_primary", false),
                        escalationOrder = conn.optInt("escalation_order", i + 1),
                        status = conn.optString("status", "active"),
                    )
                )
            }
            result
        }
    }

    suspend fun fetchBeneficiariesForCaregiver(): Result<List<MonitoredBeneficiary>> = withContext(Dispatchers.IO) {
        runCatching {
            val token = authClient.getToken()
            val userId = authClient.userId() ?: error("No user found.")

            val endpoint = BuildConfig.SUPABASE_URL.trimEnd('/') +
                "/rest/v1/care_connections?caregiver_id=eq.$userId&status=eq.active&select=id,beneficiary_id,is_primary,status&order=created_at.desc"
            val rawConnections = executeGet(endpoint, token)
            val connArray = JSONArray(rawConnections)
            if (connArray.length() == 0) return@runCatching emptyList()

            val beneficiaryIds = mutableListOf<String>()
            for (i in 0 until connArray.length()) {
                beneficiaryIds.add(connArray.getJSONObject(i).getString("beneficiary_id"))
            }

            val profilesMap = fetchProfilesByIds(beneficiaryIds, token)

            val result = mutableListOf<MonitoredBeneficiary>()
            for (i in 0 until connArray.length()) {
                val conn = connArray.getJSONObject(i)
                val bId = conn.getString("beneficiary_id")
                val profile = profilesMap[bId]
                result.add(
                    MonitoredBeneficiary(
                        connectionId = conn.getString("id"),
                        beneficiaryId = bId,
                        name = profile?.optString("full_name")?.ifBlank { null }
                            ?: profile?.optString("email")
                            ?: "Beneficiary",
                        phone = profile?.optString("phone").orEmpty(),
                        email = profile?.optString("email").orEmpty(),
                        isPrimary = conn.optBoolean("is_primary", false),
                        status = conn.optString("status", "active"),
                    )
                )
            }
            result
        }
    }

    suspend fun removeCareConnection(connectionId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val token = authClient.getToken()
            val endpoint = BuildConfig.SUPABASE_URL.trimEnd('/') +
                "/rest/v1/care_connections?id=eq.$connectionId"
            val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "DELETE"
                connectTimeout = 15_000
                readTimeout = 20_000
                setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY)
                setRequestProperty("Authorization", "Bearer $token")
            }
            try {
                val code = connection.responseCode
                if (code !in 200..299) {
                    val err = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                    error("Remove failed (HTTP $code): $err")
                }
            } finally {
                connection.disconnect()
            }
        }
    }

    suspend fun setPrimaryCaregiver(
        beneficiaryId: String,
        targetConnectionId: String,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val token = authClient.getToken()
            // Clear primary for all caregivers of this beneficiary
            val resetEndpoint = BuildConfig.SUPABASE_URL.trimEnd('/') +
                "/rest/v1/care_connections?beneficiary_id=eq.$beneficiaryId"
            executePatch(resetEndpoint, JSONObject().put("is_primary", false), token)

            // Set primary for target
            val setEndpoint = BuildConfig.SUPABASE_URL.trimEnd('/') +
                "/rest/v1/care_connections?id=eq.$targetConnectionId"
            executePatch(setEndpoint, JSONObject().put("is_primary", true).put("escalation_order", 1), token)
        }
    }

    suspend fun countActiveConnections(): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val token = authClient.getToken()
            val userId = authClient.userId() ?: error("No user found.")
            val endpoint = BuildConfig.SUPABASE_URL.trimEnd('/') +
                "/rest/v1/care_connections?or=(beneficiary_id.eq.$userId,caregiver_id.eq.$userId)&status=eq.active&select=id"
            val raw = executeGet(endpoint, token)
            val array = JSONArray(raw)
            array.length()
        }
    }

    private fun fetchProfilesByIds(ids: List<String>, token: String): Map<String, JSONObject> {
        if (ids.isEmpty()) return emptyMap()
        val inFilter = "in.(" + ids.joinToString(",") + ")"
        val endpoint = BuildConfig.SUPABASE_URL.trimEnd('/') +
            "/rest/v1/profiles?id=$inFilter&select=id,full_name,email,phone"
        val response = executeGet(endpoint, token)
        val array = JSONArray(response)
        val map = mutableMapOf<String, JSONObject>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            map[obj.getString("id")] = obj
        }
        return map
    }

    private fun executeGet(endpoint: String, token: String): String {
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 20_000
            setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY)
            setRequestProperty("Authorization", "Bearer $token")
        }
        return try {
            val responseCode = connection.responseCode
            val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (responseCode !in 200..299) error("HTTP $responseCode: $body")
            body
        } finally {
            connection.disconnect()
        }
    }

    private fun executePatch(endpoint: String, body: JSONObject, token: String) {
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 20_000
            setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY)
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("X-HTTP-Method-Override", "PATCH")
            doOutput = true
        }
        try {
            connection.outputStream.use { it.write(body.toString().toByteArray()) }
            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                val err = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                error("PATCH HTTP $responseCode: $err")
            }
        } finally {
            connection.disconnect()
        }
    }
}
```
