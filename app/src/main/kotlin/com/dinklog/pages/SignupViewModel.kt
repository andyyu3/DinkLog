package com.dinklog.pages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dinklog.DinkLogApp
import com.dinklog.models.User
import com.dinklog.repositories.UserRepository
import com.dinklog.utils.PreferencesManager
import com.dinklog.utils.ValidationConstants
import com.dinklog.utils.ValidationMessages
import com.dinklog.utils.isValidEmail
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SignupViewModel : ViewModel() {

    private val userRepository = UserRepository(DinkLogApp.getDatabase().userDao())
    private val preferencesManager = PreferencesManager(DinkLogApp.instance)

    private val _state = MutableStateFlow<SignupState>(SignupState.Idle)
    val state: StateFlow<SignupState> = _state

    fun signup(email: String, name: String, password: String, confirmPassword: String) {
        val validationError = validateInput(email, name, password, confirmPassword)
        if (validationError != null) {
            _state.value = SignupState.Error(validationError)
            return
        }

        _state.value = SignupState.Loading
        viewModelScope.launch {
            userRepository.signup(email.trim(), name.trim(), password)
                .onSuccess { user ->
                    preferencesManager.saveUserId(user.userId)
                    _state.value = SignupState.Success(user)
                }
                .onFailure { error ->
                    _state.value = SignupState.Error(error.message ?: ValidationMessages.SIGNUP_FAILED)
                }
        }
    }

    private fun validateInput(
        email: String,
        name: String,
        password: String,
        confirmPassword: String
    ): String? = when {
        email.isBlank() || name.isBlank() || password.isBlank() -> ValidationMessages.FIELDS_REQUIRED
        !email.isValidEmail() -> ValidationMessages.INVALID_EMAIL
        password.length < ValidationConstants.MIN_PASSWORD_LENGTH -> ValidationMessages.PASSWORD_TOO_SHORT
        password != confirmPassword -> ValidationMessages.PASSWORDS_NO_MATCH
        else -> null
    }

    fun resetState() {
        _state.value = SignupState.Idle
    }
}

sealed class SignupState {
    data object Idle : SignupState()
    data object Loading : SignupState()
    data class Success(val user: User) : SignupState()
    data class Error(val message: String) : SignupState()
}
