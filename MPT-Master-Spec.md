# MPT — Master Password Trainer
## Complete Project Specification & Claude Code Agent Prompts

---

## 1. PROJECT OVERVIEW

**App Name:** MPT — Master Password Trainer  
**Platform:** Android (native)  
**Package:** `com.mpt.masterpasswordtrainer`  
**Min SDK:** 26 (Android 8.0) | **Target SDK:** 34 (Android 14)  
**Purpose:** A zero-network, local-only app that helps users retain their master passwords by periodically challenging them to type both email and password, verifying correctness against securely stored hashes.

**The Problem:** Users set a strong master password for their password manager, then exclusively use biometrics/PIN to unlock it. Over weeks and months, the actual password atrophies from memory. When they need it (new device, reinstall, biometric failure), they're locked out of everything.

**The Solution:** MPT periodically reminds users to type their master password and email from memory, verifying correctness against a securely stored hash. It's a gym for password memory — not another vault.

---

## 2. SECURITY ARCHITECTURE (Critical — Read First)

### 2.1 Data Classification

| Field | Storage Method | Reversible? | Visible in UI? |
|---|---|---|---|
| Service name (e.g. "Bitwarden") | Plaintext in EncryptedSharedPreferences | Yes | Always visible |
| Service icon/color | Plaintext | Yes | Always visible |
| Email | AES-256-GCM encrypted via Android Keystore | Yes (decrypted only during challenge comparison) | Partially masked on dashboard (j•••@gmail.com), full during challenge verification |
| Password | Argon2id hash + random salt | NO — one-way | Never shown anywhere |
| Reminder interval | Plaintext in EncryptedSharedPreferences | Yes | Visible in settings |
| Last verified date | Plaintext in EncryptedSharedPreferences | Yes | Visible on dashboard |

### 2.2 Why This Model?

- **Password is HASHED (Argon2id):** Even if someone roots the device and extracts the data file, they get a hash that is computationally infeasible to reverse. Argon2id is the winner of the Password Hashing Competition and is memory-hard (resistant to GPU/ASIC attacks).
- **Email is ENCRYPTED (not hashed):** Because we need to verify it AND optionally show a masked version. We use AES-256-GCM with a key stored in Android Keystore (hardware-backed on most devices since 2018+). The key never leaves the secure hardware.
- **EncryptedSharedPreferences:** The entire preferences file is encrypted at rest using the AndroidX Security library, which wraps Android Keystore.

### 2.3 Argon2id Parameters

```
Time cost:     3 iterations
Memory cost:   65536 KB (64 MB)
Parallelism:   4 threads  
Salt:          32 bytes, cryptographically random (unique per entry)
Hash output:   32 bytes
```

These parameters take ~300-500ms on a modern phone. Fast enough for UX, slow enough to make brute-force infeasible.

### 2.4 Android Keystore Integration

- Master encryption key is generated inside Android Keystore on first app launch
- Key alias: `mpt_master_key`
- Key spec: AES-256-GCM, requires user authentication (biometric/PIN) if app lock is enabled
- On devices with hardware security module (StrongBox), prefer StrongBox

### 2.5 Zero-Network Guarantee

```xml
<!-- AndroidManifest.xml -->
<!-- NO internet permission declared AT ALL -->
<!-- Additionally, explicit declaration: -->
<application
    android:usesCleartextTraffic="false"
    android:networkSecurityConfig="@xml/network_security_config">
```

```xml
<!-- network_security_config.xml -->
<network-security-config>
    <domain-config cleartextTrafficPermitted="false">
        <domain includeSubdomains="true">*</domain>
    </domain-config>
</network-security-config>
```

### 2.6 Additional Security Measures

- **FLAG_SECURE** on all activities (prevents screenshots, screen recording, recent apps preview)
- **No clipboard access** — password input fields disable paste/copy
- **Memory wiping** — password strings are zeroed after hashing (use CharArray, not String)
- **Auto-lock** — app locks when backgrounded for >30 seconds
- **No logging** — zero `Log.d()` calls with sensitive data in release builds
- **ProGuard/R8** — full obfuscation enabled for release builds
- **No backup** — `android:allowBackup="false"` and `android:fullBackupContent="false"`

---

## 3. DATA MODEL

