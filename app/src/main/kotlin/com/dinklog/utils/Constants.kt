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
