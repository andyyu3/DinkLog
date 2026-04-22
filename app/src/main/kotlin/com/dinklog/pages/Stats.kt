package com.dinklog.pages

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.dinklog.DinkLogApp
import com.dinklog.R
import com.dinklog.databinding.FragmentStatsBinding
import com.dinklog.repositories.MatchStats
import com.dinklog.utils.PreferencesManager
import kotlinx.coroutines.launch

class Stats : Fragment() {

    private var _binding: FragmentStatsBinding? = null
    private val binding get() = _binding ?: error("Binding accessed after onDestroyView")

    private val viewModel: StatsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(LOG_TAG, "Stats: onCreate")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(LOG_TAG, "Stats: onCreateView")
        _binding = FragmentStatsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(LOG_TAG, "Stats: onViewCreated")
        setupToolbar()
        observeStats()
        loadData()
    }

    private fun setupToolbar() {
        binding.toolbar.inflateMenu(R.menu.toolbar_menu)
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_logout -> { logout(); true }
                else -> false
            }
        }
    }

    private fun logout() {
        PreferencesManager(DinkLogApp.instance).clearSession()
        findNavController().navigate(R.id.action_global_logout)
    }

    private fun observeStats() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.stats.collect { stats ->
                    stats?.let { renderStats(it) }
                }
            }
        }
    }

    private fun renderStats(stats: MatchStats) {
        val hasMatches = stats.totalMatches > 0
        binding.statsContent.visibility = if (hasMatches) View.VISIBLE else View.GONE
        binding.emptyState.visibility = if (hasMatches) View.GONE else View.VISIBLE
        binding.totalMatches.text = stats.totalMatches.toString()
        binding.wins.text = stats.wins.toString()
        binding.losses.text = stats.losses.toString()
        val winRateText = "%.1f".format(stats.winRate)
        binding.winRate.text = "$winRateText%"
        applyStatsAccessibility(stats, winRateText)
    }

    private fun applyStatsAccessibility(stats: MatchStats, winRateText: String) {
        binding.winRateCard.contentDescription =
            getString(R.string.cd_stats_win_rate, winRateText)
        binding.totalMatchesCard.contentDescription =
            getString(R.string.cd_stats_total_matches, stats.totalMatches)
        binding.winsCard.contentDescription =
            getString(R.string.cd_stats_wins, stats.wins)
        binding.lossesCard.contentDescription =
            getString(R.string.cd_stats_losses, stats.losses)
    }

    private fun loadData() {
        viewModel.loadStats()
    }

    override fun onStart() {
        super.onStart()
        Log.d(LOG_TAG, "Stats: onStart")
    }

    override fun onResume() {
        super.onResume()
        Log.d(LOG_TAG, "Stats: onResume")
    }

    override fun onPause() {
        super.onPause()
        Log.d(LOG_TAG, "Stats: onPause")
    }

    override fun onStop() {
        super.onStop()
        Log.d(LOG_TAG, "Stats: onStop")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(LOG_TAG, "Stats: onDestroyView")
        _binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(LOG_TAG, "Stats: onDestroy")
    }

    companion object {
        private const val LOG_TAG = "DinkLog"
    }
}