```kotlin
data class PasswordEntry(
    val id: String,              // UUID
    val serviceName: String,     // "Bitwarden", "1Password", etc.
    val serviceColor: Long,      // Material color for the card
    val serviceIcon: String,     // Icon identifier (shield, key, lock, etc.)
    val encryptedEmail: String,  // AES-256-GCM encrypted email (Base64)
    val emailIV: String,         // Initialization vector for email decryption (Base64)
    val passwordHash: String,    // Argon2id hash (Base64)
    val passwordSalt: String,    // Salt used for hashing (Base64)
    val reminderDays: Int,       // Check interval: 3, 7, 14, 30
    val lastVerified: Long,      // Timestamp of last successful verification
    val createdAt: Long,         // Entry creation timestamp
    val streak: Int,             // Consecutive successful verifications
    val totalAttempts: Int,      // Total verification attempts
    val successfulAttempts: Int  // Successful verifications
)
```

---

## 4. SCREEN SPECIFICATIONS

### 4.1 Onboarding Wizard (First Launch Only)

**Shown when:** `onboarding_completed` flag is false (first launch or data cleared)  
**Navigation:** App start → check flag → Onboarding OR Dashboard

**4 swipeable pages using HorizontalPager:**

**Page 1 — "The Problem":**
- Visual: Animated password text that progressively replaces characters with "?" marks, then reforms, looping
- Headline: "When did you last type your master password?"
- Body: "You set it once. Now you use fingerprint or a PIN. But what happens when you get a new phone, reinstall your vault, or biometrics fail?"

**Page 2 — "The Solution":**
- Visual: Animated cycle of icons in circular arrangement — calendar → bell → keyboard → checkmark — with connecting arrows drawing between them
- Headline: "Train your memory like a muscle"
- Body: "MPT reminds you every few days to type your master password and email from memory. Get it right? Your streak grows. Forget? You'll know before it matters."

**Page 3 — "The Trust":**
- Visual: Central shield with cloud/wifi/server icons floating toward it and bouncing away with red ❌ flash
- Headline: "Your password never leaves this device"
- Three trust points stacked vertically with staggered entrance:
  - 🔐 "Your password is hashed — even this app can't see it"
  - 📵 "Zero internet access — no network permissions at all"
  - 🛡️ "Encrypted storage backed by your phone's security chip"

**Page 4 — "Let's Start":**
- Visual: MPT shield logo with gentle pulse/glow animation
- Headline: "Ready to train?"
- Body: "Add your first master password and set up your practice schedule. It takes 30 seconds."
- CTA: Large primary button "Add My First Password →"

**UX Details:**
- Support both swipe AND "Next" button to advance
- "Skip" text button in top-right (small, understated — goes to page 4)
- Progress dots: 4 dots, active = filled 8dp accent color, inactive = outlined 6dp muted
- Each page's elements stagger in: icon first (scale up), then headline (fade + slide up), then body (100ms later)
- "Add My First Password" → navigates to AddEntryScreen with `isFromOnboarding=true`
- On first entry save with that flag: set `onboarding_completed=true`, navigate to Dashboard (clear backstack)
- User never sees empty dashboard on first experience

**Edge Cases:**
- App killed mid-onboarding → shows onboarding again next launch (flag not set)
- Skip → lands on page 4, can tap CTA
- Back out of AddEntry without saving → returns to page 4
- App data cleared → onboarding shows again (correct, entries also wiped)

### 4.2 Home / Dashboard Screen

**What the user sees:**
- App bar with "MPT" title and settings gear icon
- Cards for each password entry showing:
  - Left: Circular icon with the entry's chosen color as background
  - Center: Service name (large, bold), masked email below (j•••n@gm•••.com), status badge, streak
  - Right: ProgressRing (circular arc, days elapsed vs interval)
  - Card tap → navigate to ChallengeScreen(entryId)
  - Swipe left → delete (red), swipe right → edit (blue)
- FAB to add new entry
- Summary bar: "X entries" • "Y overdue" (red if >0)
- Empty state: shield icon + "No passwords to train" + CTA button

**Email masking logic:**
- Show first char + "•••" + last char before @ + "@" + first 2 chars of domain + "•••" + TLD
- Example: "johnsmith@gmail.com" → "j•••h@gm•••.com"

**Status logic:**
- Green: `daysSinceLastVerified < reminderDays - 1`
- Amber: `daysSinceLastVerified == reminderDays - 1` (due tomorrow)
- Red: `daysSinceLastVerified >= reminderDays` (overdue)

