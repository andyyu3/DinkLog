package com.dinklog

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.dinklog.databinding.ActivityMainBinding
import com.dinklog.utils.PreferencesManager

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(LOG_TAG, "MainActivity: onCreate")

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNavigation()
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        val prefs = PreferencesManager(this)
        val startDestination = if (prefs.getUserId() != null) {
            R.id.historyFragment
        } else {
            R.id.loginFragment
        }

        val navGraph = navController.navInflater.inflate(R.navigation.nav_graph)
        navGraph.setStartDestination(startDestination)
        navController.graph = navGraph

        binding.bottomNav.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            val showBottomNav = destination.id in setOf(
                R.id.historyFragment,
                R.id.statsFragment
            )
            binding.bottomNav.visibility = if (showBottomNav) View.VISIBLE else View.GONE
        }

        // Ensure initial bottom navigation visibility matches the current destination
        navController.currentDestination?.let { destination ->
            val showBottomNav = destination.id in setOf(
                R.id.historyFragment,
                R.id.statsFragment
            )
            binding.bottomNav.visibility = if (showBottomNav) View.VISIBLE else View.GONE
        }
    }

    override fun onStart() {
        super.onStart()
        Log.d(LOG_TAG, "MainActivity: onStart")
    }

    override fun onResume() {
        super.onResume()
        Log.d(LOG_TAG, "MainActivity: onResume")
    }

    override fun onPause() {
        super.onPause()
        Log.d(LOG_TAG, "MainActivity: onPause")
    }

    override fun onStop() {
        super.onStop()
        Log.d(LOG_TAG, "MainActivity: onStop")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(LOG_TAG, "MainActivity: onDestroy")
    }

    companion object {
        private const val LOG_TAG = "DinkLog"
    }
}
