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
import androidx.recyclerview.widget.LinearLayoutManager
import com.dinklog.R
import com.dinklog.components.MatchAdapter
import com.dinklog.databinding.FragmentHistoryBinding
import com.dinklog.utils.PreferencesManager
import kotlinx.coroutines.launch

class History : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding ?: error("Binding accessed after onDestroyView")

    private val viewModel: HistoryViewModel by viewModels()
    private val matchAdapter = MatchAdapter()

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
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupClickListeners()
        observeMatches()
        loadData()
    }

    private fun setupRecyclerView() {
        binding.matchesList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = matchAdapter
        }
    }

    private fun setupClickListeners() {
        binding.fabAddMatch.setOnClickListener {
            findNavController().navigate(R.id.action_history_to_addMatch)
        }
    }

    private fun observeMatches() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.matches.collect { matches ->
                    matchAdapter.submitList(matches)
                    binding.emptyState.visibility = if (matches.isEmpty()) View.VISIBLE else View.GONE
                    binding.matchesList.visibility = if (matches.isEmpty()) View.GONE else View.VISIBLE
                }
            }
        }
    }

    private fun loadData() {
        val userId = PreferencesManager(requireContext()).getUserId() ?: return
        viewModel.loadMatches(userId)
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
        private const val TAG = "HistoryFragment"
    }
}