**Animations:**
- Cards stagger in from bottom (50ms delay each)
- ProgressRing animates from 0 to current value
- Pull-to-refresh gesture
- Card press: subtle scale-down feedback
- Status badge pulses gently when overdue
- Delete confirmation dialog: "Delete [ServiceName]? This cannot be undone."

### 4.3 Add/Edit Entry Screen

**Flow:**
1. Service name input with autocomplete suggestions: Bitwarden, 1Password, LastPass, KeePass, Dashlane, Proton Pass, NordPass, custom
2. Row of selectable icons (8): Shield, Key, Lock, Fingerprint, Safe, Cloud, Globe, Star
3. Row of selectable accent colors (8): Red, Blue, Green, Purple, Orange, Teal, Pink, Indigo (colored circles)
4. Email text field (keyboard type: email)
5. Password text field (obscured, visibility toggle, paste DISABLED)
6. Confirm password field (must match)
7. Reminder interval: segmented button — 3d / 5d / 7d / 14d / 30d
8. Helper text: "You'll be reminded to verify every X days"
9. Full-width "Save" button (loading spinner during hash ~400ms, disabled until valid)

**On save:**
1. Generate 32-byte random salt
2. Hash password with Argon2id + salt → store hash + salt
3. Encrypt email with AES-256-GCM via Keystore → store ciphertext + IV
4. Wipe password CharArray from memory
5. Set `lastVerified` to now
6. Navigate to dashboard (or dashboard via onboarding flow)

**Validation:** Service name required (1-50 chars), email required (basic format), password required (min 1 char), confirm must match, reminder must be selected.

**Animations:** Fields slide in staggered, icon/color selection scales on tap, error messages animate in.

### 4.4 Challenge Screen (The Core Feature)

**Triggered by:** Notification tap or manual card tap

**UI:**
1. Service icon (large) + accent color circle + service name headline
2. Subtitle: "Type your credentials from memory"
3. Email text field (empty, placeholder "Email address")
4. Password text field (obscured, placeholder "Master password", no paste)
5. Full-width "Verify" button (loading during hash)
6. Bottom info: "Last verified: 8 days ago" + "Attempts today: 3"

**Verification logic:**
1. Decrypt stored email → compare with input (case-insensitive, trimmed)
2. Hash input password with stored salt → compare with stored hash
3. Results:

**Both correct:**
- Background flashes soft green
- Animated checkmark draws itself
- "Perfect! ✓" headline + streak with scale-up animation
- Subtle confetti particles
- Auto-return to dashboard after 2.5s

**Email wrong, password right:**
- Amber state, email field shakes + amber outline
- "Password is correct, but the email doesn't match."
- Email field clears and focuses

**Password wrong (regardless of email):**
- Red state, both fields shake
- "That's not right. Take a breath and try again."
- Both fields clear
- After 3 failures: offer "Tap for email hint" → shows masked email
- After 5 failures: lock for 60 seconds with countdown

**Both wrong:**
- Red state: "Neither matched. Think carefully and try again."
- Same failure counting as above

**Shake animation:** 0 → -10dp → 10dp → -6dp → 6dp → 0, 400ms, per field

**Lock state:** Button grayed, countdown timer "Locked for 45s", circular countdown animation, auto-unlocks

**Streak logic:** Incremented on success. Reset to 0 after 3 consecutive failures (not first fail — allow typos).

### 4.5 Settings Screen

1. **App Lock:** Toggle biometric/PIN requirement (BiometricPrompt on launch + resume after 30s)
2. **Default reminder interval** for new entries (segmented button)
3. **Notifications:** Toggle enable/disable, quiet hours (start/end time pickers)
4. **Appearance:** Theme selector — System / Light / Dark
5. **Data:** "Delete all data" — red button, warning dialog, must type "DELETE" to confirm
6. **About:** Version + security trust statement: "🔒 Your passwords never leave this device. No internet access, no cloud sync, no analytics. Passwords are hashed with Argon2id and can never be recovered — not even by this app."

---

## 5. NOTIFICATION SYSTEM

Using WorkManager:
- PeriodicWorkRequest runs daily, checks all entries
- Overdue entries trigger notification:
  - Single: "Time to practice!" / "Your [ServiceName] password check is due"
  - Multiple: Summary with InboxStyle listing each service
