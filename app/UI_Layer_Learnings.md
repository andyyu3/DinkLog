# PR2 Learnings — UI Layer, Lifecycle & Navigation

What we built on top of the data layer, and the Android/Kotlin concepts behind every decision.

---

## The MVVM Stack — Now Complete

```
User taps button
       ↓
Fragment (View) ─── calls ──→ ViewModel ─── calls ──→ Repository ─── calls ──→ DAO ──→ Room/SQLite
       ↑                           │
       └── observes StateFlow ─────┘
```

PR1 built the right side (DAO → Room). PR2 built the left side (Fragment → ViewModel → Repository). Data flows **down** through function calls and back **up** through reactive StateFlow streams.

---

## Build System (Gradle)

### Three Files, Three Purposes

```
settings.gradle.kts  ← "What modules exist and where to find dependencies"
build.gradle.kts     ← "What plugins are available" (root)
app/build.gradle.kts ← "What THIS module needs" (dependencies, SDK versions)
```

### Key Configuration Concepts

**`compileSdk` vs `minSdk` vs `targetSdk`:**
- `compileSdk = 36` — Which Android API level to compile against. You can USE APIs up to this level.
- `minSdk = 26` — Oldest Android version that can install your app (Android 8.0). Devices below this can't install it.
- `targetSdk = 36` — Which version your app was TESTED against. Android uses this to decide whether to apply backward-compatibility behaviors.

**`viewBinding = true`:**
Generates a type-safe binding class for each XML layout. `FragmentLoginBinding` is auto-generated from `fragment_login.xml`. Each view with an `android:id` becomes a property: `binding.emailInput` instead of `findViewById(R.id.email_input)`. Caught at compile time, not runtime.

**`kotlin-kapt`:**
Kotlin Annotation Processing Tool. Room reads your `@Entity` and `@Dao` annotations at compile time and generates the actual SQL implementation. Without kapt, annotations would do nothing.

**`sourceSets` — Custom Source Directory:**
```kotlin
sourceSets {
    getByName("main") {
        java.srcDirs("src/main/kotlin")
    }
}
```
Tells Gradle our Kotlin files live in `src/main/kotlin` instead of the default `src/main/java`. Cosmetic but clearer for a pure-Kotlin project.

---

## AndroidManifest.xml

Every Android app must declare its components in the manifest:

```xml
<application android:name=".DinkLogApp" ...>
    <activity android:name=".MainActivity" android:exported="true">
        <intent-filter>
            <action android:name="android.intent.action.MAIN" />
            <category android:name="android.intent.category.LAUNCHER" />
        </intent-filter>
    </activity>
</application>
```

- **`android:name=".DinkLogApp"`** — The `.` prefix is shorthand for the full package. Tells Android to use our custom Application subclass.
- **`android:exported="true"`** — Required for Activities that other apps can launch (the home screen launcher is "another app").
- **`MAIN` + `LAUNCHER`** — Makes this Activity the app's entry point and puts an icon in the app drawer.

---

## Application Class (DinkLogApp.kt)

```kotlin
class DinkLogApp : Application() {
    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
}
```

The `Application` object is created BEFORE any Activity. It lives for the entire app lifetime. Perfect for one-time initialization.

**`by lazy`** — Delegated property that creates the database on first access, not at app startup. If no screen needs the DB immediately, it's never created unnecessarily. After first access, the same instance is returned every time.

---

## Activity Lifecycle

```
onCreate ──→ onStart ──→ onResume ──→ [RUNNING]
                                         │
[DESTROYED] ←── onDestroy ←── onStop ←── onPause
```

| Method | When It's Called | What To Do |
|---|---|---|
| `onCreate` | Activity first created | Inflate layout, set up views, restore state |
| `onStart` | Becoming visible | Start animations, register listeners |
| `onResume` | In foreground, interactive | Resume paused operations |
| `onPause` | Losing foreground (dialog, split) | Pause operations, save drafts |
| `onStop` | No longer visible | Release heavy resources |
| `onDestroy` | Being killed permanently | Final cleanup |

**Configuration Changes:** When you rotate the device, Android DESTROYS and RECREATES the Activity. The full cycle runs: `onPause → onStop → onDestroy → onCreate → onStart → onResume`. This is why ViewModels exist — they survive this cycle.

---

## Fragment Lifecycle

Fragments add two extra callbacks:

```
onCreate → onCreateView → onViewCreated → onStart → onResume
    → [VISIBLE]
    → onPause → onStop → onDestroyView → onDestroy
```

**`onCreateView`** — Where you inflate the XML layout into a View object. Return the root View.

**`onDestroyView`** — The view is destroyed but the Fragment object may survive (e.g., on the back stack). This is why we null out `_binding` here — the views are gone, so the binding would point to dead objects.

