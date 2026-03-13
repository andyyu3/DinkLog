package com.dinklog

import android.app.Application
import android.util.Log
import com.dinklog.models.AppDatabase

class DinkLogApp : Application() {

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }

    override fun onCreate() {
        super.onCreate()
        instance = this
        Log.d(TAG, "Application created — database ready")
    }

    companion object {
        private const val TAG = "DinkLogApp"
        lateinit var instance: DinkLogApp
            private set

        fun getDatabase(): AppDatabase = instance.database
    }
}
