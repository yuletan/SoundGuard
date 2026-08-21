# SoundGuard

SoundGuard is an Android-first safety support application designed for people who may spend time alone. It continuously listens to environmental sounds using the device's microphone, classifies them in real-time with on-device machine learning, and connects a beneficiary to a network of caregivers who can respond when help may be needed.

> **Disclaimer:** SoundGuard is a hackathon prototype. It is not a medical device, certified emergency system, or replacement for emergency services such as 911/999/112. It is not intended for clinical or life-critical use.

---

## Purpose

Many people — elderly individuals, people living alone, those with medical conditions — may need a safety net when no one is physically present. SoundGuard fills this gap by turning an ordinary Android phone into a passive sound-awareness system. When the phone detects a concerning sound (a smoke alarm, breaking glass, crying, or an explosion), it initiates a structured response flow that gives the beneficiary a chance to confirm they are okay, and if they cannot, systematically notifies designated caregivers.

---

## Target Audiences

| Audience | How SoundGuard Helps |
|---|---|
| **Elderly individuals living alone** | Automatic detection of smoke alarms, falls, or unusual sounds triggers caregiver alerts without any action required from the beneficiary. |
| **People with medical conditions** | A structured response window gives the beneficiary time to confirm they are safe; if they cannot respond, caregivers are escalated to automatically. |
| **Family caregivers (adult children, partners)** | Peace of mind through a chat-based incident timeline, camera verification requests, and call-to-action buttons when an alert fires. |
| **Home care agencies** | Many-to-many model allows one caregiver to monitor multiple beneficiaries, with primary and backup escalation ordering. |
| **Hackathon judges and developers** | A full-stack reference implementation spanning on-device ML, real-time database, push notifications, camera verification, and a state-machine-driven escalation engine. |

---

## Features

### Core Safety

- **Real-time sound classification** — On-device YAMNet TensorFlow Lite model classifies 521 sound classes into 26 display categories, running continuously through an Android foreground service.
- **High-severity emergency alerts** — Sounds like crying, glass breaking, fire, smoke alarms, explosions, and sirens trigger a structured incident flow with a 2-minute beneficiary response window.
- **Sequential caregiver escalation** — If the beneficiary does not respond, alerts escalate to the primary caregiver, then sequentially to backup caregivers with 2-minute acknowledgement windows each.
- **Medium-severity monitoring** — Events like coughing, sneezing, thunder, door knocking, and alarms/telephone are logged to the incident timeline without triggering an emergency flow.
- **Low-severity context** — Animal sounds, water running, and similar events provide environmental context in the timeline.

### Beneficiary-Caregiver System

- **Role-based app** — The same APK serves both beneficiaries (people being monitored) and caregivers (people who respond to alerts).
- **Many-to-many connections** — One beneficiary can have multiple caregivers; one caregiver can support multiple beneficiaries.
- **6-character pairing codes** — Beneficiaries generate short-lived codes that caregivers enter to establish a connection. Codes expire after 24 hours.
- **Primary and backup caregivers** — Care connections carry `is_primary` and `escalation_order` fields to control who is notified first.
- **Connection management** — Caregivers can be removed, primary caregivers can be reassigned, and connections can be revoked.

### Camera Verification

- **Caregiver-triggered photo capture** — During an active incident, a caregiver can request a verification photo from the beneficiary's device.
- **Consent-first design** — Camera consent is collected during setup and can be revoked at any time in Settings. An auto-approve toggle lets beneficiaries opt in to skip the approval prompt.
- **CameraX integration** — Uses CameraX with camera2 for reliable photo capture across device manufacturers.
- **Private, time-limited storage** — Photos are uploaded to a private Supabase Storage bucket and automatically expire after 30 minutes via a `pg_cron` job.
- **RLS-protected access** — Only the requesting caregiver and the beneficiary can view snapshots; Row Level Security policies enforce this at the database level.

### Chat and Incident Timeline

