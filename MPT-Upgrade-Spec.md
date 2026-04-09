# MPT — Master Password Trainer
## Upgrade Spec v1.0 — Post-Launch Improvements
## Claude Code Agent Prompts (Execute in Order)

---

## OVERVIEW

The core MPT app is complete with onboarding, dashboard carousel, add/edit entries, challenge screen, notifications, settings, and app lock. This document contains 8 upgrade phases that improve the app's engagement, security, and usability. Each phase is independent but should be executed in order as some later phases reference earlier additions.

**Rule: No upgrade should break existing functionality. After each phase, test that all existing features still work — onboarding, add/edit entry, challenge, notifications, settings, carousel, bottom sheet, password hints.**

---

### UPGRADE 1 — Streak Calendar & History View

```
Add a streak calendar and verification history to MPT. This gives users visual proof of their consistency — like a GitHub contribution graph or Duolingo streak calendar.

## New screen: EntryDetailScreen

Create a new screen accessible by tapping a small info/stats icon on each dashboard card (bottom-right corner of the card, small "i" circle icon or a bar chart icon). Do NOT replace the existing card tap behavior — tapping the card still opens the ChallengeScreen. This is a separate icon button on the card.

Alternatively, add a third option to the long-press bottom sheet: "View stats" alongside Edit and Delete.

Both entry points navigate to a new EntryDetailScreen(entryId).

## EntryDetailScreen layout:

1. **Header:**
   - Service icon + accent color + service name (large)
   - Current streak: "🔥 12 streak" in large text
   - Best streak ever: "Personal best: 24" in smaller muted text

2. **Monthly calendar grid:**
   - Shows current month by default
   - Swipe left/right or arrow buttons to navigate months
   - Each day cell is a small square (like a GitHub heatmap):
     - Green filled: successful verification that day
     - Red filled: failed verification that day (all attempts failed, no success)
     - Gray/empty: no attempt that day
     - Today: outlined/highlighted border
     - Days with multiple verifications: slightly brighter green (but one shade is fine for v1)
   - Month and year label above the grid
   - Day-of-week headers: M T W T F S S

3. **Stats section below calendar:**
   - Total verifications: "47 total checks"
   - Success rate: "93% success rate" with a small circular progress indicator
   - Current streak: "12 days"
   - Best streak: "24 days"
   - Average interval: "Every 4.2 days" (average days between verifications)
   - Member since: "Created March 15, 2026"

4. **Recent activity list** (below stats):
   - Last 10 verification attempts
   - Each row: date + time + result icon (green checkmark or red X)
   - Simple vertical list, no pagination needed

## Data model changes:

Add a new data structure for tracking history. Since we use EncryptedSharedPreferences and not a database, store history efficiently:

```kotlin
@Serializable
data class VerificationRecord(
    val entryId: String,
    val timestamp: Long,
    val success: Boolean
)
```

- Store verification records as a JSON array under key "verification_history"
- Add a record every time a challenge is completed (success or failure)
- Cap history at 365 records per entry — delete oldest when exceeded
- Update the existing challenge verification flow to also append a VerificationRecord

## Update PasswordEntry model:
- Add: bestStreak: Int (default 0)
- When current streak exceeds bestStreak, update bestStreak
- bestStreak is never reset (unlike streak which resets on consecutive failures)

## Dashboard card update:
- Add a small stats icon button in the bottom-right area of each card
- Icon: bar chart or "i" in a circle
- Tap → navigate to EntryDetailScreen

## Design:
- Calendar should feel clean and minimal — no heavy borders, just colored squares in a grid
- Use the entry's accent color for the successful day squares instead of always green — ties it to the card visually
- Stats use large numbers with small labels below (like a fitness app dashboard)
- Support dark and light theme

Test:
- Create entry → complete 3 challenges (mix of success and failure) → open stats → calendar shows colored dots on correct days → stats numbers are accurate
- Navigate between months → works even on months with no data (shows empty grid)
- Best streak persists even after current streak resets
- History caps at 365 per entry
```

