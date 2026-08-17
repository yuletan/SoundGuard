# SoundGuard

SoundGuard is an Android-first safety support app for people who may spend time alone. It listens locally for important environmental sounds and helps a caregiver respond when the beneficiary may need assistance.

> SoundGuard is a hackathon prototype. It is not a medical device, certified emergency system, or replacement for emergency services.

## MVP

- Native Android app built with Kotlin and Jetpack Compose.
- Beneficiary and caregiver roles in the same app.
- Google OAuth authentication through Supabase.
- Many-to-many caregiver connections using one-time pairing codes or QR codes: one beneficiary requires at least one caregiver; caregivers can support multiple beneficiaries.
- Primary caregiver followed by ordered backup caregivers.
- Local YAMNet/TensorFlow Lite sound classification, initially targeting `background`, `alarm`, and `glass_break`.
- Active monitoring through an Android foreground service.
- Low-severity timeline alerts without a response flow.
- High-severity alerts with a two-minute beneficiary response window.
- Sequential caregiver escalation with two-minute acknowledgement windows.
- Firebase Cloud Messaging notifications.
- Caregiver alert timelines grouped by beneficiary.
- Caregiver-triggered front/rear camera verification after prior beneficiary consent.
- Private Supabase Storage snapshots deleted after 10 minutes.
- Call actions using phone numbers collected during setup and editable in Settings.
- No automatic emergency-services calling.

## Privacy and safety

- Raw audio is processed locally and is not stored by default.
- Monitoring requires explicit beneficiary consent and a visible active state.
- Camera consent is collected during setup and can be revoked in Settings.
- A caregiver must explicitly request a verification photo.
- Remote camera capture is best-effort: Android may block it when the device is locked, offline, or suspended.
- If a photo cannot be captured, the caregiver is prompted to call the beneficiary.
- Camera snapshots use private access-controlled storage and expire after 10 minutes.
- The app must show that monitoring and caregiver notifications are unavailable when connectivity is lost.

## Architecture

```text
Kotlin Android app
  - Jetpack Compose UI
  - Supabase Auth / database / realtime / storage
  - Android foreground audio service
  - YAMNet TensorFlow Lite inference
  - CameraX verification capture
  - Firebase Cloud Messaging
  - Android phone-call intents

Supabase
  - PostgreSQL and Row Level Security
  - Realtime incident updates
  - Private snapshot storage
  - Edge Functions or server routes for protected operations
```

Railway is not required for the MVP. Vercel may host a future web landing page or administrative tools; the primary client is the Android application.

## Repository structure

```text
.
├── app/
│   └── src/main/
│       ├── java/com/yuletan/soundguard/
│       └── res/
├── supabase/
│   ├── migrations/
│   └── functions/
├── model/
│   └── README.md
├── docs/
│   ├── privacy.md
│   └── demo.md
├── .env.example
└── README.md
```

## Local setup

### Android

1. Install Android Studio, JDK 17, and an Android SDK with API 35.
2. Register the Android app in Firebase using package name `com.yuletan.soundguard`.
3. Place the downloaded `google-services.json` in `app/google-services.json`.
4. Keep local Firebase and Supabase configuration out of Git.
5. Open the project in Android Studio and run it on an Android emulator or physical device.

The emulator is suitable for UI, authentication, database, FCM, and basic permission development. Microphone behavior, foreground-service reliability, camera behavior, battery sleep handling, and Wake Lock behavior must eventually be tested on real hardware.

### Supabase

Create a Supabase project, enable Google OAuth, and add `soundguard://auth/callback` to the allowed redirect URLs. Apply the migrations in `supabase/migrations/` once they exist. Never expose the Supabase service-role key in the Android app.

### Model

The first model phase uses the YAMNet TensorFlow Lite baseline. Test it with recordings from the actual demo phone before adding a custom classifier using YAMNet embeddings.

## Planned implementation phases

1. Android foundation and shared domain models.
2. Supabase schema, Row Level Security, and client configuration.
3. Google OAuth authentication and role-based onboarding.
4. Beneficiary-caregiver invitations and ordering.
5. Audio foreground service and YAMNet inference.
6. Incident state machine and escalation.
7. Caregiver timelines and FCM notifications.
8. Camera permissions, caregiver-triggered snapshots, and expiry.
9. Accessibility, testing, demo reliability, and deployment.

## Worktree strategy

Create feature worktrees only after the foundation and backend contracts are merged. These can then proceed in parallel:

- `feature/auth-onboarding`
- `feature/care-connections`
- `feature/audio-monitoring`
- `feature/incident-engine`
- `feature/caregiver-ui`
- `feature/fcm-notifications`
- `feature/camera-verification`
- `feature/testing-documentation`

Merge shared models and database contracts first. Audio monitoring and caregiver UI can work concurrently once the incident contract is stable. Camera and FCM depend on the incident and connection permissions.

## Important limitations

- Android is the first supported platform; iOS is future work.
- Monitoring requires an active internet connection for caregiver alerts and snapshots.
- Foreground audio is more reliable than browser audio but still depends on Android battery and permission settings.
- Remote camera capture cannot be guaranteed while the device is locked or suspended.
- Model predictions may produce false alarms or miss unfamiliar sounds.

## License

To be defined.

## Current hackathon demo additions

The current Android debug demo also includes:

- More sensitive YAMNet target prioritization so recognized sounds can take priority over louder background noise.
- Alert thresholds tuned for distant or quieter glass-break events, including a `0.45` glass-break alert threshold.
- In-memory alert history so different events such as glass break, alarm, doorbell, cough, crying, water, animal sounds, and thunder can appear as separate caregiver messages.
- Low-severity monitoring messages for potentially useful context such as cough/sneeze/snore, water, animal sounds, thunder, crying, and door events.
- A same-device Caregiver Admin Preview for demonstrating beneficiary-to-caregiver behavior without a second phone.
- A WhatsApp-style beneficiary chat preview with message timestamps, alert acknowledgement, call action, camera-request approval/decline, and photo preview.

### Demo-only limitations

- The Admin Preview and demo caregiver link are local to the current app process; they do not create a real Supabase caregiver connection.
- Demo verification photos are captured and displayed locally on the same device.
- The real remote-camera flow still requires a backend request, beneficiary-device handling, private upload, and caregiver download/preview through Supabase.
- Alert history is currently in memory and is cleared when the app process is terminated. Persistent incident history requires wiring the audio service to the `incidents` and `notifications` tables.

### Suggested demo flow

1. Open the beneficiary dashboard and link the Demo Caregiver.
2. Open Caregiver Preview and trigger different simulator events from the beneficiary dashboard.
3. Return to the caregiver chat to see separate timestamped alert messages.
4. Request a verification photo, approve it, capture the image, and finish the camera screen.
5. Review the captured photo and use the call recommendation for follow-up.