- **WhatsApp-style chat** — Each caregiver-beneficiary pair has a conversation-style timeline built from incident records, notification acknowledgements, and photo requests.
- **Message types** — Incidents (with severity, confidence, and status), photo requests (with approval status and expiry countdowns), and acknowledgements.
- **Deep linking** — Push notifications open directly to the relevant chat via `soundguard://chat?beneficiary_id=...` intents.
- **Clear chat** — Caregivers can clear the incident history for a specific beneficiary, which deletes the underlying database records and storage objects.

### Notifications and Alerts

- **Firebase Cloud Messaging** — Server-side notification triggers automatically create FCM data messages when incidents transition through the state machine.
- **Foreground service notifications** — An ongoing low-importance notification shows that monitoring is active; high-importance heads-up notifications appear for emergency alerts.
- **Device token management** — FCM tokens are registered in Supabase on app start and on token refresh, ensuring notifications reach the correct device.

### Settings and Preferences

- **Profile editing** — Full name and emergency phone number can be updated at any time.
- **Permission management** — Microphone, notifications, battery optimization, and camera permissions are all managed from a single Settings screen.
- **Dark mode** — Full dark theme support with a high-contrast color system.
- **Auto-approve camera requests** — Beneficiaries can toggle whether photo requests are automatically approved or require manual confirmation.
- **Role switching** — Users can switch between beneficiary and caregiver roles after removing all active connections.
- **Account data reset** — A full data wipe is available, deleting all connections, incidents, snapshots, and profile data.

---

## Tech Stack

### Android Client

| Component | Technology |
|---|---|
| Language | Kotlin |
| UI Framework | Jetpack Compose with Material 3 |
| Sound Classification | YAMNet TensorFlow Lite (`yamnet.tflite`, 15,600-sample input window) |
| Camera | CameraX (camera2 + camera-lifecycle + camera-view) |
| Push Notifications | Firebase Cloud Messaging (`firebase-bom:33.7.0`) |
| Phone Number Validation | libphonenumber `8.13.55` |
| Build System | Gradle `8.10.2`, Kotlin `2.0.21`, AGP `8.5.2` |
| Min SDK | 26 (Android 8.0) |
| Target / Compile SDK | 35 (Android 15) |
| JDK | 17 |

### Backend

| Component | Technology |
|---|---|
| Database | Supabase (PostgreSQL with Row Level Security) |
| Authentication | Supabase Auth — Google OAuth + Email OTP |
| Realtime | Supabase Realtime for incident status updates |
| Storage | Supabase Storage — private `camera-snapshots` bucket |
| Edge Functions | Supabase RPCs for pairing, cleanup, and account management |
| Scheduled Jobs | `pg_cron` for camera snapshot expiration |

### Infrastructure

| Component | Details |
|---|---|
| Hosting | Android APK distributed directly (no app store required for demo) |
| CI/CD | Not yet configured |
| Future web | Vercel may host a landing page or admin dashboard |

---

## Architecture Overview

```
Android App (Kotlin + Compose)
├── AudioMonitoringService (foreground service)
│   ├── AudioRecord (16 kHz mono PCM)
│   ├── SoundClassifier (YAMNet TFLite inference)
│   │   ├── SoundSmoother (rolling average)
│   │   ├── HysteresisSwitcher (stable category detection)
│   │   └── EmergencyGate (2-frame confirmation)
│   └── IncidentStateMachine (state transitions + escalation)
├── UI Layer (Jetpack Compose)
│   ├── BeneficiaryDashboard (monitoring, alerts, response)
│   ├── CaregiverDashboard (beneficiary list, risk summaries)
│   ├── ChatScreen / ChatListScreen (incident timeline)
│   ├── SettingsScreen (profile, permissions, preferences)
│   └── CameraTestScreen / CameraPreview (CameraX)
├── Network Layer (raw HttpURLConnection)
│   ├── AuthClient (Supabase Auth)
│   ├── ProfileClient (Supabase DB)
│   ├── CareClient (pairing, connections)
│   ├── IncidentClient (incidents, notifications)
│   ├── ChatRepository (chat message assembly)
│   ├── SnapshotClient (camera snapshots, storage)
│   ├── NotificationClient (notifications)
│   └── DeviceTokenClient (FCM tokens)
└── Supabase Backend
    ├── PostgreSQL (profiles, incidents, notifications, care_connections, camera_snapshots)
    ├── Row Level Security (access control per table)
    ├── RPCs (create_pairing_code, accept_pairing_code, reset_my_account_data, etc.)
    ├── Storage (camera-snapshots bucket, private)
    └── pg_cron (snapshot expiration)
```

