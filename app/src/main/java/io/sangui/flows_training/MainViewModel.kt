package io.sangui.flows_training

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import timber.log.Timber

class MainViewModel: ViewModel(), CoroutineScope {

    private val job: CompletableJob = SupervisorJob()
    override val coroutineContext = Dispatchers.IO + job

    private val _loginUiState = MutableStateFlow<LoginUiState>(LoginUiState.Empty)
    val loginUiState: StateFlow<LoginUiState> = _loginUiState

    fun login(username: String, password: String) = launch {
        _loginUiState.value = LoginUiState.Loading
        delay(2000L)
        if (username == "android" && password == "topsecret") {
            _loginUiState.value = LoginUiState.Success
        } else {
            _loginUiState.value = LoginUiState.Error("wrong username or password")
        }
    }

    sealed class LoginUiState {
        object Success : LoginUiState()
        data class Error(val message: String): LoginUiState()
        object Loading : LoginUiState()
        object Empty : LoginUiState()
    }
}