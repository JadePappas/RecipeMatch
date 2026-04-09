package com.example.recipematch.viewmodel

import androidx.lifecycle.*
import com.example.recipematch.model.User
import com.example.recipematch.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import kotlinx.coroutines.launch

class UserViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val repository = UserRepository()

    private val userId = auth.currentUser?.uid ?: ""

    val userData: LiveData<User?> = if (userId.isNotEmpty()) {
        repository.getUserProfileLive(userId)
    } else {
        MutableLiveData(null)
    }

    private val _updateResult = MutableLiveData<Boolean>()
    val updateResult: LiveData<Boolean> = _updateResult

    private val _loginResult = MutableLiveData<Boolean>()
    val loginResult: LiveData<Boolean> = _loginResult

    private val _signupResult = MutableLiveData<Boolean>()
    val signupResult: LiveData<Boolean> = _signupResult

    private val _passwordUpdateMessage = MutableLiveData<String?>()
    val passwordUpdateMessage: LiveData<String?> = _passwordUpdateMessage

    private fun getBeltTitle(level: Int): String {
        return when (level) {
            1 -> "White Belt"
            2 -> "Yellow Belt"
            3 -> "Orange Belt"
            4 -> "Green Belt"
            5 -> "Blue Belt"
            6 -> "Purple Belt"
            7 -> "Red Belt"
            8 -> "Brown Belt"
            else -> "Black Belt"
        }
    }

    fun signUp(email: String, password: String, username: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = auth.currentUser?.uid ?: ""
                    val newUser = User(
                        userId = uid, 
                        username = username, 
                        email = email, 
                        displayName = username,
                        levelTitle = "White Belt"
                    )
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

    fun isUserLoggedIn(): Boolean = auth.currentUser != null

    fun signOut() {
        auth.signOut()
    }

    fun updateProfile(displayName: String, email: String) {
        val currentUser = auth.currentUser
        val uid = currentUser?.uid ?: return

        viewModelScope.launch {
            val currentData = userData.value
            val updatedUser = currentData?.copy(
                displayName = displayName,
                email = email
            ) ?: User(userId = uid, displayName = displayName, email = email)

            val success = repository.updateUserProfile(updatedUser)
            
            if (success && email != currentUser.email) {
                currentUser.updateEmail(email).addOnCompleteListener { task ->
                    _updateResult.postValue(task.isSuccessful)
                }
            } else {
                _updateResult.postValue(success)
            }
        }
    }

    fun rewardExperience(xpAmount: Int) {
        val uid = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            val currentData = userData.value ?: repository.getUserProfile(uid)
            if (currentData != null) {
                var newXp = currentData.xp + xpAmount
                var newLevel = currentData.levelNumber
                val newRecipesCompleted = currentData.recipesCompleted + 1
                
                while (newXp >= currentData.totalXpNeeded) {
                    newXp -= currentData.totalXpNeeded
                    newLevel++
                }

                val updatedUser = currentData.copy(
                    xp = newXp,
                    levelNumber = newLevel,
                    levelTitle = getBeltTitle(newLevel),
                    recipesCompleted = newRecipesCompleted
                )
                repository.updateUserProfile(updatedUser)
            }
        }
    }

    fun updatePassword(newPassword: String) {
        auth.currentUser?.updatePassword(newPassword)
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _passwordUpdateMessage.postValue("Success")
                } else {
                    val error = when (task.exception) {
                        is FirebaseAuthRecentLoginRequiredException -> "Security timeout: Please log out and back in to change your password."
                        else -> task.exception?.localizedMessage ?: "Failed to update password"
                    }
                    _passwordUpdateMessage.postValue(error)
                }
            }
    }
}