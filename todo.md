# DinkLog — Development Tickets

## Status Key
- [ ] Not started
- [x] Complete

---

## COMPLETED — Data Layer (PR1)

- [x] **DL-1: Define database constants** — `utils/Constants.kt` with `DatabaseConstants`, `UserColumns`, `MatchColumns` objects. All table/column names as `const val` to eliminate magic strings.
- [x] **DL-2: Create User entity** — `models/User.kt` Room `@Entity` with auto-generated PK, unique email index, password hash, and timestamp with default.
- [x] **DL-3: Create Match entity** — `models/Match.kt` Room `@Entity` with foreign key to User (CASCADE delete), indexed `userId` column, nullable fields for optional match data.
- [x] **DL-4: Create UserDao** — `models/UserDao.kt` DAO interface with `insert`, `findByEmail`, `findById`, `emailExists` queries. All `suspend` functions.
- [x] **DL-5: Create MatchDao** — `models/MatchDao.kt` DAO interface with `insert`, `delete`, `getMatchesByUser` (returns `Flow`), plus aggregate stats queries (`getTotalMatches`, `getWinCount`, `getLossCount`).
- [x] **DL-6: Create AppDatabase singleton** — `models/AppDatabase.kt` Room database with double-checked locking singleton, `@Volatile` instance, `userDao()` and `matchDao()` abstract accessors.

---

## COMPLETED — UI Layer, Navigation & Lifecycle (PR2)

### Build & Config

- [x] **CF-1: Set up build.gradle files** — Root and app-level `build.gradle.kts` with Room, Coroutines, Navigation Component, Material Design, and ViewBinding dependencies. Apply `kotlin-kapt` plugin for Room annotation processing.
- [x] **CF-2: Configure AndroidManifest.xml** — Declare `DinkLogApp` as `android:name` on `<application>`, register `MainActivity` as launcher activity, add internet permission for future API use.

### App Foundation

- [x] **AF-1: Implement DinkLogApp.kt** — `Application` subclass that initializes the Room database singleton on app startup. This is the entry point for app-wide setup.
- [x] **AF-2: Implement MainActivity.kt** — Single Activity that hosts the NavHostFragment. Log all 6 lifecycle methods (`onCreate`, `onStart`, `onResume`, `onPause`, `onStop`, `onDestroy`) with `Log.d("MainActivity", "...")` for the checkpoint demo. Set up bottom navigation bar for History/Stats tabs.

### Repository Layer

- [x] **RP-1: Implement UserRepository.kt** — Wraps `UserDao` with business logic: password hashing (SHA-256), email format validation, login credential checking, signup with email uniqueness check. All functions return `Result<T>` for success/failure handling. Use `Dispatchers.IO` for DB calls.
- [x] **RP-2: Implement MatchRepository.kt** — Wraps `MatchDao` with: `addMatch()` that auto-calculates `isWin` from scores, `getMatchesForUser()` returning `Flow<List<Match>>`, `getStats()` aggregating win/loss/total. Use `Dispatchers.IO`.

### Utilities

- [x] **UT-1: Implement PreferencesManager.kt** — SharedPreferences wrapper for session management: `saveUserId(Long)`, `getUserId(): Long?`, `clearSession()`. Store under a named constant key. This keeps login state across app restarts.
- [x] **UT-2: Implement DateFormatter.kt** — Extension functions for `Long` timestamps: `formatMatchDate()` → "Mar 15, 2026", `formatRelativeTime()` → "2 days ago". Used by History and Stats screens.
- [x] **UT-3: Implement Validation.kt** — `String.isValidEmail()` extension function using `android.util.Patterns.EMAIL_ADDRESS` regex. Reusable across Login and Signup validation.

### UI Screens (Fragments + ViewModels)

