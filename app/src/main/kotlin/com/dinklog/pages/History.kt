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
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import android.widget.Toast
import com.dinklog.BuildConfig
import com.dinklog.DinkLogApp
import com.dinklog.R
import com.dinklog.components.MatchAdapter
import com.dinklog.databinding.FragmentHistoryBinding
import com.dinklog.components.SwipeToDeleteCallback
import com.dinklog.models.Match
import com.dinklog.utils.PreferencesManager
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class History : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding ?: error("Binding accessed after onDestroyView")

    private val viewModel: HistoryViewModel by viewModels()
    // ADDED: pass click handler to navigate to AddMatch in edit mode
    private val matchAdapter = MatchAdapter(onMatchClick = { matchId ->
        findNavController().navigate(
            R.id.action_history_to_addMatch,
            bundleOf("matchId" to matchId)
        )
    })

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(LOG_TAG, "History: onCreate")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(LOG_TAG, "History: onCreateView")
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(LOG_TAG, "History: onViewCreated")
        setupToolbar()
        setupRecyclerView()
        setupSwipeToDelete()
        setupClickListeners()
        observeMatches()
        loadData()
    }

    private fun setupToolbar() {
        binding.toolbar.inflateMenu(R.menu.toolbar_menu)
        binding.toolbar.menu.findItem(R.id.action_seed_debug).isVisible = BuildConfig.DEBUG
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_logout -> { logout(); true }
                R.id.action_seed_debug -> { seedDebug(); true }
                else -> false
            }
        }
    }

    private fun seedDebug() {
        viewModel.seedDebugMatches { count ->
            val msg = getString(R.string.toast_seed_complete, count)
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
        }
    }

    private fun logout() {
        PreferencesManager(DinkLogApp.instance).clearSession()
        findNavController().navigate(R.id.action_global_logout)
    }

    private fun setupRecyclerView() {
        binding.matchesList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = matchAdapter
        }
    }

    private fun setupSwipeToDelete() {
        val callback = SwipeToDeleteCallback(requireContext()) { position ->
            val match = matchAdapter.peek(position) ?: return@SwipeToDeleteCallback
            viewModel.deleteMatch(match)
            showUndoSnackbar(match)
        }
        ItemTouchHelper(callback).attachToRecyclerView(binding.matchesList)
    }

    private fun showUndoSnackbar(deletedMatch: Match) {
        Snackbar.make(binding.root, R.string.match_deleted, Snackbar.LENGTH_LONG)
            .setAction(R.string.action_undo) { viewModel.undoDelete(deletedMatch) }
            .show()
    }

    private fun setupClickListeners() {
        binding.fabAddMatch.setOnClickListener {
            findNavController().navigate(R.id.action_history_to_addMatch)
        }
    }

    private fun observeMatches() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.matches.collect { pagingData ->
                    matchAdapter.submitData(pagingData)
                }
            }
        }
        matchAdapter.addLoadStateListener { loadStates ->
            val refreshDone = loadStates.refresh is LoadState.NotLoading
            val isEmpty = refreshDone && matchAdapter.itemCount == 0
            binding.emptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
            binding.matchesList.visibility = if (isEmpty) View.GONE else View.VISIBLE
        }
    }

    private fun loadData() {
        viewModel.loadMatches()
    }

    override fun onStart() {
        super.onStart()
        Log.d(LOG_TAG, "History: onStart")
    }

    override fun onResume() {
        super.onResume()
        Log.d(LOG_TAG, "History: onResume")
    }

    override fun onPause() {
        super.onPause()
        Log.d(LOG_TAG, "History: onPause")
    }

    override fun onStop() {
        super.onStop()
        Log.d(LOG_TAG, "History: onStop")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(LOG_TAG, "History: onDestroyView")
        _binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(LOG_TAG, "History: onDestroy")
    }

    companion object {
        private const val LOG_TAG = "DinkLog"
    }
}