**Key difference from Activity:** A Fragment's view can be created and destroyed multiple times while the Fragment itself lives on. If you navigate away and then come back, `onCreateView` runs again.

---

## ViewBinding

```kotlin
private var _binding: FragmentLoginBinding? = null
private val binding get() = _binding ?: error("Binding accessed after onDestroyView")
```

**Why nullable `_binding`?** The binding references Views that only exist between `onCreateView` and `onDestroyView`. Outside that window, views don't exist. Making it nullable and setting to null in `onDestroyView` prevents:
1. Memory leaks (binding holds view references → view holds Activity reference)
2. Crashes from accessing dead views

**The non-null `binding` accessor** throws immediately if accessed at the wrong time. This is fail-fast: better to crash with a clear message than silently use stale data.

---

## ViewModel + StateFlow

### The Pattern

```kotlin
class LoginViewModel : ViewModel() {
    private val _state = MutableStateFlow<LoginState>(LoginState.Idle)
    val state: StateFlow<LoginState> = _state
}
```

**`MutableStateFlow` (private)** — The ViewModel can write to this.
**`StateFlow` (public)** — Fragments can only read/observe. This enforces unidirectional data flow: events go IN to the ViewModel, state comes OUT.

### Sealed Class for UI State

```kotlin
sealed class LoginState {
    data object Idle : LoginState()
    data object Loading : LoginState()
    data class Success(val user: User) : LoginState()
    data class Error(val message: String) : LoginState()
}
```

`sealed class` means ALL subclasses are defined here. The compiler knows every possible state, so `when` expressions are exhaustive — if you add a new state later and forget to handle it, the code won't compile.

### viewModelScope

```kotlin
viewModelScope.launch {
    // runs on Main thread by default
    // automatically cancelled when ViewModel is destroyed
}
```

Every ViewModel gets a free CoroutineScope. When the ViewModel dies (user permanently leaves the screen), all running coroutines are cancelled. This prevents:
- Network calls completing after the screen is gone
- Database writes updating state that nobody observes
- Memory leaks from captured references

---

## Repository Layer

### Why Not Call DAOs Directly?

The Repository is the **single source of truth** abstraction. Right now it wraps Room, but it could also:
- Cache results in memory
- Merge local + remote data (after adding Firebase)
- Apply business logic (password hashing, isWin calculation)

ViewModels never know WHERE data comes from — they just ask the Repository.

### `withContext(Dispatchers.IO)`

```kotlin
suspend fun login(email: String, password: String): Result<User> =
    withContext(Dispatchers.IO) { ... }
```

Database operations must not run on the Main (UI) thread — they'd freeze the app. `Dispatchers.IO` switches to a thread pool optimized for blocking I/O (up to 64 threads). The calling coroutine suspends (doesn't block) until the IO work completes.

### `Result<T>` for Error Handling

```kotlin
Result.success(user)
Result.failure(Exception("Incorrect password"))
```

Instead of throwing exceptions (which crash the app if uncaught), we wrap outcomes in `Result`. The ViewModel pattern-matches:
```kotlin
.onSuccess { user -> _state.value = LoginState.Success(user) }
.onFailure { error -> _state.value = LoginState.Error(error.message) }
```

---

## Navigation Component

### nav_graph.xml — The App's Road Map

Each `<fragment>` is a destination. Each `<action>` is an allowed transition:

```xml
<action
    android:id="@+id/action_login_to_history"
    app:destination="@id/historyFragment"
    app:popUpTo="@id/loginFragment"
    app:popUpToInclusive="true" />
```

