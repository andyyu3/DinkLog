# DinkLog — Room Database Schema

**Database name:** `dinklog.db`
**Version:** 1
**Singleton pattern:** Double-checked locking (`@Volatile` + `synchronized`) in `AppDatabase.getInstance(context)`

---

## Entity-Relationship Diagram

```
┌──────────────┐        1 : N        ┌──────────────┐
│    users     │◄─────────────────────│   matches    │
│              │  ON DELETE CASCADE   │              │
│  PK user_id  │                      │  PK match_id │
│              │                      │  FK user_id ──┘
└──────────────┘                      └──────────────┘
```

---

## Tables

### `users`

| Column | Type | Constraints | Notes |
|---|---|---|---|
| `user_id` | INTEGER | PRIMARY KEY, auto-increment | Room `autoGenerate = true` |
| `email` | TEXT | NOT NULL | Covered by a UNIQUE index |
| `name` | TEXT | NOT NULL | |
| `password_hash` | TEXT | NOT NULL | PBKDF2-HMAC-SHA256 with random salt |
| `created_at` | INTEGER | NOT NULL | Epoch millis, defaults to `System.currentTimeMillis()` |

**Indices:**

| Index | Column(s) | Unique |
|---|---|---|
| `index_users_email` | `email` | Yes |

### `matches`

| Column | Type | Constraints | Notes |
|---|---|---|---|
| `match_id` | INTEGER | PRIMARY KEY, auto-increment | Room `autoGenerate = true` |
| `logged_by_user_id` | INTEGER | NOT NULL, FOREIGN KEY → `users(user_id)` | ON DELETE CASCADE |
| `match_date` | INTEGER | NOT NULL | Epoch millis, defaults to `System.currentTimeMillis()` |
| `partner_name` | TEXT | Nullable | Optional doubles partner |
| `opponent1_name` | TEXT | NOT NULL | |
| `opponent2_name` | TEXT | Nullable | Optional second opponent (doubles) |
| `team_score` | INTEGER | NOT NULL | |
| `opponent_score` | INTEGER | NOT NULL | |
| `is_win` | INTEGER | NOT NULL | Boolean stored as 0/1; computed as `team_score > opponent_score` |
| `location_name` | TEXT | NOT NULL | |
| `notes` | TEXT | Nullable | |

**Indices:**

| Index | Column(s) | Unique |
|---|---|---|
| `index_matches_logged_by_user_id` | `logged_by_user_id` | No |

---

## DAO Query Methods

### UserDao (`models/UserDao.kt`)

#### Create

| Method | Annotation | Signature | Description |
|---|---|---|---|
| `insert` | `@Insert` | `suspend fun insert(user: User): Long` | Inserts a new user row; returns the auto-generated `user_id` |

#### Read

| Method | Annotation | Signature | Description |
|---|---|---|---|
| `findByEmail` | `@Query` | `suspend fun findByEmail(email: String): User?` | Looks up a single user by email; returns null if not found |
| `findById` | `@Query` | `suspend fun findById(userId: Long): User?` | Looks up a single user by primary key |
| `emailExists` | `@Query` | `suspend fun emailExists(email: String): Boolean` | Returns true if any row has the given email (`COUNT(*) > 0`) |

#### Update

No update methods defined.

#### Delete

No delete methods defined.

---

### MatchDao (`models/MatchDao.kt`)

#### Create

| Method | Annotation | Signature | Description |
|---|---|---|---|
| `insert` | `@Insert` | `suspend fun insert(match: Match): Long` | Inserts a new match row; returns the auto-generated `match_id` |

#### Read

| Method | Annotation | Signature | Description |
|---|---|---|---|
| `getMatchesByUser` | `@Query` | `fun getMatchesByUser(userId: Long): Flow<List<Match>>` | Returns all matches for a user ordered by `match_date DESC`. Returns a `Flow` (not `suspend`) so the UI receives live updates when data changes |
| `getMatchById` | `@Query` | `suspend fun getMatchById(matchId: Long): Match?` | Looks up a single match by primary key; used to pre-populate the edit form |
| `getTotalMatches` | `@Query` | `suspend fun getTotalMatches(userId: Long): Int` | `COUNT(*)` of all matches for a user |
| `getWinCount` | `@Query` | `suspend fun getWinCount(userId: Long): Int` | `COUNT(*)` where `is_win = 1` |
| `getLossCount` | `@Query` | `suspend fun getLossCount(userId: Long): Int` | `COUNT(*)` where `is_win = 0` |

#### Update

| Method | Annotation | Signature | Description |
|---|---|---|---|
| `update` | `@Update` | `suspend fun update(match: Match)` | Updates an existing match row matched by primary key |

#### Delete

| Method | Annotation | Signature | Description |
|---|---|---|---|
| `delete` | `@Delete` | `suspend fun delete(match: Match)` | Deletes a match row matched by primary key |

---

## CASCADE Delete Behavior

The `matches` table declares a foreign key on `logged_by_user_id` referencing `users(user_id)` with `onDelete = ForeignKey.CASCADE`.

This means that when a row is deleted from the `users` table, SQLite automatically deletes every row in `matches` whose `logged_by_user_id` equals the deleted user's `user_id`. The application does not need to manually delete a user's matches before deleting the user — the database engine handles it atomically.

This guarantees referential integrity: there can never be orphaned match rows pointing to a non-existent user. It also simplifies a future "delete account" feature — a single `DELETE FROM users WHERE user_id = ?` will cleanly remove the user and all of their match history in one operation.