- Deep link: `mpt://challenge/{entryId}` opens ChallengeScreen
- Notification channel: "Password Reminders", importance DEFAULT
- POST_NOTIFICATIONS permission request on Android 13+ (triggered on first entry creation)
- RECEIVE_BOOT_COMPLETED for persistence across reboots

---

## 6. TECH STACK & DEPENDENCIES

```kotlin
// build.gradle.kts (app level)
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

dependencies {
    // Core
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.1")
    
    // Compose BOM
    implementation(platform("androidx.compose:compose-bom:2024.08.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.animation:animation")
    
    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.7")
    
    // Security
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    
    // Argon2 hashing
    implementation("com.lambdapioneer.argon2kt:argon2kt:1.6.0")
    
    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.9.1")
    
    // Biometric
    implementation("androidx.biometric:biometric:1.1.0")
    
    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")
    
    // Splash screen
    implementation("androidx.core:core-splashscreen:1.0.1")
}
```

---

## 7. UI/UX DESIGN DIRECTION

- **Style:** Clean, security-focused, modern Material 3
- **Theme:** Dynamic color (Material You) with deep navy/slate fallback palette
- **Typography:** Material 3 type scale, slightly heavier headings — solid and trustworthy feel
- **Cards:** Each entry has user-chosen accent color for instant recognition
- **Animations:** Success checkmark draws itself + particles, failure fields shake, streak number scales up, progress rings use spring physics, cards stagger slide-in
- **Icons:** Shield for the app, curated set per entry (shield, key, lock, fingerprint, safe, cloud, globe, star)

---

## 8. PROJECT STRUCTURE

```
app/src/main/java/com/mpt/masterpasswordtrainer/
├── MPTApplication.kt
├── MainActivity.kt
├── data/
│   ├── model/
│   │   └── PasswordEntry.kt
│   ├── repository/
│   │   └── PasswordRepository.kt
│   └── security/
│       ├── HashUtil.kt
│       ├── CryptoUtil.kt
│       └── KeystoreManager.kt
├── ui/
│   ├── theme/
│   │   ├── Theme.kt
│   │   ├── Color.kt
│   │   └── Type.kt
│   ├── navigation/
│   │   └── NavGraph.kt
│   ├── screens/
│   │   ├── onboarding/
│   │   │   ├── OnboardingScreen.kt
│   │   │   ├── OnboardingViewModel.kt
│   │   │   └── OnboardingPage.kt
│   │   ├── dashboard/
│   │   │   ├── DashboardScreen.kt
│   │   │   └── DashboardViewModel.kt
│   │   ├── addentry/
│   │   │   ├── AddEntryScreen.kt
│   │   │   └── AddEntryViewModel.kt
│   │   ├── challenge/
│   │   │   ├── ChallengeScreen.kt
│   │   │   └── ChallengeViewModel.kt
│   │   └── settings/
│   │       ├── SettingsScreen.kt
│   │       └── SettingsViewModel.kt
│   └── components/
│       ├── EntryCard.kt
│       ├── StatusBadge.kt
│       ├── ProgressRing.kt
│       ├── SuccessAnimation.kt
│       ├── ShakeAnimation.kt
│       ├── ServiceIconPicker.kt
│       └── OnboardingAnimations.kt
├── worker/
│   └── ReminderWorker.kt
└── util/
    ├── DateUtils.kt
    └── Constants.kt
```

---

## 9. CLAUDE CODE AGENT PROMPTS (Execute in Order)

---

### PHASE 1 — Project Scaffolding, Theme & Navigation