---

## Sound Classification System

### Categories and Severity

The YAMNet model outputs 521 raw class scores. These are aggregated into 26 display categories:

| Category | Display Label | Severity | Emergency Alert |
|---|---|---|---|
| `speech` | Human Speech | None | No |
| `crying` | Crying Detected | **High** | **Yes** |
| `singing` | Singing | None | No |
| `human_body` | Cough / Sneeze / Snore | Medium | No |
| `crowd` | Crowd / Cheering | None | No |
| `animal` | Animal Sound | Low | No |
| `music` | Music / TV | None | No |
| `wind` | Wind | None | No |
| `thunder` | Thunder | Medium | No |
| `water` | Water Sound | Low | No |
| `fire` | Fire / Crackling | **High** | **Yes** |
| `vehicle` | Vehicle | None | No |
| `emergency_vehicle` | Emergency Vehicle Siren | **High** | **Yes** |
| `train` | Train | None | No |
| `aircraft` | Aircraft | None | No |
| `door` | Door / Doorbell / Knock | Medium | No |
| `household` | Household Sound | None | No |
| `alarm_telephone` | Alarm / Telephone | Medium | No |
| `siren_smoke` | Siren / Smoke Alarm | **High** | **Yes** |
| `mechanism` | Mechanical Sound | None | No |
| `construction` | Tool / Construction | None | No |
| `explosion_gunshot` | Explosion / Gunshot | **High** | **Yes** |
| `wood` | Wood Sound | None | No |
| `glass_break` | Glass Breaking | **High** | **Yes** |
| `liquid` | Liquid Sound | None | No |
| `object_impact` | Object Impact | None | No |
| `background` | Normal Background | None | No |

### Thresholds

| Category | Display Threshold | Emergency Threshold |
|---|---|---|
| `crying` | 0.30 | 0.40 |
| `glass_break` | 0.30 | 0.40 |
| `fire` | 0.50 | 0.50 |
| `explosion_gunshot` | 0.50 | 0.60 |
| `siren_smoke` | 0.50 | 0.65 |
| `emergency_vehicle` | 0.50 | 0.70 |
| `speech` | 0.25 | — |
| `crying` (display) | 0.30 | — |
| `human_body` | 0.30 | — |
| `door` | 0.20 | — |
| `animal` | 0.35 | — |
| `vehicle` | 0.35 | — |
| `water` | 0.30 | — |

### Signal Processing Pipeline

1. **Audio capture** — `AudioRecord` captures mono 16 kHz PCM audio via the foreground service.
2. **Sliding window** — A ring buffer feeds 15,600-sample windows (~0.975s) to the model every ~0.5s.
3. **TFLite inference** — YAMNet produces 521 class scores per window.
4. **Category aggregation** — Scores are mapped to 26 categories (top-2 summed for emergency, top-3 for others).
5. **Smoothing** — `SoundSmoother` applies a rolling average across 3–5 frames to reduce jitter.
6. **Hysteresis** — `HysteresisSwitcher` requires an enter threshold to switch categories and a 60% exit threshold to leave, preventing rapid flipping.
7. **Emergency gate** — `EmergencyGate` requires 2+ consecutive qualifying frames above the emergency threshold before triggering an alert.
8. **TV/music suppression** — When music/TV likelihood exceeds 45%, emergency thresholds are raised by +20% (capped at 85%) to reduce false alarms from television audio.
9. **Recognition priority** — Safety-relevant categories (`crying`, `human_body`, `door`, `siren_smoke`, `emergency_vehicle`, `glass_break`, `explosion_gunshot`, `fire`, `object_impact`) take priority over louder background categories when their scores exceed 70% of the display threshold.

