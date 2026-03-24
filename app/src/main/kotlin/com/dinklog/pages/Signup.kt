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
import com.dinklog.databinding.FragmentSignupBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class Signup : Fragment() {

    private var _binding: FragmentSignupBinding? = null
    private val binding get() = _binding ?: error("Binding accessed after onDestroyView")

    private val viewModel: SignupViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(LOG_TAG, "Signup: onCreate")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(LOG_TAG, "Signup: onCreateView")
        _binding = FragmentSignupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(LOG_TAG, "Signup: onViewCreated")
        setupClickListeners()
        observeState()
    }

    private fun setupClickListeners() {
        binding.signupButton.setOnClickListener {
            viewModel.signup(
                email = binding.emailInput.text.toString(),
                name = binding.nameInput.text.toString(),
                password = binding.passwordInput.text.toString(),
                confirmPassword = binding.confirmPasswordInput.text.toString()
            )
        }

        binding.loginLink.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    when (state) {
                        is SignupState.Idle -> binding.loading.visibility = View.GONE
                        is SignupState.Loading -> binding.loading.visibility = View.VISIBLE
                        is SignupState.Success -> handleSignupSuccess()
                        is SignupState.Error -> handleSignupError(state.message)
                    }
                }
            }
        }
    }

    private fun handleSignupSuccess() {
        binding.loading.visibility = View.GONE
        findNavController().navigate(R.id.action_signup_to_history)
    }

    private fun handleSignupError(message: String) {
        binding.loading.visibility = View.GONE
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
        viewModel.resetState()
    }

    override fun onStart() {
        super.onStart()
        Log.d(LOG_TAG, "Signup: onStart")
    }

    override fun onResume() {
        super.onResume()
        Log.d(LOG_TAG, "Signup: onResume")
    }

    override fun onPause() {
        super.onPause()
        Log.d(LOG_TAG, "Signup: onPause")
    }

    override fun onStop() {
        super.onStop()
        Log.d(LOG_TAG, "Signup: onStop")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(LOG_TAG, "Signup: onDestroyView")
        _binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(LOG_TAG, "Signup: onDestroy")
    }

    companion object {
        private const val LOG_TAG = "DinkLog"
    }
}