---

### UPGRADE 2 — Difficulty Scaling

```
Add adaptive difficulty to MPT challenges. After users build a strong streak, the challenge gets slightly harder to ensure real recall rather than autopilot muscle memory.

## Difficulty levels:

Define 3 difficulty tiers based on current streak:

**Normal (streak 0-4):**
- Standard challenge: email/username field first, password field second
- No time pressure
- This is the existing behavior, change nothing

**Intermediate (streak 5-9):**
- Fields are shown in REVERSED order: password field first, email/username field second
- Small banner at top: "Reversed order — intermediate mode" with a subtle accent color
- No time pressure
- Why: breaking the visual habit forces genuine recall rather than muscle memory triggered by seeing the same field layout

**Advanced (streak 10+):**
- Fields in reversed order (same as intermediate)
- A 5-second countdown delay before input fields become active. Show a pulsing "Recall your credentials..." message with a circular countdown animation. After 5 seconds, fields unlock and the user can type.
- Small banner: "Delayed recall — advanced mode" 
- Why: the delay prevents users from immediately typing from muscle memory and forces them to actually think and recall

## Implementation:

1. **ChallengeViewModel:**
   - On load, check current streak for the entry
   - Set difficulty tier based on streak thresholds
   - Control field order and delay state

2. **ChallengeScreen:**
   - Normal: existing layout unchanged
   - Intermediate: swap the visual order — password field on top, email/username below. Labels stay correct so there's no confusion, just different ordering.
   - Advanced: show a delay overlay/countdown for 5 seconds. Fields are visible but disabled (grayed out). Countdown animation in center. After 5s, fields enable with a subtle "unlock" animation. Then same reversed order as intermediate.

3. **Difficulty banner:**
   - Sits between the header (service name) and the input fields
   - Small, not intrusive — a rounded chip or subtle text
   - Uses accent color from the entry
   - Intermediate: shows a shuffle/swap icon + "Intermediate mode"
   - Advanced: shows a brain/timer icon + "Advanced mode"

4. **Settings integration:**
   - Add a toggle in Settings under a new "Challenge" section: "Adaptive difficulty" — ON by default
   - When OFF, all challenges use Normal difficulty regardless of streak
   - Helper text: "Increases challenge difficulty as your streak grows"

5. **Streak thresholds should be constants** in a config object so they're easy to tune later

## Important:
- Difficulty is purely a UI change — the verification logic (hash comparison) stays identical
- If the user fails and streak drops below a threshold, difficulty drops back down on the next challenge
- The password hint behavior (shown after 3 failures) works the same at all difficulty levels
- If no email/username is stored, intermediate/advanced only reverses nothing — just show password field with the delay for advanced

Test:
- Streak 0-4: normal challenge layout
- Get streak to 5: next challenge shows reversed fields + intermediate banner
- Get streak to 10: next challenge shows 5-second delay + reversed fields + advanced banner
- Fail enough to drop streak to 3: next challenge is back to normal
- Turn off adaptive difficulty in settings: all challenges are normal regardless of streak
```

---

### UPGRADE 3 — Home Screen Widget