### Fallback

When the TFLite model is unavailable, a heuristic fallback classifies sounds by RMS amplitude:
- RMS > 0.30 → "Loud Sound" (Low severity)
- RMS > 0.03 → "Speech / Active Sound" (None)
- RMS > 0.005 → "Low Level Sound" (None)
- RMS ≤ 0.005 → "Normal Background" (None)

---

## Incident State Machine

### States

```
Detected → WaitingUser → CaregiverNotified → CaregiverAcknowledged
                                                    ↓
                                              Resolved / FalseAlarm
                                                    ↓
                                              Escalated (all caregivers exhausted)
```

### Severity Behavior

| Severity | Flow |
|---|---|
| **High** | Creates incident with `WaitingUser` status. 2-minute beneficiary response window. If no response → escalates to primary caregiver (2-min ack window) → sequential escalation to each backup caregiver → `Escalated`. |
| **Medium** | Creates incident with `Detected` status. Added to timeline history. No response flow, no caregiver notification escalation. |
| **Low / None** | No incident created. Optionally shown as contextual monitoring messages. |

### Beneficiary Response Options

- **"I'm OK"** → Incident resolved as `FalseAlarm`
- **"Need Help"** → Incident transitions to `CaregiverNotified`, escalation begins

### Caregiver Acknowledgment

- If no response within 2 minutes → escalates to next caregiver by `escalation_order`
- After all caregivers are tried → `Escalated` with no further deadline

---

## Database Schema

### Tables

| Table | Purpose |
|---|---|
| `profiles` | User profiles — id, email, full_name, phone, role, setup_completed_at. Auto-created on auth signup via trigger. |
| `beneficiary_settings` | Consent and preference flags — monitoring consent, camera share, auto-approve. |
| `caregiver_settings` | Notification preferences. |
| `care_connections` | Many-to-many beneficiary↔caregiver links — status, is_primary, escalation_order. |
| `care_invitations` | Temporary 6-character pairing codes with 24-hour expiry. |
| `devices` | Registered devices per beneficiary. |
| `incidents` | Detected sound events — severity, confidence, status, timestamps, sound label. |
| `notifications` | Queued caregiver notifications per incident — status lifecycle, channel, timestamps. |
| `camera_snapshots` | Photo verification requests — approval status, expiry, storage path, incident link. |
| `device_push_tokens` | FCM tokens per user for push notification delivery. |

### Key RPCs

| Function | Purpose |
|---|---|
| `create_pairing_code()` | Generate a 6-character alphanumeric invite code valid for 24 hours. |
| `accept_pairing_code(p_code)` | Redeem a code and create (or reactivate) a care connection. Prevents self-pairing. |
| `link_demo_james()` | Demo shortcut — links to a fixed test beneficiary profile. |
| `reset_my_account_data()` | Delete all user data and reset profile state. |
| `caregiver_clear_incidents_for_beneficiary(p_beneficiary_id)` | Delete all incidents, snapshots, and notifications for a specific beneficiary. |
| `expire_camera_snapshots()` | Cron job — delete expired storage objects and mark rows as expired. |

### Row Level Security

All tables have RLS policies ensuring:
- Users can only see their own profile.
- Incidents are visible to the beneficiary and connected caregivers.
- Notifications are visible only to the assigned caregiver.
- Camera snapshots are visible to the requester, the beneficiary, and connected caregivers.
- Care connections are manageable by both parties.
- Storage uploads are restricted to the beneficiary or connected caregivers.

### Trigger-Based Automation