```
You are building an Android app called "MPT — Master Password Trainer" (package: com.mpt.masterpasswordtrainer).

Create a new Android project with these specs:
- Kotlin + Jetpack Compose
- Min SDK 26, Target SDK 34
- Material 3 with dynamic color theming (Material You)
- ZERO internet permissions — this is a fully offline security app

Set up the following:

1. **build.gradle.kts** with these dependencies:
   - Compose BOM 2024.08.00
   - Material 3
   - Material Icons Extended
   - Compose Animation
   - Navigation Compose 2.7.7
   - AndroidX Security Crypto 1.1.0-alpha06
   - argon2kt 1.6.0 (com.lambdapioneer.argon2kt:argon2kt)
   - WorkManager 2.9.1
   - Biometric 1.1.0
   - Kotlinx Serialization JSON 1.7.1
   - Core Splash Screen 1.0.1
   - Kotlin serialization plugin

2. **AndroidManifest.xml:**
   - NO internet permissions
   - android:usesCleartextTraffic="false"
   - android:allowBackup="false"
   - android:fullBackupContent="false"
   - FLAG_SECURE will be set in MainActivity

3. **Theme (ui/theme/):**
   - Dynamic color (Material You) with fallback colors
   - Base palette: deep navy/slate primary, with security-feeling tones
   - Support system dark/light mode
   - Define the Material 3 type scale

4. **Navigation (ui/navigation/NavGraph.kt):**
   - Routes: Onboarding, Dashboard, AddEntry, Challenge(entryId), Settings
   - Challenge route takes an entryId string parameter
   - AddEntry route takes optional isFromOnboarding boolean parameter
   - Start destination: check onboarding_completed SharedPreference → Onboarding or Dashboard

5. **MainActivity.kt:**
   - Set FLAG_SECURE (window.setFlags)
   - Edge-to-edge display
   - Splash screen support
   - Set up NavHost

6. **Skeleton screens** — create all 5 screen composables with just a centered title text for now:
   - OnboardingScreen
   - DashboardScreen
   - AddEntryScreen
   - ChallengeScreen
   - SettingsScreen

7. **Data model** (data/model/PasswordEntry.kt):
   @Serializable data class with fields:
   - id: String (UUID)
   - serviceName: String
   - serviceColor: Long
   - serviceIcon: String
   - encryptedEmail: String
   - emailIV: String
   - passwordHash: String
   - passwordSalt: String
   - reminderDays: Int
   - lastVerified: Long
   - createdAt: Long
   - streak: Int (default 0)
   - totalAttempts: Int (default 0)
   - successfulAttempts: Int (default 0)

Make sure the project compiles and runs, showing the Onboarding screen on first launch. Navigation between all screens should work even though they're just skeletons.

IMPORTANT: This is a SECURITY app. Every design and code decision should reflect that. No shortcuts on security configuration.
```

---

### PHASE 2 — Onboarding Wizard

```
Continue building MPT. Build the onboarding wizard that shows on first launch only.

Create these files:

1. **ui/screens/onboarding/OnboardingScreen.kt:**
   - Full-screen onboarding using HorizontalPager (4 pages)
   - Each page has: animated visual area (top 45%), headline, body text, progress dots + navigation
   - Support swipe between pages AND "Next" button
   - "Skip" text button in top-right corner (navigates to last page)
   - Final page has "Add My First Password →" button instead of "Next"

   Page 1 — "The Problem":
   - Headline: "When did you last type your master password?"
   - Body: "You set it once. Now you use fingerprint or a PIN. But what happens when you get a new phone, reinstall your vault, or biometrics fail?"
   - Animation: Show a password text that progressively replaces characters with "?" marks, then reforms. Loop.

   Page 2 — "The Solution":
   - Headline: "Train your memory like a muscle"
   - Body: "MPT reminds you every few days to type your master password and email from memory. Get it right? Your streak grows. Forget? You'll know before it matters."
   - Animation: Cycle of icons in a circular arrangement: calendar → bell → keyboard → checkmark, with connecting arrows drawing between them.

   Page 3 — "The Trust":
   - Headline: "Your password never leaves this device"
   - Three trust points with icons, stacked vertically with staggered entrance:
     - Lock icon: "Your password is hashed — even this app can't see it"
     - No-wifi icon: "Zero internet access — no network permissions at all"
     - Shield icon: "Encrypted storage backed by your phone's security chip"
   - Animation: Central shield with cloud/wifi/server icons approaching and bouncing off.

   Page 4 — "Let's Start":
   - Headline: "Ready to train?"
   - Body: "Add your first master password and set up your practice schedule. It takes 30 seconds."
   - Large CTA button: "Add My First Password →"
   - Animation: MPT shield logo with gentle pulse/glow effect.

2. **ui/screens/onboarding/OnboardingViewModel.kt:**
   - Tracks current page index
   - Manages onboarding completion state
   - Methods: nextPage(), skipToEnd(), completeOnboarding()
   - Reads/writes onboarding_completed flag in SharedPreferences

3. **ui/screens/onboarding/OnboardingPage.kt:**
   - Reusable composable for a single onboarding page
   - Parameters: visual content (composable lambda), headline, body, page index
   - Handles staggered entrance animations for text elements

4. **ui/components/OnboardingAnimations.kt:**
   - PasswordFadeAnimation composable (Screen 1): password text with characters randomly replacing with "?" then reforming, looping
   - CycleAnimation composable (Screen 2): 4 icons in circle with arrows drawing between them
   - ShieldBounceAnimation composable (Screen 3): central shield, cloud/wifi/server icons float toward and bounce away with ❌ flash
   - LogoPulseAnimation composable (Screen 4): shield scales 1.0↔1.05 with radial glow pulse
   - Use Compose animation APIs: rememberInfiniteTransition, animateFloat, Animatable

5. **Progress dots component:**
   - Row of 4 dots
   - Active: filled circle 8dp, accent color
   - Inactive: outlined circle 6dp, muted
   - Smooth size and color transitions

6. **Update NavGraph.kt:**
   - "Add My First Password" navigates to AddEntry with isFromOnboarding=true
   - When AddEntry saves with isFromOnboarding=true: set onboarding_completed=true, navigate to Dashboard (clear entire backstack so back button exits app)
   - App killed mid-onboarding → shows onboarding again (flag not set until entry saved)

DESIGN: Pages should feel premium. Animations smooth at 60fps. Content centered with good spacing. Headlines: headlineMedium bold. Body: bodyLarge, max ~280dp width. Support light + dark theme.

TEST: First launch → onboarding → swipe 4 pages → tap CTA → AddEntryScreen → save → Dashboard with entry. Kill app → relaunch → straight to Dashboard.
```