```
Add an Android home screen widget to MPT that shows password check status at a glance.

## Widget design:

**Small widget (2x1 cells):**
- Shows count of overdue entries: "2 overdue" in red or "All good ✓" in green
- Tap anywhere → opens MPT dashboard

**Medium widget (4x2 cells):**
- Shows a compact list of entries (up to 3 visible):
  - Each row: service icon color dot + service name + status (green dot / amber dot / red dot)
  - Entries sorted: overdue first, then due soon, then on track
- If more than 3 entries, show "and X more..." at bottom
- Tap on a specific entry row → opens ChallengeScreen for that entry directly
- Tap on empty area → opens dashboard
- Bottom of widget: last updated time in tiny muted text

## Implementation:

1. **Create widget files:**
   - `widget/MPTWidgetProvider.kt` — extends AppWidgetProvider
   - `widget/MPTWidgetService.kt` — RemoteViewsService for the list in medium widget
   - `res/xml/mpt_widget_info.xml` — widget metadata (min size, preview, resize mode)
   - `res/layout/widget_small.xml` — RemoteViews layout for small widget
   - `res/layout/widget_medium.xml` — RemoteViews layout for medium widget
   - `res/layout/widget_list_item.xml` — individual row in medium widget list

2. **Widget updates:**
   - Update widget when: app opens, challenge completed, entry added/deleted
   - Also update on a schedule via `updatePeriodMillis` (minimum 30 minutes per Android rules)
   - Use `AppWidgetManager.updateAppWidget()` after any data change
   - Add a broadcast action in ReminderWorker to also trigger widget update when checking for overdue entries

3. **AndroidManifest additions:**
   - Register widget provider as a receiver
   - Register widget service

4. **Styling:**
   - Widget background: use system default widget background with rounded corners (Material You compatible on Android 12+)
   - For Android 12+: use `@android:id/background` with rounded corner radius
   - Text colors should work on both light and dark home screens — use system default widget text colors
   - Status dots: green (#4CAF50), amber (#FF9800), red (#F44336) — same as in-app

5. **Deep links:**
   - Small widget tap: PendingIntent to launch MainActivity (dashboard)
   - Medium widget row tap: PendingIntent with deep link `mpt://challenge/{entryId}`

## Important:
- Widgets use RemoteViews — they CANNOT use Jetpack Compose. Use traditional XML layouts.
- Keep widget layouts simple — complex layouts cause performance issues
- Widget must read data from EncryptedSharedPreferences (same source as the app)
- No sensitive data on the widget — show service names and status dots only, NO masked emails, NO hints

Test:
- Add small widget to home screen → shows "All good" or "X overdue"
- Add medium widget → shows list of entries with status dots
- Complete a challenge → widget updates to reflect new status
- Tap widget entry → opens correct challenge screen
- Works after device reboot
```

---

### UPGRADE 4 — Encrypted Backup & Restore

```
Add encrypted backup and restore functionality to MPT. Users can export their data as an encrypted file and import it on another device. Still zero network — just a file the user manages themselves.

## Settings additions:

Add a new section in Settings called "Backup & Restore" (place it between Help and Data sections):

- **"Export encrypted backup"** button with a download/export icon
- **"Import backup"** button with an upload/import icon

## Export flow:

1. User taps "Export encrypted backup"
2. A dialog appears: "Create backup password"
   - Password field (with visibility toggle)
   - Confirm password field
   - Helper text: "This password encrypts your backup file. You'll need it to restore. It can be different from your master passwords."
   - "Create Backup" button
3. On confirm:
   - Collect all PasswordEntry data (including hashes, encrypted emails, IVs, salts, hints, history, settings)
   - Serialize everything to JSON
   - Generate a random salt for the backup encryption
   - Derive an AES-256 key from the user's backup password using PBKDF2 (100,000 iterations) or Argon2id
   - Encrypt the JSON payload with AES-256-GCM using the derived key
   - Package into a file: salt (32 bytes) + IV (12 bytes) + encrypted data
   - File extension: `.mptbackup`
   - File name: `mpt_backup_YYYY-MM-DD.mptbackup`
   - Open Android share sheet / file save picker (ACTION_CREATE_DOCUMENT) so user can save the file wherever they want (Downloads, Google Drive, USB, etc.)
4. Show success toast: "Backup saved successfully"

## Import flow:

1. User taps "Import backup"
2. Warning dialog: "Importing a backup will REPLACE all current data. This cannot be undone. Continue?"
   - "Cancel" and "Continue" buttons
3. On continue: open file picker (ACTION_OPEN_DOCUMENT) filtered to `.mptbackup` files (also allow `*/*` as fallback since file type filtering is unreliable on Android)
4. User selects a file → password dialog appears:
   - Password field: "Enter backup password"
   - "Restore" button
