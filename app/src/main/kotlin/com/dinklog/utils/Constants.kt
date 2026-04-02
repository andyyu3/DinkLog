package com.dinklog.utils

object DatabaseConstants {
    const val DATABASE_NAME = "dinklog.db"
    const val DATABASE_VERSION = 1
}

object UserColumns {
    const val TABLE_NAME = "users"
    const val ID = "user_id"
    const val EMAIL = "email"
    const val NAME = "name"
    const val PASSWORD_HASH = "password_hash"
    const val CREATED_AT = "created_at"
}

object MatchColumns {
    const val TABLE_NAME = "matches"
    const val ID = "match_id"
    const val USER_ID = "logged_by_user_id"
    const val DATE = "match_date"
    const val PARTNER_NAME = "partner_name"
    const val OPPONENT1_NAME = "opponent1_name"
    const val OPPONENT2_NAME = "opponent2_name"
    const val TEAM_SCORE = "team_score"
    const val OPPONENT_SCORE = "opponent_score"
    const val IS_WIN = "is_win"
    const val LOCATION_NAME = "location_name"
    const val NOTES = "notes"
}

object ValidationConstants {
    const val MIN_PASSWORD_LENGTH = 6
    const val MIN_SCORE = 0
}

object ValidationMessages {
    const val FIELDS_REQUIRED = "Please fill in all fields"
    const val INVALID_EMAIL = "Please enter a valid email"
    const val PASSWORD_TOO_SHORT = "Password must be at least ${ ValidationConstants.MIN_PASSWORD_LENGTH} characters"
    const val PASSWORDS_NO_MATCH = "Passwords do not match"
    const val OPPONENT1_REQUIRED = "Opponent 1 name is required"
    const val SCORES_REQUIRED = "Both scores are required"
    const val SCORES_NOT_NUMBERS = "Scores must be numbers"
    const val SCORES_NEGATIVE = "Scores cannot be negative"
    const val LOCATION_REQUIRED = "Location is required"
    const val LOGIN_FAILED = "Login failed"
    const val SIGNUP_FAILED = "Signup failed"
    const val SAVE_FAILED = "Save failed"
    const val UPDATE_FAILED = "Update failed"
    const val NO_ACCOUNT = "No account found with that email"
    const val INCORRECT_PASSWORD = "Incorrect password"
    const val EMAIL_IN_USE = "Email already in use"
}

object CryptoConstants {
    const val ALGORITHM = "PBKDF2WithHmacSHA256"
    const val ITERATIONS = 120_000
    const val KEY_LENGTH = 256
    const val SALT_LENGTH = 16
    const val SEPARATOR = ":"
}

object DateMessages {
    const val TODAY = "Today"
    const val YESTERDAY = "Yesterday"
}

object LocationMessages {
    const val PERMISSION_DENIED = "Location access denied — please enter location manually"
    const val UNAVAILABLE = "Could not detect location — please enter manually"
}
