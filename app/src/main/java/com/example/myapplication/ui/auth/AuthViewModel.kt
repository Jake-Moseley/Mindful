package com.example.myapplication.ui.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.userProfileChangeRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoginMode: Boolean = true,
    val email: String = "",
    val password: String = "",
    val displayName: String = "",
    val errorMessage: String? = null,
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = false
)

class AuthViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState

    companion object {
        private const val WEB_CLIENT_ID = "31216854424-p44r5oov4icrfmgm150tv6b5sb3ieqgs.apps.googleusercontent.com"
    }

    init {
        if (auth.currentUser != null) {
            _uiState.value = _uiState.value.copy(isAuthenticated = true)
        }
    }

    fun onEmailChanged(email: String) {
        _uiState.value = _uiState.value.copy(email = email, errorMessage = null)
    }

    fun onPasswordChanged(password: String) {
        _uiState.value = _uiState.value.copy(password = password, errorMessage = null)
    }

    fun onDisplayNameChanged(name: String) {
        _uiState.value = _uiState.value.copy(displayName = name, errorMessage = null)
    }

    fun toggleMode() {
        _uiState.value = _uiState.value.copy(
            isLoginMode = !_uiState.value.isLoginMode,
            errorMessage = null
        )
    }

    fun submit() {
        val state = _uiState.value
        if (state.email.isBlank() || state.password.isBlank()) {
            _uiState.value = state.copy(errorMessage = "Email and password are required")
            return
        }
        if (!state.isLoginMode && state.displayName.isBlank()) {
            _uiState.value = state.copy(errorMessage = "Display name is required")
            return
        }

        _uiState.value = state.copy(isLoading = true, errorMessage = null)

        if (state.isLoginMode) login() else register()
    }

    fun signInWithGoogle(context: Context) {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(WEB_CLIENT_ID)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val credentialManager = CredentialManager.create(context)

        viewModelScope.launch {
            try {
                val result = credentialManager.getCredential(context, request)
                val googleIdToken = GoogleIdTokenCredential.createFrom(result.credential.data)
                val firebaseCredential = GoogleAuthProvider.getCredential(googleIdToken.idToken, null)

                auth.signInWithCredential(firebaseCredential)
                    .addOnSuccessListener {
                        _uiState.value = _uiState.value.copy(isAuthenticated = true, isLoading = false)
                    }
                    .addOnFailureListener { e ->
                        _uiState.value = _uiState.value.copy(
                            errorMessage = e.localizedMessage ?: "Google sign-in failed",
                            isLoading = false
                        )
                    }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.localizedMessage ?: "Google sign-in failed",
                    isLoading = false
                )
            }
        }
    }

    private fun login() {
        val state = _uiState.value
        auth.signInWithEmailAndPassword(state.email, state.password)
            .addOnSuccessListener {
                _uiState.value = _uiState.value.copy(isAuthenticated = true, isLoading = false)
            }
            .addOnFailureListener { e ->
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.localizedMessage ?: "Login failed",
                    isLoading = false
                )
            }
    }

    private fun register() {
        val state = _uiState.value
        auth.createUserWithEmailAndPassword(state.email, state.password)
            .addOnSuccessListener { result ->
                val profileUpdates = userProfileChangeRequest {
                    displayName = state.displayName
                }
                result.user?.updateProfile(profileUpdates)?.addOnCompleteListener {
                    _uiState.value = _uiState.value.copy(isAuthenticated = true, isLoading = false)
                }
            }
            .addOnFailureListener { e ->
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.localizedMessage ?: "Registration failed",
                    isLoading = false
                )
            }
    }

    fun logout() {
        auth.signOut()
        _uiState.value = AuthUiState()
    }
}
