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
import com.dinklog.databinding.FragmentAddMatchBinding
import com.dinklog.utils.PreferencesManager
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class AddMatch : Fragment() {

    private var _binding: FragmentAddMatchBinding? = null
    private val binding get() = _binding ?: error("Binding accessed after onDestroyView")

    private val viewModel: AddMatchViewModel by viewModels()

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
        _binding = FragmentAddMatchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
        observeState()
    }

    private fun setupClickListeners() {
        binding.saveButton.setOnClickListener {
            val userId = PreferencesManager(requireContext()).getUserId() ?: return@setOnClickListener
            viewModel.saveMatch(
                userId = userId,
                opponent1 = binding.opponent1Input.text.toString(),
                opponent2 = binding.opponent2Input.text.toString(),
                partner = binding.partnerInput.text.toString(),
                teamScore = binding.teamScoreInput.text.toString(),
                opponentScore = binding.opponentScoreInput.text.toString(),
                location = binding.locationInput.text.toString(),
                notes = binding.notesInput.text.toString()
            )
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    when (state) {
                        is SaveState.Idle -> binding.loading.visibility = View.GONE
                        is SaveState.Saving -> binding.loading.visibility = View.VISIBLE
                        is SaveState.Success -> findNavController().popBackStack()
                        is SaveState.Error -> handleSaveError(state.message)
                    }
                }
            }
        }
    }

    private fun handleSaveError(message: String) {
        binding.loading.visibility = View.GONE
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
        viewModel.resetState()
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
        private const val TAG = "AddMatchFragment"
    }
}
