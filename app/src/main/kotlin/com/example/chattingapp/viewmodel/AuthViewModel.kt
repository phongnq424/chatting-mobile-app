package com.example.chattingapp.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.example.chattingapp.data.repository.AuthRepository
import com.google.firebase.auth.FirebaseUser

class AuthViewModel(
    private val repo: AuthRepository
) : ViewModel() {

    var isLoading = mutableStateOf(false)
    var loginSuccess = mutableStateOf(false)
    var errorMessage = mutableStateOf<String?>(null)

    val currentUser: FirebaseUser?
        get() = repo.getCurrentUser()

    fun login(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            errorMessage.value = "Vui lòng không để trống!"
            return
        }

        isLoading.value = true

        repo.signInWithEmail(email.trim(), pass) { error ->
            isLoading.value = false

            if (error == null) {
                loginSuccess.value = true
                errorMessage.value = null
            } else {
                loginSuccess.value = false
                errorMessage.value = error
            }
        }
    }

    fun signUp(email: String, pass: String, name: String) {
        if (email.isBlank() || pass.isBlank() || name.isBlank()) {
            errorMessage.value = "Vui lòng điền đầy đủ thông tin!"
            return
        }

        isLoading.value = true

        repo.signUpWithEmail(
            email = email.trim(),
            pass = pass,
            name = name.trim()
        ) { error ->
            isLoading.value = false

            if (error == null) {
                loginSuccess.value = true
                errorMessage.value = null
            } else {
                loginSuccess.value = false
                errorMessage.value = error
            }
        }
    }

    fun logout(onComplete: () -> Unit) {
        isLoading.value = true

        repo.logout {
            isLoading.value = false
            loginSuccess.value = false
            errorMessage.value = null
            onComplete()
        }
    }
    fun onGoogleSignInResult(idToken: String) {
        isLoading.value = true

        repo.signInWithGoogle(idToken) { error ->
            isLoading.value = false

            if (error == null) {
                loginSuccess.value = true
                errorMessage.value = null
            } else {
                loginSuccess.value = false
                errorMessage.value = error
            }
        }
    }

    fun updateName(newName: String, onSuccess: () -> Unit) {
        if (newName.isBlank()) {
            errorMessage.value = "Tên không được để trống"
            return
        }

        isLoading.value = true

        repo.updateDisplayName(newName.trim()) { error ->
            isLoading.value = false

            if (error == null) {
                errorMessage.value = null
                onSuccess()
            } else {
                errorMessage.value = error
            }
        }
    }

    fun updateFCMToken(token: String) {
        repo.updateFCMToken(token)
    }

    fun clearError() {
        errorMessage.value = null
    }

    fun resetLoginSuccess() {
        loginSuccess.value = false
    }
}