5. On restore:
   - Read file: extract salt + IV + encrypted data
   - Derive key from entered password + salt
   - Attempt decryption
   - If decryption fails (wrong password or corrupted file): show error "Incorrect password or corrupted backup file. Try again."
   - If decryption succeeds:
     - Parse JSON payload
     - Clear ALL existing data (entries, history, settings)
     - Write imported data to EncryptedSharedPreferences
     - Regenerate Android Keystore encryption key for emails (re-encrypt all emails with the new device's Keystore key since the old device's key won't exist here)
     - Show success: "Backup restored — X entries imported"
     - Navigate to dashboard

## CRITICAL security detail for email re-encryption:

The exported data includes emails encrypted with the OLD device's Keystore key. That key doesn't exist on the new device. So during export, we need to:
- Decrypt each email using the current Keystore key
- Include the PLAINTEXT email in the backup JSON (the backup file itself is encrypted with the backup password, so the email is still protected)
- On import: encrypt each email with the NEW device's Keystore key

Password hashes and salts transfer directly — they don't depend on the Keystore.

## Backup file format (internal structure):

```
[32 bytes salt][12 bytes IV][encrypted JSON payload]
```

The JSON payload before encryption:
```json
{
  "version": 1,
  "exportDate": 1234567890,
  "entries": [
    {
      "id": "...",
      "serviceName": "...",
      "serviceColor": 123456,
      "serviceIcon": "shield",
      "email": "plaintext-email@example.com",
      "passwordHash": "base64...",
      "passwordSalt": "base64...",
      "passwordHint": "...",
      "reminderDays": 7,
      "lastVerified": 1234567890,
      "createdAt": 1234567890,
      "streak": 5,
      "bestStreak": 12,
      "totalAttempts": 47,
      "successfulAttempts": 44
    }
  ],
  "history": [...],
  "settings": {
    "defaultInterval": 7,
    "appLockEnabled": false,
    "theme": "system",
    "notificationsEnabled": true,
    "quietHoursStart": "22:00",
    "quietHoursEnd": "08:00",
    "adaptiveDifficulty": true
  }
}
```

## File versioning:
- Include `"version": 1` in the JSON payload
- Future app updates that change the data model should increment this and handle migration during import

## Important:
- The backup file NEVER contains the Android Keystore key — that's non-exportable by design
- If backup password is weak, the backup is still at risk — consider showing a password strength hint but don't enforce requirements (user's choice)
- Test import on a different emulator/device than export to verify Keystore re-encryption works

Test:
- Export backup → file appears in chosen location → file is not readable as plain text (encrypted)
- Import on same device with correct password → all entries restored with correct data
- Import with wrong password → error message, no data changed
- Create a second emulator → install MPT → import the backup file → all entries appear → run challenge → password verification works → emails display correctly masked
- Export with 0 entries → should still work (empty backup)
```

---

### UPGRADE 5 — Panic Wipe

```
Add a panic wipe feature to MPT. If a specific number of wrong password attempts are made in a row across ALL challenges combined, automatically delete all app data.

## How it works:

- There is a GLOBAL consecutive failure counter (not per-entry)
- Every failed challenge attempt (wrong password) increments this counter
- Every successful challenge resets it to 0
- When the counter reaches the panic threshold, ALL data is wiped instantly

## Settings:

Add to the Security section in Settings, below App Lock:

- **"Panic wipe"** — toggle, OFF by default
- When enabled, show a dropdown/selector: "Wipe after X failed attempts"
  - Options: 15, 25, 50 (conservative defaults — accidental wipe would be very bad)
  - Default: 25
- Warning text below in muted red: "All entries will be permanently deleted after the selected number of consecutive failed attempts across all entries. Successful verification resets the counter."
- When toggling ON, show a confirmation dialog: "Are you sure? If you fail X consecutive times, all your data will be permanently deleted with no way to recover it. Make sure you have a backup."

## Implementation:

