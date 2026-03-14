# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

**DinkLog** — An Android pickleball match-tracking app built with Kotlin for CSE 5236 at Ohio State University. Package: `com.dinklog`. Team project by Michael Gorman, Ryan Liu, and Andy Yu.

Project tracker: https://www.notion.so/CSE-5236-Mobile-Apps-2fb6d1dd02a38005bd7eff4a98ca77a7

## Build & Run

Standard Android Gradle project. Kotlin 1.9.22, JDK 17, targeting API 36, minimum API 26. Uses `kotlin-kapt` for Room annotation processing.

```bash
./gradlew assembleDebug          # Build debug APK
./gradlew installDebug           # Install on connected device/emulator
./gradlew test                   # Run all unit tests
./gradlew test --tests "com.dinklog.SomeTest"  # Run a single test class
./gradlew clean                  # Clean build
```

Entry point is `MainActivity`, which starts at `loginFragment` unless a saved session exists in `PreferencesManager`, in which case it starts at `historyFragment`.

**Note:** No unit tests exist yet — the `test` directory is empty.

## Architecture

Strict MVVM with unidirectional data flow:

```
Fragment (View) --> ViewModel --> Repository --> DAO --> Room/SQLite
       ^                |
       +-- StateFlow ---+
```

**Layer rules** (enforced by `app/kotlin-rules.md`):
- **Fragment/Activity** — observe state, call ViewModel methods, no business logic
- **ViewModel** — holds `StateFlow<UiState>` / `LiveData`, calls Repository, no `Context` or Android framework imports
- **Repository** — all business logic (password hashing, `isWin` calculation, validation), uses `Dispatchers.IO`, returns `Result<T>`
- **Model** — pure `data class`, no logic, no Android imports

State is exposed as sealed classes (e.g., `LoginState` with `Idle / Loading / Success / Error`). Fragments observe with `repeatOnLifecycle(Lifecycle.State.STARTED)`.

## Key Source Locations

```
app/src/main/kotlin/com/dinklog/
+-- DinkLogApp.kt                  # Application class; lazy-initializes AppDatabase
+-- MainActivity.kt                # Single-Activity host; NavController + BottomNav
+-- models/                        # Room entities, DAOs, AppDatabase singleton
+-- repositories/                  # UserRepository (auth/hashing), MatchRepository (CRUD + stats)
+-- pages/                         # One Fragment + ViewModel per screen
+-- components/
|   +-- MatchAdapter.kt            # ListAdapter + DiffUtil for History RecyclerView
|   +-- SwipeToDeleteCallback.kt   # ItemTouchHelper for swipe-to-delete on matches
+-- utils/
    +-- Constants.kt               # All DB/column/prefs key constants (single source of truth)
    +-- PreferencesManager.kt      # SharedPreferences session wrapper (userId persistence)
    +-- DateFormatter.kt           # Long.formatMatchDate(), Long.formatRelativeTime()
    +-- Validation.kt              # String.isValidEmail() extension
```

Navigation graph: `app/src/main/res/navigation/nav_graph.xml`
Flows: Login/Signup -> History (auth removes login from back stack); History -> AddMatch; bottom nav between History and Stats.

## Coding Rules (from `app/kotlin-rules.md`)

- **Function size hard limit: 20 lines.** Extract helpers with descriptive names.
- **No magic strings or numbers.** All constants in `utils/Constants.kt` companion/top-level objects.
- **No `!!` operator.** Use `?: return`, `?: throw`, or `requireNotNull()`.
- **No `runBlocking`** outside of tests.
- **No `lateinit var`** on non-injected fields.
- **No business logic in XML** (`android:onClick`).
- Use `viewModelScope` in ViewModels, `Dispatchers.IO` in Repositories.
- Coroutines: DAO functions returning a single value are `suspend`; those returning live data use `Flow` (no `suspend`).
- `TODO` comments require a linked issue number.
- Prefer `val` over `var`; use `when` exhaustively over `if/else` chains.
- Prefer expression bodies (`= ...`) for single-expression functions.

## Room Database

- `AppDatabase` is a double-checked locking singleton (`@Volatile` + `synchronized`), accessed via `AppDatabase.getInstance(context)` and exposed through `DinkLogApp.database`.
- Schema: `users` table (unique email index) + `matches` table (FK to users with CASCADE delete, indexed on `logged_by_user_id`).
- Current `DATABASE_VERSION = 1` in `Constants.kt`. Incrementing it requires adding a migration — `fallbackToDestructiveMigration()` is not used.
- Constants for all table/column names live in `DatabaseConstants`, `UserColumns`, and `MatchColumns` objects in `Constants.kt`.

## Session Management

`PreferencesManager` wraps SharedPreferences for user session. `getUserId()` returns `Long?` (null = not logged in). `MainActivity` checks this at startup to set the nav graph's start destination dynamically.
