# PR1 Learnings — Data Layer Implementation

What we built, how it works, and the Kotlin/Android concepts behind every decision.

---

## Why Start with the Data Layer?

In MVVM architecture, data flows **upward**:

```
Entities → DAOs → Repository → ViewModel → View
```

Everything depends on the shape of your data. Getting entities and DAOs right first prevents cascading refactors later. Room also validates your SQL at **compile time**, so schema errors surface immediately when you build — not at runtime.

---

## Kotlin Fundamentals Used

### `object` — The Singleton Keyword

```kotlin
object UserColumns {
    const val TABLE_NAME = "users"
    const val EMAIL = "email"
}
```

`object` creates a **singleton** — exactly one instance, ever. It's like a `static class` in Java. We use it for constants because they don't need multiple instances.

### `const val` — Compile-Time Constants

`const val` means the value is known at **compile time** (not runtime). It can only be a `String` or primitive type. The compiler **inlines** the value everywhere it's used, making it more efficient than a regular `val`. We use these for all column names, table names, and database configuration.

### `data class` — Auto-Generated Boilerplate

```kotlin
data class User(
    val userId: Long = 0,
    val email: String,
    val name: String
)
```

A regular `class` is just a container. A `data class` automatically generates: `equals()`, `hashCode()`, `toString()`, and `copy()`. Our kotlin-rules say *"Prefer data class for models; never add logic to them."* In MVVM, the Model layer is **data shape only** — no business logic allowed.

### `val` vs `var` — Immutability by Default

- `val` = immutable (like `final` in Java). Once assigned, it cannot change.
- `var` = mutable, can be reassigned.

Our kotlin-rules say *"Use val by default; use var only when mutation is genuinely required."* Every field in our entities is `val`.

### Default Parameter Values

```kotlin
data class User(
    val userId: Long = 0,           // Room auto-generates this
    val email: String,               // required — no default
    val createdAt: Long = System.currentTimeMillis()  // defaults to "now"
)
```

Kotlin lets you give parameters defaults. When creating a `User`, you only need to provide `email`, `name`, and `passwordHash`. The `userId` defaults to 0 (Room auto-generates it), and `createdAt` defaults to the current time.

### Nullable Types — `String?` vs `String`

```kotlin
val partnerName: String? = null    // CAN be null (singles matches have no partner)
val opponent1Name: String          // can NEVER be null
```

The `?` suffix means a type is nullable. Kotlin enforces this at **compile time**, unlike Java where anything could be null. This eliminates an entire class of `NullPointerException` bugs.

### The Colon `:` in Kotlin

The colon has two meanings depending on context:

- **After a variable/parameter:** means "has type" → `email: String` means "email is a String"
- **After a function name:** means "returns type" → `fun findByEmail(): User?` means "returns a nullable User"

### String Templates — `${expression}`

```kotlin
@Query("SELECT * FROM ${UserColumns.TABLE_NAME} WHERE ${UserColumns.EMAIL} = :email")
```

The `${}` syntax lets you embed Kotlin expressions inside strings. This is how we reference our named constants inside SQL queries instead of hardcoding magic strings.

### Expression Body Functions — `= ...`

```kotlin
// Block body (multi-statement)
fun getInstance(context: Context): AppDatabase {
    return instance ?: synchronized(this) { ... }
}

// Expression body (single expression, cleaner)
fun getInstance(context: Context): AppDatabase =
    instance ?: synchronized(this) { ... }
```

Our kotlin-rules say *"Prefer expression bodies for single-expression functions."* No `return` keyword needed — the result of the expression IS the return value.

### `.also { }` — Scope Function

```kotlin
buildDatabase(context).also { instance = it }
```

`also` runs a block of code and returns the **original object**. Here it:
1. Builds the database
2. Saves it to `instance` (side effect)
3. Returns the database object

`it` is the implicit parameter name for single-parameter lambdas in Kotlin.

---

## Room Database Concepts

### `@Entity` — Maps a Class to a Table

```kotlin
@Entity(
    tableName = UserColumns.TABLE_NAME,
    indices = [Index(value = [UserColumns.EMAIL], unique = true)]
)
data class User( ... )
```

`@Entity` tells Room "this class maps to a database table." Each property becomes a column. The `indices` parameter creates database indexes for faster lookups. `unique = true` enforces that no two users can share the same email.

### `@PrimaryKey(autoGenerate = true)` — Auto-Incrementing IDs

Room auto-increments the ID column. When inserting, you pass `0` as the ID and Room assigns the next available number.

### `@ColumnInfo(name = "...")` — Column Naming

Maps a Kotlin property name (camelCase) to a specific SQL column name (snake_case). Without this, Room would use the property name directly.

### `@ForeignKey` — Enforcing Relationships

```kotlin
foreignKeys = [
    ForeignKey(
        entity = User::class,
        parentColumns = [UserColumns.ID],
        childColumns = [MatchColumns.USER_ID],
        onDelete = ForeignKey.CASCADE
    )
]
```

A foreign key enforces a **relationship** between tables. Every match must belong to a valid user. `CASCADE` means: if you delete a User, all their Matches get auto-deleted too, preventing orphaned records.

### Why Index Foreign Key Columns

```kotlin
indices = [Index(value = [MatchColumns.USER_ID])]
```

