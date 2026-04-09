<p align="center">
  <img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher_round.png" width="120" alt="MPT Logo"/>
</p>

<h1 align="center">MPT — Master Password Trainer</h1>

<p align="center">
  <strong>A gym for your password memory. No cloud. No network. Just you and your recall.</strong>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white" alt="Android"/>
  <img src="https://img.shields.io/badge/Kotlin-2.0-7F52FF?logo=kotlin&logoColor=white" alt="Kotlin"/>
  <img src="https://img.shields.io/badge/Jetpack_Compose-Material_3-4285F4?logo=jetpackcompose&logoColor=white" alt="Compose"/>
  <img src="https://img.shields.io/badge/Network-ZERO-red" alt="Zero Network"/>
  <img src="https://img.shields.io/badge/Min_SDK-26_(Android_8)-green" alt="Min SDK 26"/>
</p>

---

## The Problem

You created a strong master password for your password manager. Then you enabled fingerprint unlock and never typed it again.

Weeks pass. Months pass. Then one day — new phone, factory reset, biometric failure — and you're locked out of *everything*.

## The Solution

**MPT periodically reminds you to type your master password and email from memory**, verifying correctness against a securely stored hash. Think of it as spaced repetition for your most important credentials.

Get it right? Your streak grows. Forget? You'll know *before* it matters.

---

## Features

### Core
- **Password & Email Challenges** — Type your credentials from memory; MPT verifies them against secure hashes without ever storing the actual password
- **Smart Reminders** — Configurable intervals (3 / 5 / 7 / 14 / 30 days) with notification nudges when you're overdue
- **Streak Tracking** — Build and maintain streaks to stay sharp; streaks reset after consecutive failures
- **Multiple Entries** — Track passwords for Bitwarden, 1Password, LastPass, KeePass, or any custom service
- **Beautiful Dashboard** — Color-coded cards with progress rings, status indicators, and masked email previews
- **Guided Onboarding** — Animated walkthrough explains the concept and gets you started in 30 seconds
- **App Lock** — Optional biometric/PIN gate to protect the app itself
- **Dark & Light Themes** — Full Material You / Material 3 dynamic theming

### Advanced
- **Streak Calendar & History** — GitHub-style contribution calendar showing your verification history per entry, with detailed stats (success rate, best streak, total checks)
- **Adaptive Difficulty** — As your streak grows, challenges get harder: reversed field order at streak 5+, delayed recall at streak 10+ — ensuring real memory, not muscle memory
- **Home Screen Widget** — Glanceable widget showing overdue status; tap to jump straight into a challenge
- **Encrypted Backup & Restore** — Export all data as an AES-256-GCM encrypted `.mptbackup` file; import on another device with zero cloud dependency
- **Panic Wipe** — Optional dead-man's switch: after X consecutive global failures, all data is silently wiped — the app looks freshly installed
- **Multiple Password Versions** — Store up to 3 password versions per entry (Current / Previous / Legacy) for smooth password rotation periods
- **Custom Notifications** — Write your own reminder messages per entry for a personal nudge
- **Global Statistics** — Aggregate dashboard with weekly/monthly charts, per-entry comparisons, and fun stats

---

## Security & Privacy

MPT is built with a security-first architecture. Your credentials never leave your device.

| | |
|---|---|
| **Zero Network** | No internet permission declared. Not even requested. `usesCleartextTraffic="false"` enforced. |
| **Password Hashing** | Argon2id (winner of the Password Hashing Competition) — memory-hard, GPU/ASIC resistant. Your password is **never stored**, only a one-way hash. |
| **Email Encryption** | AES-256-GCM with keys stored in Android Keystore (hardware-backed on most modern devices). |
| **Encrypted Storage** | All data sits in EncryptedSharedPreferences, encrypted at rest via AndroidX Security. |
| **Screenshot Protection** | `FLAG_SECURE` on all screens — no screenshots, no screen recording, no recent apps preview. |
| **No Clipboard** | Password fields disable copy/paste entirely. |
| **Memory Safety** | Passwords use `CharArray` (not `String`) and are zeroed immediately after hashing. |
| **No Logging** | Zero sensitive data in logs. Full ProGuard/R8 obfuscation in release builds. |

### Argon2id Parameters
```
Time cost:     3 iterations
Memory cost:   65536 KB (64 MB)
Parallelism:   4 threads
Salt:          32 bytes (cryptographically random, unique per entry)
Hash output:   32 bytes
```

---

## How It Works

```
1. Add an entry       →  Pick your service, type email + password, set reminder interval
2. MPT hashes & stores →  Password is Argon2id hashed, email is AES-256 encrypted
3. Get reminded        →  Notification arrives when it's time to practice
4. Type from memory    →  Enter your email and password without any hints
5. Verify & grow       →  Correct? Streak grows. Wrong? Try again — hints after 3 failures
```

Your actual password is **never visible** anywhere in the app — not in storage, not in UI, not in logs. MPT only knows if what you typed *matches* the hash.

---

## Screenshots

> *Coming soon — screenshots of the dashboard, challenge screen, streak calendar, and onboarding flow.*

---

## Tech Stack

| Component | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 (Material You) |
| Navigation | Compose Navigation |
| Password Hashing | Argon2id via [argon2kt](https://github.com/nicholasio/argon2kt) |
| Encryption | AES-256-GCM via Android Keystore |
| Storage | EncryptedSharedPreferences (AndroidX Security) |
| Background Work | WorkManager |
| Biometrics | AndroidX Biometric |
| Serialization | Kotlinx Serialization |
| Min SDK | 26 (Android 8.0) |
| Target SDK | 34 (Android 14) |

---

## Building from Source

### Prerequisites
- Android Studio Ladybug (2024.x) or newer
- JDK 17 (bundled with Android Studio)
- Android SDK 34+

### Build

```bash
git clone https://github.com/your-username/MPT.git
cd MPT
./gradlew assembleDebug
```

Install the debug APK on your device or emulator:

```bash
./gradlew installDebug
```

---

## Project Structure

```
app/src/main/java/com/mpt/masterpasswordtrainer/
├── data/
│   ├── model/          # Data classes (PasswordEntry, BackupData, etc.)
│   ├── repository/     # EncryptedSharedPreferences data layer
│   ├── security/       # HashUtil, CryptoUtil, KeystoreManager
│   └── backup/         # Encrypted backup/restore logic
├── ui/
│   ├── theme/          # Material 3 theming (colors, typography)
│   ├── navigation/     # Compose NavGraph + deep links
│   ├── screens/        # All app screens (dashboard, challenge, settings, etc.)
│   └── components/     # Reusable UI components (cards, animations, pickers)
├── widget/             # Home screen widget
├── worker/             # WorkManager reminder scheduling
└── util/               # Constants, date utilities
```

---

## Contributing

Contributions are welcome! Please open an issue first to discuss what you'd like to change.

## License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.

---

<p align="center">
  <strong>Your password. Your device. Your memory.</strong><br/>
  <sub>MPT never phones home. Never will.</sub>
</p>
