package com.example.recipematch.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.recipematch.model.User
import com.example.recipematch.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class UserViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val repository = UserRepository()

    private val _signupResult = MutableLiveData<Boolean>()
    val signupResult: LiveData<Boolean> = _signupResult

    private val _loginResult = MutableLiveData<Boolean>()
    val loginResult: LiveData<Boolean> = _loginResult

    private val _userData = MutableLiveData<User?>()
    val userData: LiveData<User?> = _userData

    fun signUp(email: String, password: String, username: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid ?: ""
                    val newUser = User(userId = userId, username = username, email = email)
                    
                    viewModelScope.launch {
                        val success = repository.createUserProfile(newUser)
                        _signupResult.postValue(success)
                    }
                } else {
                    _signupResult.postValue(false)
                }
            }
    }

    fun signIn(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                _loginResult.postValue(task.isSuccessful)
            }
    }

    fun signOut() {
        auth.signOut()
    }

    fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null
    }

    fun fetchUserProfile() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            viewModelScope.launch {
                val user = repository.getUserProfile(userId)
                _userData.postValue(user)
            }
        }
    }
}