- **Profile auto-creation** — A trigger on `auth.users` inserts a `profiles` row on signup.
- **Incident notification enqueue** — A trigger on `incidents` automatically creates `notifications` rows when incidents transition to `waiting_user`, `caregiver_notified`, or `escalated`.
- **Camera request auto-approval** — A trigger applies the beneficiary's auto-approve preference when a new snapshot request is inserted.
- **Care link removal cleanup** — A trigger on `care_connections` DELETE/UPDATE purges associated notifications, incidents, and camera snapshots for the removed pair.
- **Field protection triggers** — Prevent mutation of immutable fields on incidents and snapshots.

---

## Setup Instructions

### Prerequisites

- Android Studio (latest stable)
- JDK 17
- Android SDK with API 35
- A Supabase project
- A Firebase project

### Android

1. Clone the repository.
2. Register the Android app in Firebase using package name `com.yuletan.soundguard`.
3. Place the downloaded `google-services.json` in `app/google-services.json`.
4. Keep local Firebase and Supabase configuration out of version control.
5. Open the project in Android Studio and run on an emulator or physical device.

The emulator is suitable for UI, authentication, database, and FCM development. Microphone behavior, foreground-service reliability, camera behavior, battery sleep handling, and Wake Lock behavior must be tested on real hardware.

### Supabase

1. Create a Supabase project.
2. Enable Google OAuth in the Supabase dashboard and add `soundguard://auth/callback` to the allowed redirect URLs.
3. Apply the migrations in `supabase/migrations/` in order.
4. Create the `camera-snapshots` storage bucket (private, not public).
5. Enable `pg_cron` and schedule `expire_camera_snapshots()` to run every minute.
6. Never expose the Supabase service-role key in the Android app.

### Environment Variables

Copy `.env.example` to `.env` and fill in:
- `SUPABASE_URL` — Your Supabase project URL
- `SUPABASE_ANON_KEY` — Your Supabase anonymous/public key
- `FIREBASE_PROJECT_ID` — Your Firebase project ID

---

## Repository Structure

```
.
├── app/
│   └── src/main/
│       ├── java/com/yuletan/soundguard/
│       │   ├── MainActivity.kt          # All screens, navigation, state
│       │   ├── AudioMonitoringService.kt # Foreground service, audio capture
│       │   ├── SoundClassifier.kt        # YAMNet inference, thresholds, smoothing
│       │   ├── IncidentStateMachine.kt   # State transitions, escalation
│       │   ├── UiComponents.kt           # Reusable Compose components, risk tiers
│       │   ├── CameraPreview.kt          # CameraX preview and capture
│       │   ├── ChatScreen.kt             # Incident timeline chat
│       │   ├── ChatListScreen.kt         # Conversation list
│       │   ├── ChatMessage.kt            # Message data models
│       │   ├── ChatRepository.kt         # Chat assembly from DB records
│       │   ├── AuthClient.kt             # Supabase Auth (Google OAuth, OTP)
│       │   ├── ProfileClient.kt          # Supabase profile CRUD
│       │   ├── CareClient.kt             # Pairing, connections, RPCs
│       │   ├── IncidentClient.kt         # Incidents, notifications
│       │   ├── SnapshotClient.kt         # Camera snapshots, storage upload
│       │   ├── NotificationClient.kt     # Notification management
│       │   ├── DeviceTokenClient.kt      # FCM token registration
│       │   ├── PushMessagingService.kt   # FCM message handling
│       │   └── SoundGuardTheme.kt        # Color tokens, theme
│       ├── assets/
│       │   └── yamnet.tflite             # YAMNet TFLite model
│       └── res/
│           └── drawable/                  # Icons and assets
├── supabase/
│   ├── migrations/                        # 34 SQL migration files
│   └── functions/                         # Edge functions (if any)
├── model/
│   └── README.md                          # Model notes
├── docs/
│   ├── privacy.md                         # Privacy documentation
│   └── demo.md                            # Demo checklist
├── soundguard-redesign-mockups.html       # Interactive HTML mockups
├── soundguard-ux-audit.md                 # UI/UX audit
└── README.md
```

