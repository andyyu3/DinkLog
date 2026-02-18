# Kotlin Rules — DinkLog

Enforced on every file. No exceptions without a documented reason in the PR.

---

## 1. SOLID + SRP

- **One responsibility per class.** If you need "and" to describe a class, split it.
- **Open/Closed:** Extend behavior via interfaces and composition, not by modifying existing classes.
- **Liskov:** Every subtype must be substitutable for its parent without changing program correctness.
- **Interface Segregation:** Prefer narrow, role-specific interfaces over fat ones.
- **Dependency Inversion:** Classes depend on abstractions (`interface`), never on concrete implementations.

---

## 2. DRY

- Extract any logic used more than once into a named function or extension.
- Never duplicate string keys, route names, or field names — define them once in a constants file.
- Shared UI behavior belongs in a base class or extension function, not copy-pasted across fragments/activities.

---

## 3. Function Size

- **Hard limit: 20 lines per function.** If it grows beyond that, extract a helper with a descriptive name.
- Every function does exactly **one** thing. The name must fully describe it without reading the body.
- Avoid boolean parameters that change function behavior — split into two functions instead.
- Prefer expression bodies (`= ...`) for single-expression functions.

```kotlin
// Bad
fun process(data: List<Match>, format: Boolean) { ... }

// Good
fun getRawMatches(): List<Match> { ... }
fun getFormattedMatches(): List<String> { ... }
```

---

## 4. No Magic Strings or Numbers

- All constants live in a companion object or top-level `object` in `utils/Constants.kt`.
- String resource keys, SharedPreferences keys, Room column names, and route names are named constants.
- Use `enum class` or `sealed class` instead of raw string/int flags.

```kotlin
// Bad
prefs.getString("user_id", null)

// Good
prefs.getString(PrefsKeys.USER_ID, null)
```

---

## 5. MVVM Architecture

**Layer responsibilities — never cross them:**

| Layer | Allowed | Forbidden |
|---|---|---|
| **View** (Activity/Fragment) | Observe state, call ViewModel | Business logic, direct DB/network access |
| **ViewModel** | Hold UI state, call Repository | `Context`, `View`, Android framework imports |
| **Repository** | Data access, mapping | UI state, LiveData/StateFlow emission |
| **Model** | Data shape only | Logic, formatting, Android imports |

- ViewModels expose state as `StateFlow<UiState>` or `LiveData<T>` — never raw mutable fields.
- Use `UiState` sealed classes to represent loading / success / error.
- One ViewModel per screen. Never share a ViewModel across unrelated screens.

```kotlin
sealed class UiState<out T> {
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}
```

---

## 6. File Organization

```
com.dinklog/
├── components/      # Reusable adapters, custom views
├── models/          # Data classes, Room entities, enums
├── pages/           # One folder per screen: Screen.kt + ScreenViewModel.kt
├── repositories/    # One repository per domain entity
├── utils/           # Constants.kt, Extensions.kt, DateFormatter.kt
└── DinkLogApp.kt    # Application class only
```

- File name matches the single top-level class/object it contains.
- No "Utils" catch-all classes — use focused extension files instead.

---

## 7. Kotlin Idioms

- Prefer `data class` for models; never add logic to them.
- Use `val` by default; use `var` only when mutation is genuinely required.
- Use `when` expressions exhaustively over chains of `if/else`.
- Prefer `?.let`, `?:`, and `run`/`apply`/`also` over null checks — but only when it improves readability.
- Never use `!!` — use `?: return`, `?: throw`, or `requireNotNull()` with a message.
- Coroutines: use `viewModelScope` in ViewModels, `Dispatchers.IO` in repositories.

---

## 8. Naming

| Element | Convention | Example |
|---|---|---|
| Class | `PascalCase` | `MatchRepository` |
| Function / variable | `camelCase` | `fetchMatchHistory()` |
| Constant | `SCREAMING_SNAKE` | `MAX_SCORE` |
| Layout resource | `snake_case` prefixed by type | `fragment_history` |
| String resource | `snake_case` | `label_add_match` |

---

## 9. What Is Always Forbidden

- `lateinit var` on a non-injected field (use nullable + `?:` instead)
- `runBlocking` outside of tests
- Business logic in XML click handlers (`android:onClick`)
- Committing `TODO` comments without a linked issue number