---

### PHASE 3 — Security Layer (Crypto + Hashing + Storage)

```
Continue building MPT. We now need the security layer — this is the most critical part of the app.

Create these files:

1. **data/security/KeystoreManager.kt:**
   - Generate or retrieve AES-256-GCM key from Android Keystore
   - Key alias: "mpt_master_key"
   - Use KeyGenParameterSpec with:
     - PURPOSE_ENCRYPT | PURPOSE_DECRYPT
     - BLOCK_MODE_GCM
     - ENCRYPTION_PADDING_NONE
     - KeySize 256
   - Prefer StrongBox if available (setIsStrongBoxBacked)
   - Method: getOrCreateKey(): SecretKey

2. **data/security/CryptoUtil.kt:**
   - encrypt(plaintext: String, key: SecretKey): Pair<String, String> → (ciphertext Base64, IV Base64)
   - decrypt(ciphertext: String, iv: String, key: SecretKey): String
   - Use AES/GCM/NoPadding cipher
   - GCM generates its own IV — extract after encryption
   - Inputs/outputs are Base64-encoded strings

3. **data/security/HashUtil.kt:**
   - generateSalt(): String — 32 bytes, SecureRandom, Base64
   - hashPassword(password: CharArray, salt: String): String — Argon2id hash, Base64
   - verifyPassword(password: CharArray, salt: String, expectedHash: String): Boolean
   - Argon2id parameters: timeCost=3, memoryCost=65536, parallelism=4, hashLength=32
   - Use argon2kt library
   - CRITICAL: After hashing, zero out the CharArray: password.fill('\u0000')

4. **data/repository/PasswordRepository.kt:**
   - Uses EncryptedSharedPreferences (AndroidX Security)
   - Methods: getAllEntries(), getEntry(id), saveEntry(entry), deleteEntry(id), updateVerification(id, success)
   - Entries stored as JSON array via Kotlinx Serialization
   - updateVerification updates lastVerified, streak, attempt counters

SECURITY REQUIREMENTS:
- NEVER use String for passwords — always CharArray (Strings are immutable, linger in memory)
- Always zero out CharArrays after use
- No Log.d() with sensitive data
- Handle KeyPermanentlyInvalidatedException gracefully (user changed device lock → "data reset required" flow)

TEST: Create entry with email+password → encrypt email → hash password → store → retrieve → decrypt email → verify password.
```

---

### PHASE 4 — Add Entry Screen (Full UI)

