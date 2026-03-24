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
import com.dinklog.databinding.FragmentLoginBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class Login : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding ?: error("Binding accessed after onDestroyView")

    private val viewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(LOG_TAG, "Login: onCreate")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(LOG_TAG, "Login: onCreateView")
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(LOG_TAG, "Login: onViewCreated")
        setupClickListeners()
        observeState()
    }

    private fun setupClickListeners() {
        binding.loginButton.setOnClickListener {
            viewModel.login(
                email = binding.emailInput.text.toString(),
                password = binding.passwordInput.text.toString()
            )
        }

        binding.signupLink.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_signup)
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    when (state) {
                        is LoginState.Idle -> binding.loading.visibility = View.GONE
                        is LoginState.Loading -> binding.loading.visibility = View.VISIBLE
                        is LoginState.Success -> handleLoginSuccess()
                        is LoginState.Error -> handleLoginError(state.message)
                    }
                }
            }
        }
    }

    private fun handleLoginSuccess() {
        binding.loading.visibility = View.GONE
        findNavController().navigate(R.id.action_login_to_history)
    }

    private fun handleLoginError(message: String) {
        binding.loading.visibility = View.GONE
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
        viewModel.resetState()
    }

    override fun onStart() {
        super.onStart()
        Log.d(LOG_TAG, "Login: onStart")
    }

    override fun onResume() {
        super.onResume()
        Log.d(LOG_TAG, "Login: onResume")
    }

    override fun onPause() {
        super.onPause()
        Log.d(LOG_TAG, "Login: onPause")
    }

    override fun onStop() {
        super.onStop()
        Log.d(LOG_TAG, "Login: onStop")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(LOG_TAG, "Login: onDestroyView")
        _binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(LOG_TAG, "Login: onDestroy")
    }

    companion object {
        private const val LOG_TAG = "DinkLog"
    }
}
