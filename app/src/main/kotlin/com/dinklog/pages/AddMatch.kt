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
import com.dinklog.R
import com.dinklog.databinding.FragmentAddMatchBinding
import com.dinklog.models.Match
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class AddMatch : Fragment() {

    private var _binding: FragmentAddMatchBinding? = null
    private val binding get() = _binding ?: error("Binding accessed after onDestroyView")

    private val viewModel: AddMatchViewModel by viewModels()

    private val matchId: Long by lazy {
        arguments?.getLong(ARG_MATCH_ID, NEW_MATCH) ?: NEW_MATCH
    }

    private val isEditMode: Boolean get() = matchId != NEW_MATCH

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(LOG_TAG, "AddMatch: onCreate")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(LOG_TAG, "AddMatch: onCreateView")
        _binding = FragmentAddMatchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(LOG_TAG, "AddMatch: onViewCreated")
        setupEditMode()
        setupClickListeners()
        observeState()
    }

    private fun setupEditMode() {
        if (!isEditMode) return
        binding.title.setText(R.string.title_edit_match)
        binding.saveButton.setText(R.string.btn_update_match)
        viewModel.loadMatch(matchId)
        observeMatch()
    }

    private fun observeMatch() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.match.collect { match ->
                    if (match != null) populateFields(match)
                }
            }
        }
    }

    private fun populateFields(match: Match) {
        binding.opponent1Input.setText(match.opponent1Name)
        binding.opponent2Input.setText(match.opponent2Name ?: "")
        binding.partnerInput.setText(match.partnerName ?: "")
        binding.teamScoreInput.setText(match.teamScore.toString())
        binding.opponentScoreInput.setText(match.opponentScore.toString())
        binding.locationInput.setText(match.locationName)
        binding.notesInput.setText(match.notes ?: "")
    }

    private fun setupClickListeners() {
        binding.saveButton.setOnClickListener {
            if (isEditMode) submitEdit() else submitCreate()
        }
    }

    private fun submitCreate() {
        viewModel.saveMatch(
            opponent1 = binding.opponent1Input.text.toString(),
            opponent2 = binding.opponent2Input.text.toString(),
            partner = binding.partnerInput.text.toString(),
            teamScore = binding.teamScoreInput.text.toString(),
            opponentScore = binding.opponentScoreInput.text.toString(),
            location = binding.locationInput.text.toString(),
            notes = binding.notesInput.text.toString()
        )
    }

    private fun submitEdit() {
        viewModel.editMatch(
            opponent1 = binding.opponent1Input.text.toString(),
            opponent2 = binding.opponent2Input.text.toString(),
            partner = binding.partnerInput.text.toString(),
            teamScore = binding.teamScoreInput.text.toString(),
            opponentScore = binding.opponentScoreInput.text.toString(),
            location = binding.locationInput.text.toString(),
            notes = binding.notesInput.text.toString()
        )
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
        Log.d(LOG_TAG, "AddMatch: onStart")
    }

    override fun onResume() {
        super.onResume()
        Log.d(LOG_TAG, "AddMatch: onResume")
    }

    override fun onPause() {
        super.onPause()
        Log.d(LOG_TAG, "AddMatch: onPause")
    }

    override fun onStop() {
        super.onStop()
        Log.d(LOG_TAG, "AddMatch: onStop")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(LOG_TAG, "AddMatch: onDestroyView")
        _binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(LOG_TAG, "AddMatch: onDestroy")
    }

    companion object {
        private const val LOG_TAG = "DinkLog"
        private const val ARG_MATCH_ID = "matchId"
        private const val NEW_MATCH = -1L
    }
}