1. **Global failure counter:**
   - Stored in SharedPreferences (not encrypted prefs — it's just a number)
   - Key: "global_consecutive_failures"
   - Incremented in ChallengeViewModel after any password failure
   - Reset to 0 after any password success
   - Checked after each increment: if >= threshold AND panic wipe enabled → trigger wipe

2. **Wipe action:**
   - Delete all entries from EncryptedSharedPreferences
   - Delete all verification history
   - Reset all settings to defaults
   - Delete Keystore key
   - Reset onboarding flag (so onboarding shows again — the app looks freshly installed)
   - Navigate to onboarding screen with backstack cleared
   - Show no toast, no confirmation — it should look like a fresh install to an observer
   - The wipe should be SILENT — no "data deleted" message. The app just appears as fresh.

3. **Challenge screen indicator (optional but recommended):**
   - Do NOT show the current failure count — that would help an attacker gauge how many attempts remain
   - Do NOT show any warning before the wipe happens
   - The wipe should be sudden and without warning from the attacker's perspective

4. **Counter persists across app restarts** — stored in SharedPreferences

## Important:
- The panic wipe counter is GLOBAL across all entries — failing 10 times on "Bitwarden" then 15 times on "1Password" = 25 total
- Only password failures count — email-only failures do not increment (the attacker might not even have an email to try)
- The feature is OFF by default — users must consciously enable it
- Recommend users set up encrypted backup (Upgrade 4) before enabling panic wipe
- When panic wipe is enabled in Settings and the user also has no backup, show an amber warning: "Consider creating a backup first. Panic wipe will permanently delete all data."

Test:
- Enable panic wipe with threshold 15 → fail challenges 15 times in a row → app wipes → shows onboarding as if fresh install
- Enable panic wipe → fail 14 times → succeed once → counter resets → fail 14 more → no wipe (counter was reset)
- Disable panic wipe → fail 50 times → nothing happens
- After wipe, app behaves exactly like fresh install — onboarding, empty state, everything clean
```

---

### UPGRADE 6 — Multiple Password Versions

```
Add support for storing multiple password versions per entry. Some users rotate their master password and want to practice both the old and new one during the transition period.

## How it works:

- Each entry can have up to 3 password versions (hashes)
- Each version has a label: "Current", "Previous", or "Legacy" (or custom labels)
- During challenge, the app accepts ANY of the stored versions as correct
- After successful verification, the app tells the user WHICH version matched

## Data model changes:

Replace the single password hash/salt with a list:

```kotlin
@Serializable
data class PasswordVersion(
    val id: String,           // UUID
    val label: String,        // "Current", "Previous", custom label
    val passwordHash: String, // Argon2id hash
    val passwordSalt: String, // Salt
    val createdAt: Long       // When this version was added
)
```

Update PasswordEntry:
- Remove: passwordHash, passwordSalt
- Add: passwordVersions: List<PasswordVersion> (max 3)
- Migration: existing entries with single hash/salt get converted to a list with one PasswordVersion labeled "Current"

## Add Entry screen changes:

- After the password + confirm password fields, add a subtle expandable section: "Additional password versions (optional)"
- Tapping it expands to show:
  - The first password is automatically labeled "Current"
  - "Add another version" button (appears only if fewer than 3 versions exist)
  - Each additional version has:
    - A text field for label (default: "Previous" for 2nd, "Legacy" for 3rd, user can edit)
    - Password field
    - Confirm password field
    - A small X button to remove this version
- Keep it collapsed by default — most users will only have 1 password. The section should not clutter the default experience.

## Edit Entry screen changes:

- Show existing password versions as a list with their labels
- Each version shows: label + "Added March 15, 2026" + delete button (cannot delete the last remaining version)
- "Add version" button if under 3
- Editing a version's password: new password + confirm fields appear inline
- Leave empty = keep existing hash (same as current edit behavior, but per-version)

## Challenge screen changes:

- Input fields stay the same — user types one password
- Verification: hash the input and compare against ALL stored version hashes
- If ANY version matches → success
- Success message includes which version: "Correct! (matched: Current)" or "Correct! (matched: Previous)"
- This helps the user know which password they remembered
- If none match → failure (same as existing behavior)
- All versions are checked every time — no skipping

## Stats / history:
- Verification records can optionally store which version matched
- On the stats screen, show per-version success counts: "Current: 15 times, Previous: 3 times"

## Important:
- Each version has its own independent hash + salt — they're completely separate Argon2id hashes
- Migration of existing data must be seamless — existing entries should continue to work without user intervention
- The challenge screen UI doesn't change — the multi-version matching happens silently in the ViewModel
- Password hint remains a single field per entry (not per version)
- Backup/restore (Upgrade 4) needs to handle the new structure

Test:
- Create entry with 1 password → challenge works as before
- Edit entry → add second version "Previous" with different password → challenge → type either password → both accepted, result shows which matched
- Add third version → type any of the three → correct
- Delete a version → it's removed → its password no longer accepted
- Old entries created before this upgrade still work (migration)
- Export/import backup with multi-version entries works correctly
```

---

### UPGRADE 7 — Custom Notification Messages

```
Let users write their own reminder notification text per entry. A personal nudge is more effective than a generic message.

## Add Entry / Edit Entry screen change:

- Below the reminder interval section, add a new collapsible section: "Custom reminder message (optional)"
- Text input field with placeholder: "e.g. Don't forget your vault password!"
- Character limit: 80 characters (notifications get truncated beyond this on most devices)
- Counter: "23/80" shown when typing
- Helper text: "Shown in the reminder notification instead of the default message"
- If left empty, the default notification text is used

## Data model change:

- Add to PasswordEntry: `customReminderMessage: String` (default empty string "")
- Same as password hint — plain text in EncryptedSharedPreferences, no individual encryption needed

## ReminderWorker change:

- When building a notification for an overdue entry:
  - If `customReminderMessage` is not empty: use it as the notification body text
  - If empty: use the default "Your [ServiceName] password check is due"
  - Notification title stays "Time to practice!" regardless (or make title customizable too — your call, but body is more impactful)

## Backup/restore:

- Include customReminderMessage in the backup JSON payload
- Handle missing field during import of older backups (default to empty string)

Test:
- Create entry with custom message "Hey dummy, check your Bitwarden!" → wait for notification (or manually trigger worker for testing) → notification shows custom message
- Create entry without custom message → notification shows default
- Edit entry to add/change/remove custom message → next notification reflects change
```

---

### UPGRADE 8 — Statistics Screen

```
Add a global statistics screen to MPT showing aggregate data across all entries. This is a dedicated screen, separate from the per-entry stats in Upgrade 1.

## Navigation:

- Add a small bar chart icon to the Dashboard top app bar, next to the settings gear
- Tapping it opens the new GlobalStatsScreen
- Or: add it as a tab/section in Settings — your call on where it feels more natural. Dashboard top bar is probably better since stats are something users check frequently.

## GlobalStatsScreen layout:

1. **Header area — key metrics in large numbers:**
   
   Row of 2-3 large stat cards (horizontal scrollable if needed):
   - "Total Checks" — big number + small label (e.g., "142")
   - "Success Rate" — percentage with a circular progress ring (e.g., "94%")
   - "Active Streak" — longest current active streak across all entries with the service name (e.g., "18 — Bitwarden")

2. **Weekly activity chart:**
   - Bar chart showing number of verifications per day for the last 7 days
   - Each bar split into green (success) and red (failure) portions
   - Day labels below: Mon, Tue, Wed, Thu, Fri, Sat, Sun
   - If no activity on a day, show a tiny gray stub so the axis is consistent
   - Use Compose Canvas to draw this — no external charting library needed

3. **Monthly trend:**
   - Simple line or bar chart showing total verifications per month for the last 6 months
   - Months with no data show 0
   - Again, Compose Canvas — keep it simple

4. **Per-entry comparison section:**
   - List of all entries sorted by success rate (highest first)
   - Each row: service icon + name + success rate bar + streak
   - The bar is a horizontal progress bar showing success percentage, colored with the entry's accent color
   - Tap on an entry row → navigate to that entry's detail/stats screen (from Upgrade 1)

5. **Fun stats at the bottom (optional but delightful):**
   - "Most practiced": entry with the most total checks
   - "Most reliable": entry with highest success rate
   - "Needs work": entry with lowest success rate (if below 80%)
   - "Total days trained": count of unique days with at least one verification
   - "Longest gap": most days between verifications for any entry

## Data source:

- All data comes from the VerificationRecord history (added in Upgrade 1) and PasswordEntry fields
- No new data storage needed — just read and compute

## Design:

- Clean, dashboard-style layout similar to fitness or habit tracker apps
- Large numbers, minimal labels
- Charts are simple and clean — not overly detailed
- Use entry accent colors throughout so it's visually tied to the cards
- Scrollable vertically
- Support dark and light theme
- Empty state: if no verifications yet, show "Complete your first challenge to start tracking stats" with an illustration

## Performance:
- Stats computation might be slow with lots of history — calculate on Dispatchers.Default, show loading state
- Cache computed stats in ViewModel, recalculate when screen is opened (not on every recomposition)

Test:
- With 0 entries: shows empty state
- With entries but 0 verifications: shows zeros everywhere, empty charts
- Complete several challenges across multiple entries → stats screen shows accurate numbers
- Weekly chart shows correct days
- Per-entry list sorted correctly by success rate
- Tap entry in comparison list → navigates to entry detail (Upgrade 1)
```

---

## EXECUTION ORDER & DEPENDENCIES

```
Upgrade 1 (Streak Calendar)     → independent, do first — Upgrade 8 depends on its history data
Upgrade 2 (Difficulty Scaling)  → independent
Upgrade 3 (Home Widget)         → independent
Upgrade 4 (Encrypted Backup)    → independent, but do before Upgrade 5
Upgrade 5 (Panic Wipe)          → do after Upgrade 4 (so backup recommendation makes sense)
Upgrade 6 (Password Versions)   → independent but touches core data model, do carefully
Upgrade 7 (Custom Notifications)→ independent, easiest upgrade
Upgrade 8 (Statistics Screen)   → depends on Upgrade 1 (uses VerificationRecord history)
```

**Recommended order: 1 → 7 → 2 → 3 → 4 → 5 → 6 → 8**

Start with Upgrade 1 (adds the history infrastructure everything else builds on) and Upgrade 7 (quick win, easy). Then difficulty scaling and widget for engagement. Then backup and panic wipe as a pair. Password versions last since it touches the core data model. Statistics screen last since it needs the history data from Upgrade 1 to be meaningful.

---

## TESTING AFTER ALL UPGRADES

After completing all 8 upgrades, run this full regression test:

```
Full regression test for MPT after all upgrades:

1. Fresh install → onboarding works → create first entry → dashboard shows card
2. Create 3 entries with different settings (one with email, one without, one with hint, one with custom notification, one with multiple password versions)
3. Challenge each entry → verify success/failure flows at all difficulty levels
4. Check streak calendar shows correct data
5. Check global stats screen shows accurate numbers
6. Add home screen widget → verify it shows correct status and updates
7. Export encrypted backup → uninstall app → reinstall → import backup → all data restored
8. Enable panic wipe → fail enough times → app wipes → shows as fresh install
9. Settings: all new options present and functional
10. Onboarding replay from settings still works
11. Notifications fire with custom messages
12. Edit entry with multiple password versions → challenge accepts all versions
13. Dark mode / light mode: all new screens render correctly
14. Rotate device: no crashes, state preserved
```

---

*Upgrade Spec version: 1.0*  
*Builds on: MPT-Master-Spec.md v2.0*  
*Last updated: April 2026*
