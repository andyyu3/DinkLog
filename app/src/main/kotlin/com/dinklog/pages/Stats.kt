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
import com.dinklog.databinding.FragmentStatsBinding
import com.dinklog.utils.PreferencesManager
import kotlinx.coroutines.launch

class Stats : Fragment() {

    private var _binding: FragmentStatsBinding? = null
    private val binding get() = _binding ?: error("Binding accessed after onDestroyView")

    private val viewModel: StatsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(TAG, "onCreateView")
        _binding = FragmentStatsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeStats()
        loadData()
    }

    private fun observeStats() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.stats.collect { stats ->
                    if (stats == null) return@collect

                    val hasMatches = stats.totalMatches > 0
                    binding.statsContent.visibility = if (hasMatches) View.VISIBLE else View.GONE
                    binding.emptyState.visibility = if (hasMatches) View.GONE else View.VISIBLE

                    binding.totalMatches.text = stats.totalMatches.toString()
                    binding.wins.text = stats.wins.toString()
                    binding.losses.text = stats.losses.toString()
                    binding.winRate.text = "%.1f%%".format(stats.winRate)
                }
            }
        }
    }

    private fun loadData() {
        val userId = PreferencesManager(requireContext()).getUserId() ?: return
        viewModel.loadStats(userId)
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart")
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume")
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause")
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "onStop")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG, "onDestroyView")
        _binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")
    }

    companion object {
        private const val TAG = "StatsFragment"
    }
}