- [x] **UI-1: Login screen** — `Login.kt` Fragment with email/password fields and Login/Sign Up buttons. `LoginViewModel.kt` exposes `StateFlow<LoginState>` (Idle/Loading/Success/Error). On success, save userId via PreferencesManager and navigate to History. Log all Fragment lifecycle methods (`onCreate`, `onCreateView`, `onStart`, `onResume`, `onPause`, `onStop`, `onDestroyView`, `onDestroy`) for checkpoint demo.
- [x] **UI-2: Signup screen** — `Signup.kt` Fragment with email/name/password/confirm-password fields. `SignupViewModel.kt` validates input (email format, password strength, passwords match) and calls `UserRepository.signup()`. On success, auto-login and navigate to History. Log Fragment lifecycle methods.
- [x] **UI-3: History screen** — `History.kt` Fragment with RecyclerView showing matches ordered by date (newest first). `HistoryViewModel.kt` observes `Flow<List<Match>>` from repository and exposes via `StateFlow`. Show empty state when no matches. FAB button navigates to AddMatch. Log Fragment lifecycle methods.
- [x] **UI-4: AddMatch screen** — `AddMatch.kt` Fragment with form: opponent1 (required), opponent2 (optional), partner (optional), team score (required), opponent score (required), location (required), notes (optional). `AddMatchViewModel.kt` validates inputs and saves via `MatchRepository`. Navigate back to History on success.
- [x] **UI-5: Stats screen** — `Stats.kt` Fragment displaying total matches, wins, losses, win percentage. `StatsViewModel.kt` queries aggregate data from `MatchRepository` and exposes via `StateFlow<MatchStats?>`.

### Reusable Components

- [x] **CP-1: Implement MatchAdapter.kt** — `ListAdapter<Match, ViewHolder>` with `DiffUtil.ItemCallback` for efficient RecyclerView updates. Each item shows date, opponents, score, and win/loss indicator.

### Navigation

- [x] **NV-1: Set up Navigation Component** — `nav_graph.xml` defining all Fragment destinations and actions. Login → History (on auth success), Login ↔ Signup, History → AddMatch, bottom nav between History ↔ Stats. Session check: if `PreferencesManager` has userId, start at History; otherwise start at Login.

### Layouts

- [x] **LY-1: Create layout XMLs** — `activity_main.xml` (NavHostFragment + BottomNavigationView), `fragment_login.xml`, `fragment_signup.xml`, `fragment_history.xml` (RecyclerView + FAB), `fragment_add_match.xml` (form), `fragment_stats.xml`, `item_match.xml` (RecyclerView item).

### Checkpoint 2 Deliverables

- [ ] **CK-1: Lifecycle logging verification** — Confirm all 6 Activity lifecycle methods and all 8 Fragment lifecycle methods log to Logcat with descriptive tags. Trigger them by rotating device, navigating between screens, and pressing back/home.
- [ ] **CK-2: Physical device install** — Build and install debug APK on a real Android device (Android 9+). Verify all screens render and navigation works.
- [ ] **CK-3: Debugging demo prep** — Set breakpoints in `LoginViewModel` (credential check), `MatchRepository` (match insert), and a Fragment lifecycle method. Step through code and inspect variable state.
- [ ] **CK-4: Profiling demo prep** — Run CPU profiler during match list scroll and memory profiler during navigation. Capture screenshots for submission.
- [ ] **CK-5: Screenshots & submission** — Capture: profiler output, lifecycle Logcat logs, debugger with breakpoints/variables. Zip project and upload to OneDrive.

---

## FUTURE — Post-Checkpoint Enhancements

- [ ] **FT-1: GPS auto-location** — Use `FusedLocationProviderClient` to detect court location when logging a match. Reverse-geocode coordinates via Android `Geocoder` to auto-fill location field. Request `ACCESS_FINE_LOCATION` permission at runtime.
- [ ] **FT-2: Google Places API integration** — Use Places API to convert GPS coordinates to named locations (e.g., "Jesse Owens North"). Show nearby courts as suggestions.
- [ ] **FT-3: Weather API integration** — Fetch wind/weather conditions at match time and location. Store as optional match metadata.
- [ ] **FT-4: Map visualization** — Google Maps showing where user has played over time. Tap a pin to see matches at that location.
- [ ] **FT-5: Logout functionality** — Clear PreferencesManager session, navigate back to Login screen.
- [ ] **FT-6: Delete match** — Swipe-to-delete on History list with confirmation dialog.
- [ ] **FT-7: Advanced analytics** — Performance trends over time, win rate by partner, win rate by location. Charts using a library like MPAndroidChart.
- [ ] **FT-8: Cloud backup** — Firebase sync for multi-device support.
- [ ] **FT-9: Export data** — Export match history to CSV or PDF.