```
Continue building MPT. Build the complete Add Entry screen.

Create AddEntryScreen.kt and AddEntryViewModel.kt:

**AddEntryViewModel:**
- State: serviceName, serviceColor, serviceIcon, email, password, confirmPassword, reminderDays, isLoading, errors map
- Validation: service name required 1-50 chars, email required basic format, password required min 1 char, confirm must match
- Save: validates → hashes password → encrypts email → creates PasswordEntry → saves via repository
- Uses CharArray for password fields, zeros after save

**AddEntryScreen UI — Material 3:**

1. Top app bar with back arrow and "Add Entry" title

2. Service section:
   - Text field with autocomplete suggestions: Bitwarden, 1Password, LastPass, KeePass, Dashlane, Proton Pass, NordPass, custom
   - Row of 8 selectable icons: Shield, Key, Lock, Fingerprint, Safe, Cloud, Globe, Star (Material Icons Extended)
   - Row of 8 selectable accent colors: Red, Blue, Green, Purple, Orange, Teal, Pink, Indigo (colored circles)

3. Credentials section:
   - Email field (keyboard type: email)
   - Password field (obscured, visibility toggle, paste DISABLED)
   - Confirm password field (matching indicator)

4. Reminder section:
   - Material 3 SegmentedButton: 3d / 5d / 7d / 14d / 30d
   - Helper: "You'll be reminded to verify every X days"

5. Full-width Save button (loading spinner during hashing, disabled until valid)

6. Animations: staggered field entrance, icon/color scale on tap, error fade-in

Scrollable if content exceeds viewport. Proper IME actions (next → next → done).

Handle isFromOnboarding flag: on save, if from onboarding → set onboarding_completed=true → navigate to Dashboard clearing backstack.
```

---

### PHASE 5 — Dashboard Screen (Full UI)

```
Continue building MPT. Build the complete Dashboard screen.

Create DashboardScreen.kt, DashboardViewModel.kt, and components: EntryCard.kt, StatusBadge.kt, ProgressRing.kt

**DashboardViewModel:**
- Loads all entries, calculates status (green/amber/red), sorts: overdue first → due soon → recently verified

**DashboardScreen UI:**

1. Top app bar: "MPT" + shield icon + settings gear button
2. Summary bar: "X entries" • "Y overdue" (red if >0)
3. Entry cards (LazyColumn):
   - Left: circular icon with accent color background
   - Center: service name (bold), masked email, status badge, streak
   - Right: ProgressRing (animated arc, green→amber→red, days remaining in center)
   - Tap → ChallengeScreen(entryId)
   - Swipe left → delete (red), swipe right → edit (blue)
4. FAB: "+" → AddEntryScreen
5. Empty state: shield icon + "No passwords to train" + CTA

Email masking: first char + "•••" + last char before @ + "@" + first 2 of domain + "•••" + TLD

Status: Green (<reminderDays-1 elapsed), Amber (==reminderDays-1), Red (>=reminderDays)

Animations: staggered card slide-in (50ms each), ProgressRing spring animation, pull-to-refresh, scale-down press feedback, overdue badge pulse.

Delete confirmation: "Delete [ServiceName]? This cannot be undone."
```

---

### PHASE 6 — Challenge Screen (The Core Feature)

```
Continue building MPT. Build the Challenge screen — the heart of the app.

Create ChallengeScreen.kt, ChallengeViewModel.kt, SuccessAnimation.kt, ShakeAnimation.kt

**ChallengeViewModel:**
- Loads entry by ID
- State: email input, password input, verificationResult (null/success/emailWrong/passwordWrong/bothWrong), attemptCount, isLocked, lockTimeRemaining, isVerifying
- verify(): decrypt email → compare (case-insensitive, trimmed), hash password → compare, determine result, update repository
- After 5 consecutive fails: lock 60 seconds
- After 3 fails: offer masked email hint
- Streak: increment on success, reset to 0 after 3 consecutive failures
- Zero out password CharArray after verification

**ChallengeScreen UI:**

1. Header: large service icon + accent color + service name + "Type your credentials from memory"
2. Email field (empty, placeholder "Email address")
3. Password field (obscured, no paste, placeholder "Master password")
4. Verify button (full width, loading during hash)

Results:
- Both correct: green flash, animated checkmark (SVG path draw), "Perfect! ✓", streak scale-up, confetti particles, auto-return 2.5s
- Email wrong only: amber, email field shakes + amber outline, "Password correct, email doesn't match", email clears and focuses
- Password wrong: red, both shake, "That's not right. Take a breath and try again.", both clear
- Both wrong: red, "Neither matched. Think carefully and try again."
- 3 failures: "Tap for email hint" → masked email
- 5 failures: lock 60s with countdown timer around verify button

Shake: 0→-10dp→10dp→-6dp→6dp→0, 400ms per field
Bottom: "Last verified: 8 days ago" + "Attempts today: 3"
```

