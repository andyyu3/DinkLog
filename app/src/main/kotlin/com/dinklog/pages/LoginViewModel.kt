package com.dinklog.pages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dinklog.DinkLogApp
import com.dinklog.models.User
import com.dinklog.repositories.UserRepository
import com.dinklog.utils.PreferencesManager
import com.dinklog.utils.ValidationMessages
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LoginViewModel : ViewModel() {

    private val userRepository = UserRepository(DinkLogApp.getDatabase().userDao())
    private val preferencesManager = PreferencesManager(DinkLogApp.instance)

    private val _state = MutableStateFlow<LoginState>(LoginState.Idle)
    val state: StateFlow<LoginState> = _state

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _state.value = LoginState.Error(ValidationMessages.FIELDS_REQUIRED)
            return
        }

        _state.value = LoginState.Loading
        viewModelScope.launch {
            userRepository.login(email.trim(), password)
                .onSuccess { user ->
                    preferencesManager.saveUserId(user.userId)
                    _state.value = LoginState.Success(user)
                }
                .onFailure { error ->
                    _state.value = LoginState.Error(error.message ?: ValidationMessages.LOGIN_FAILED)
                }
        }
    }

    fun resetState() {
        _state.value = LoginState.Idle
    }
}

sealed class LoginState {
    data object Idle : LoginState()
    data object Loading : LoginState()
    data class Success(val user: User) : LoginState()
    data class Error(val message: String) : LoginState()
}