---

## Android Permissions

| Permission | Purpose |
|---|---|
| `INTERNET` | Supabase database and storage connectivity |
| `RECORD_AUDIO` | YAMNet sound classification via AudioRecord |
| `CAMERA` | Verification photo capture via CameraX |
| `POST_NOTIFICATIONS` | FCM push notifications and emergency alerts |
| `WAKE_LOCK` | Keep classification running when screen is off (12-hour partial wake lock) |
| `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_MICROPHONE` | Audio monitoring foreground service |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Prevent Doze from killing the monitoring service |

---

## Privacy and Safety Design

- **Audio is processed locally** — Raw audio is never stored or transmitted. Classification happens entirely on-device using the YAMNet TFLite model.
- **Consent-first monitoring** — Audio monitoring requires explicit beneficiary consent collected during setup and can be revoked at any time.
- **Camera consent is separate** — Camera permissions are requested and configured independently. The beneficiary controls whether photo requests require manual approval.
- **Time-limited snapshots** — Camera photos auto-delete after 30 minutes via a cron job. They are stored in a private, access-controlled Supabase Storage bucket.
- **RLS enforcement** — Every database table has Row Level Security policies ensuring users can only access data they are authorized to see.
- **No emergency services calling** — SoundGuard does not call 911 or any emergency number. It notifies designated caregivers only.
- **Connectivity transparency** — The app displays when monitoring and caregiver notifications are unavailable due to connectivity loss.
- **Full account data reset** — Users can delete all their data from the server via the `reset_my_account_data()` RPC.

---

## Demo Features

The current debug build includes several features designed for live demonstration:

- **Same-device Caregiver Admin Preview** — Simulates the beneficiary-to-caregiver flow without requiring a second phone.
- **Sound simulator** — Buttons on the beneficiary dashboard trigger simulated events (glass break, alarm, cough, crying, siren, explosion, etc.) through `AudioMonitoringService.simulateSound()`.
- **Demo caregiver "James"** — A fixed test profile linked via the `link_demo_james()` RPC for instant pairing.
- **WhatsApp-style chat preview** — Incident timeline with timestamped alerts, photo preview, call action, and camera request approval/decline.
- **In-memory alert history** — Different events appear as separate caregiver messages with timestamps and severity labels.

### Demo-Only Limitations

- The Admin Preview and demo caregiver link are local to the current app process; they do not create a real Supabase caregiver connection.
- Demo verification photos are captured and displayed locally on the same device.
- Alert history is currently in memory and is cleared when the app process is terminated.

### Suggested Demo Flow

1. Open the beneficiary dashboard and link the Demo Caregiver.
2. Open the Caregiver Preview and trigger different simulator events from the beneficiary dashboard.
3. Return to the caregiver chat to see separate timestamped alert messages.
4. Request a verification photo, approve it, capture the image, and finish the camera screen.
5. Review the captured photo and use the call recommendation for follow-up.

---

## Planned Implementation Phases

1. Android foundation and shared domain models.
2. Supabase schema, Row Level Security, and client configuration.
3. Google OAuth authentication and role-based onboarding.
4. Beneficiary-caregiver invitations and ordering.
5. Audio foreground service and YAMNet inference.
6. Incident state machine and escalation.
7. Caregiver timelines and FCM notifications.
8. Camera permissions, caregiver-triggered snapshots, and expiry.
9. Accessibility, testing, demo reliability, and deployment.

---

## Important Limitations

- Android is the first supported platform; iOS is future work.
- Monitoring requires an active internet connection for caregiver alerts and snapshots.
- Foreground audio is more reliable than browser audio but still depends on Android battery and permission settings.
- Remote camera capture cannot be guaranteed while the device is locked or suspended.
- Model predictions may produce false alarms or miss unfamiliar sounds.
- The YAMNet baseline model covers general environmental sounds; a custom fine-tuned model may improve accuracy for specific use cases.

---

## License

To be defined.