**`popUpTo` + `popUpToInclusive`** — After login succeeds, we navigate to History AND remove Login from the back stack. Without this, pressing Back from History would go back to Login (bad UX — you're already logged in).

### Bottom Navigation Integration

```kotlin
binding.bottomNav.setupWithNavController(navController)
```

This one-liner connects the bottom nav tabs to the navigation graph. Menu item IDs must match fragment IDs in the nav graph. When user taps a tab, Navigation Component handles the Fragment transaction.

### Dynamic Start Destination

```kotlin
val startDestination = if (prefs.getUserId() != null) {
    R.id.historyFragment
} else {
    R.id.loginFragment
}
```

Returning users skip login entirely. We inflate the nav graph, override the start destination, then set it on the controller.

---

## Observing State in Fragments

```kotlin
viewLifecycleOwner.lifecycleScope.launch {
    viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.state.collect { state -> ... }
    }
}
```

Three layers of lifecycle safety:

1. **`viewLifecycleOwner`** — Tied to the VIEW lifecycle (not the Fragment). Prevents updating views that don't exist.
2. **`lifecycleScope`** — Coroutine scope that auto-cancels when the lifecycle ends.
3. **`repeatOnLifecycle(STARTED)`** — Starts collecting when visible, STOPS when backgrounded. Prevents wasting resources observing state nobody can see.

---

## RecyclerView + ListAdapter + DiffUtil

### The Performance Trifecta

```kotlin
class MatchAdapter : ListAdapter<Match, MatchViewHolder>(MatchDiffCallback) { ... }
```

- **RecyclerView** — Recycles ViewHolder objects as rows scroll off-screen. Only ~10 ViewHolders exist at a time, regardless of list size.
- **ListAdapter** — Accepts a whole list via `submitList()` and calculates changes on a background thread.
- **DiffUtil** — Computes the minimum set of add/remove/move operations. Only changed rows get rebound.

```kotlin
object MatchDiffCallback : DiffUtil.ItemCallback<Match>() {
    override fun areItemsTheSame(old: Match, new: Match) = old.matchId == new.matchId
    override fun areContentsTheSame(old: Match, new: Match) = old == new
}
```

- `areItemsTheSame` — "Is this the same row?" (check ID)
- `areContentsTheSame` — "Did the data change?" (uses `data class` auto-generated `equals()`)

---

## Utility Patterns

### Extension Functions

```kotlin
fun Long.formatMatchDate(): String { ... }
fun String.isValidEmail(): Boolean { ... }
```

Extension functions add methods to existing types without modifying them. `1708300000000L.formatMatchDate()` reads naturally. No utility class needed.

### SharedPreferences for Session

```kotlin
fun getUserId(): Long? {
    val id = prefs.getLong(KEY_USER_ID, NO_USER)
    return if (id == NO_USER) null else id
}
```

SharedPreferences doesn't support nullable types, so we use a sentinel value (`-1L`) and convert to Kotlin-idiomatic `null`. Callers get clean null safety: `prefs.getUserId() ?: navigateToLogin()`.

---

## Kotlin-Rules Compliance

| Rule | How We Applied It |
|---|---|
| **#1 SRP** | Each Fragment handles one screen; each ViewModel manages one screen's state |
| **#2 DRY** | `isValidEmail()` extension reusable across Login and Signup |
| **#3 Function Size** | All functions under 20 lines; extracted helpers like `handleLoginSuccess()` |
| **#4 No Magic Strings** | All strings in `strings.xml`, keys in Constants, tags in companion objects |
| **#5 MVVM** | View observes, ViewModel holds state, Repository handles data, Model is shape only |
| **#6 File Organization** | One class per file, organized by feature (pages/, components/, utils/) |
| **#7 Idioms** | `val`, `sealed class`, `by viewModels()`, `?.let`, `?:` elvis, extension functions |
| **#8 Naming** | PascalCase classes, camelCase functions, SCREAMING_SNAKE constants |
| **#9 Forbidden** | No `!!`, no `runBlocking`, no business logic in XML |

---

## Files Created

```
Build & Config:
├── settings.gradle.kts
├── build.gradle.kts (root)
├── app/build.gradle.kts
├── gradle/wrapper/gradle-wrapper.properties
└── app/src/main/AndroidManifest.xml

Resources:
├── res/values/themes.xml
├── res/values/colors.xml
├── res/values/strings.xml
├── res/menu/bottom_nav_menu.xml
├── res/navigation/nav_graph.xml
├── res/layout/activity_main.xml
├── res/layout/fragment_login.xml
├── res/layout/fragment_signup.xml
├── res/layout/fragment_history.xml
├── res/layout/fragment_add_match.xml
├── res/layout/fragment_stats.xml
└── res/layout/item_match.xml

Kotlin Source:
├── DinkLogApp.kt          ← Application singleton
├── MainActivity.kt        ← Single Activity with lifecycle logging
├── utils/Validation.kt    ← Email regex extension function
├── utils/PreferencesManager.kt  ← Session management
├── utils/DateFormatter.kt       ← Timestamp formatting extensions
├── repositories/UserRepository.kt   ← Auth business logic
├── repositories/MatchRepository.kt  ← Match CRUD + stats
├── components/MatchAdapter.kt       ← RecyclerView adapter with DiffUtil
├── pages/Login.kt + LoginViewModel.kt
├── pages/Signup.kt + SignupViewModel.kt
├── pages/History.kt + HistoryViewModel.kt
├── pages/AddMatch.kt + AddMatchViewModel.kt
└── pages/Stats.kt + StatsViewModel.kt
```

---

## What's Next

The natural next steps are:
1. **GPS auto-location** — `FusedLocationProviderClient` to detect court location
2. **Google Places API** — Convert coordinates to named locations
3. **Logout functionality** — Clear session and navigate to Login
4. **Delete match** — Swipe-to-delete on History list
5. **Testing** — Unit tests for repositories, integration tests for Room, UI tests for navigation