Without an index, querying "all matches for user X" would scan the **entire** matches table. With an index, it's a fast lookup. Room warns you at compile time if you forget to index a foreign key column.

### `@Dao` — Data Access Object

```kotlin
@Dao
interface UserDao {
    @Insert
    suspend fun insert(user: User): Long

    @Query("SELECT * FROM ...")
    suspend fun findByEmail(email: String): User?
}
```

A DAO is an `interface` (not a class) where you declare **what** database operations you need. Room generates the actual SQL implementation at compile time. You never write the implementation yourself.

- `:email` in SQL is a **Room parameter binding** — Room maps it to the function parameter, preventing SQL injection automatically.
- `insert()` returns `Long` — the auto-generated primary key of the new row.

### `suspend` — Coroutine Functions

```kotlin
suspend fun insert(user: User): Long
```

`suspend` marks a function as a **coroutine** — it can pause without blocking the thread. Database operations are slow (disk I/O). If they ran on the main (UI) thread, the app would freeze. `suspend` forces callers to run these from a coroutine, ensuring they execute on a background thread.

Think of it like `async` in JavaScript — you can't call a `suspend` function from regular code; you need to be inside a coroutine scope.

### `Flow<List<Match>>` — Reactive Data Streams

```kotlin
fun getMatchesByUser(userId: Long): Flow<List<Match>>  // no suspend!
```

`Flow` is Kotlin's reactive data stream. When the database changes, Flow **automatically emits the new data** to anything observing it.

Compare:
- **`suspend fun`** = "give me the data **once**"
- **`fun` returning `Flow`** = "give me the data now **AND every time it changes**"

`getMatchesByUser()` returns `Flow` because the History screen needs to live-update when you add a new match. The stats queries (`getWinCount`, etc.) return plain values because they're one-shot calculations.

Note: Flow-returning functions are NOT `suspend` — Room handles the threading internally.

### SQL Aggregation for Stats

```kotlin
@Query("SELECT COUNT(*) FROM matches WHERE logged_by_user_id = :userId AND is_win = 1")
suspend fun getWinCount(userId: Long): Int
```

Instead of loading every match into memory and counting in Kotlin, the database does the math with `COUNT(*)`. This is far more efficient — imagine a user with 1000 matches; you'd rather have SQLite count them than load 1000 objects into RAM.

---

## The Singleton Pattern (AppDatabase)

```kotlin
companion object {
    @Volatile
    private var instance: AppDatabase? = null

    fun getInstance(context: Context): AppDatabase =
        instance ?: synchronized(this) {
            instance ?: buildDatabase(context).also { instance = it }
        }
}
```

You only ever want **one** database connection open. Multiple connections can cause data corruption.

- **`@Volatile`** ensures all threads see the latest value of `instance` (prevents stale CPU cache reads).
- **`synchronized(this)`** ensures only one thread can create the instance at a time.
- **Double-checked locking:** first check `instance` without locking (fast path for subsequent calls), then lock and check again (safe path for first creation).

### `abstract class` — Room Generates the Implementation

```kotlin
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun matchDao(): MatchDao
}
```

An `abstract class` can't be instantiated directly. Room generates a concrete subclass at compile time that implements these abstract functions. You declare "I need a UserDao" and Room provides one.

---

## Kotlin-Rules Compliance

Every decision was driven by the project's `kotlin-rules.md`:

| Rule | How We Applied It |
|---|---|
| **#1 SRP** — One responsibility per class | Each file has exactly one class or interface |
| **#2 DRY** — No duplicates | All column names defined once in `Constants.kt` |
| **#3 Function Size** — Max 20 lines | Longest function is 4 lines |
| **#4 No Magic Strings** | Every string literal is a constant reference |
| **#5 MVVM** — Model = data shape only | Entities are pure `data class` with no logic |
| **#6 File Organization** — One class per file | 6 files, 6 top-level declarations |
| **#7 Idioms** — `val`, `data class`, no `!!` | All fields immutable, no force-unwraps |
| **#8 Naming** — PascalCase/camelCase/SCREAMING_SNAKE | Followed throughout |
| **#9 Forbidden** — No `lateinit var`, no `runBlocking` | None used anywhere |

---

## Files Created

```
utils/
└── Constants.kt        ← Named constants for all DB/column names (Rule #4)

models/
├── User.kt             ← User entity — shape of a user row
├── UserDao.kt          ← User database operations interface
├── Match.kt            ← Match entity — shape of a match row, FK to User
├── MatchDao.kt         ← Match database operations + stats queries
└── AppDatabase.kt      ← Room database singleton tying it all together
```

---

## Required build.gradle Dependencies

```groovy
plugins {
    id 'kotlin-kapt'  // Room annotation processor
}

dependencies {
    // Room
    implementation "androidx.room:room-runtime:2.6.1"
    kapt "androidx.room:room-compiler:2.6.1"
    implementation "androidx.room:room-ktx:2.6.1"

    // Coroutines
    implementation "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3"
}
```

---

## What's Next

The natural next step is the **Repository layer** (`UserRepository.kt`, `MatchRepository.kt`), which wraps the DAOs and adds business logic like password hashing, input validation, and error handling. After that: ViewModels, then the UI fragments.