---

### PHASE 7 — Notifications & WorkManager

```
Continue building MPT. Implement the notification/reminder system.

Create worker/ReminderWorker.kt + update MPTApplication.kt:

1. **ReminderWorker (PeriodicWorkRequest — daily):**
   - Read all entries, check if daysSinceLastVerified >= reminderDays
   - Single overdue: notification "Time to practice!" / "Your [ServiceName] password check is due"
   - Multiple overdue: summary InboxStyle "X passwords need practice"
   - Deep link: mpt://challenge/{entryId} → ChallengeScreen

2. **Notification setup:**
   - Channel: "mpt_reminders", importance DEFAULT
   - Create channel in MPTApplication.onCreate()

3. **WorkManager in MPTApplication:**
   - Enqueue with ExistingPeriodicWorkPolicy.KEEP
   - Unique name: "mpt_daily_reminder_check"

4. **Deep link handling in NavGraph**

5. **POST_NOTIFICATIONS permission** for Android 13+ (request on first entry creation)

6. **RECEIVE_BOOT_COMPLETED** permission + BootReceiver to reschedule after reboot
```

---

### PHASE 8 — Settings & App Lock

```
Continue building MPT. Build Settings screen and biometric app lock.

**SettingsScreen.kt + SettingsViewModel.kt:**

1. App Lock: toggle biometric/PIN to open app (BiometricPrompt on launch + resume after 30s background)
2. Default reminder interval (segmented button)
3. Notifications: enable toggle + quiet hours (time pickers)
4. Appearance: theme System/Light/Dark
5. Data: "Delete all data" — red button, dialog requires typing "DELETE"
6. About: version + security statement about zero network, Argon2id hashing, device-only storage

**Biometric gate in MainActivity:**
- On cold start + warm start after 30s: BiometricPrompt before showing content
- CryptoObject tied to Keystore
- Fallback: device PIN/pattern
- Failed/cancelled: locked screen with retry, no entry data visible
```

---

### PHASE 9 — Polish, Testing & Release

```
Final phase for MPT.

1. Animations: staggered card entrance, shared element service icon transitions, accent-colored ripples, keyboard inset animation, spring physics on progress rings

2. Edge cases: timezone changes, date set to past, Keystore key invalidation (show "Security reset required"), app kill during challenge, rotation/config change, all empty states

3. Accessibility: contentDescription on all elements, 48dp touch targets, TalkBack support, WCAG AA contrast

4. Performance: Argon2 on Dispatchers.Default, lazy loading, minimal APK size

5. ProGuard/R8: minification + obfuscation, keep serialization models + Argon2 native lib

6. App icon: adaptive icon — shield on deep navy background, vector drawable

7. Code cleanup: remove TODOs, remove debug logs, KDoc on public functions

8. Build release APK and test on API 26, 31, 34:
   - Full flow: onboarding → add entry → notification → challenge → streak
   - 5 failures → lockout → cooldown → retry
   - App lock → background → resume → biometric
   - Delete all data → clean slate
   - Onboarding never reappears after first entry saved
```

---

## 10. DEVELOPMENT ENVIRONMENT SETUP

1. **Android Studio Ladybug (2024.x)** or newer
2. **JDK 17** (bundled with Android Studio)
3. **Android SDK 34+**, Build Tools 34.0.0
4. **Emulator:** Pixel 7 with API 34, Google APIs
5. **Git** — initialize from Phase 1

No Node. No Python. Pure Android.

---

## 11. TIMELINE ESTIMATE

| Phase | Description | Est. Time |
|-------|-------------|-----------|
| 1 | Scaffolding + theme + navigation | 2-3 hours |
| 2 | Onboarding wizard | 3-4 hours |
| 3 | Security layer (crypto, hashing, storage) | 3-4 hours |
| 4 | Add Entry screen | 3-4 hours |
| 5 | Dashboard screen | 3-4 hours |
| 6 | Challenge screen | 4-5 hours |
| 7 | Notifications + WorkManager | 2-3 hours |
| 8 | Settings + App lock | 2-3 hours |
| 9 | Polish + testing + release | 3-4 hours |
| **Total** | | **~25-34 hours** |

---

*Document version: 2.0 — Merged final spec*  
*Last updated: April 2026*
