# DinkLog

A pickleball match tracking app for Android, built with Kotlin for CSE 5236 (Mobile Application Development) at Ohio State University.

https://www.notion.so/CSE-5236-Mobile-Apps-2fb6d1dd02a38005bd7eff4a98ca77a7

## Team

- **Michael Gorman** — gorman.270@osu.edu
- **Ryan Liu** — liu.10260@osu.edu
- **Andy Yu** — yu.3428@osu.edu

## Features

- User registration and login with local Room database
- Log pickleball matches with opponents, scores, partner, location, and notes
- Match history with win/loss indicators
- Aggregate stats: total matches, wins, losses, win rate
- Session persistence across app restarts
- Full Activity and Fragment lifecycle logging for checkpoint demo

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| Architecture | MVVM (Fragment → ViewModel → Repository → DAO) |
| Database | Room (SQLite) |
| Navigation | Jetpack Navigation Component |
| UI | Material Design 3, ViewBinding, RecyclerView + DiffUtil |
| Async | Kotlin Coroutines + StateFlow |

## Prerequisites

- **Android Studio** Hedgehog (2023.1.1) or newer
- **JDK 17** (bundled with Android Studio)
- **Android SDK 36** (install via SDK Manager)
- A physical Android device (API 26+) or an emulator

## How to Run

1. **Clone the repository**
   ```bash
   git clone https://github.com/rbl24/cse5236.git
   cd cse5236
   ```

2. **Open in Android Studio**
   - File → Open → select the `cse5236` root folder
   - Wait for Gradle sync to complete (Android Studio will prompt you)

3. **Select a run target**
   - Connect a physical device via USB with USB Debugging enabled, **or**
   - Create an emulator via Tools → Device Manager (any device API 26+)

4. **Build and Run**
   - Click the green **Run** button (▶) or press `Shift+F10`
   - The app will install and launch on the selected device
   - You should see the Login screen on first launch

5. **Create an account**
   - Tap "Don't have an account? Sign Up"
   - Fill in email, name, and password
   - After signup you'll be taken to the History screen

## Project Structure

```
app/src/main/kotlin/com/dinklog/
├── DinkLogApp.kt                 # Application singleton, database init
├── MainActivity.kt               # Single Activity host with lifecycle logging
├── models/
│   ├── User.kt                   # User Room entity
│   ├── Match.kt                  # Match Room entity
│   ├── UserDao.kt                # User data access queries
│   ├── MatchDao.kt               # Match data access queries
│   └── AppDatabase.kt            # Room database singleton
├── repositories/
│   ├── UserRepository.kt         # Auth business logic (login, signup, hashing)
│   └── MatchRepository.kt        # Match CRUD + aggregate stats
├── pages/
│   ├── Login.kt / LoginViewModel.kt
│   ├── Signup.kt / SignupViewModel.kt
│   ├── History.kt / HistoryViewModel.kt
│   ├── AddMatch.kt / AddMatchViewModel.kt
│   └── Stats.kt / StatsViewModel.kt
├── components/
│   └── MatchAdapter.kt           # RecyclerView adapter with DiffUtil
└── utils/
    ├── Constants.kt              # Database table/column name constants
    ├── Validation.kt             # Email validation extension
    ├── DateFormatter.kt          # Timestamp formatting extensions
    └── PreferencesManager.kt     # SharedPreferences session wrapper
```
