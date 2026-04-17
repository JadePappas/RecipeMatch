package com.example.recipematch.viewmodel

import android.util.Log
import androidx.lifecycle.*
import com.example.recipematch.model.User
import com.example.recipematch.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import kotlinx.coroutines.launch

class UserViewModel(private val savedStateHandle: SavedStateHandle) : ViewModel() {
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

    private val _loginResult = MutableLiveData<Pair<Boolean, String?>>()
    val loginResult: LiveData<Pair<Boolean, String?>> = _loginResult

    private val _signupResult = MutableLiveData<Pair<Boolean, String?>>()
    val signupResult: LiveData<Pair<Boolean, String?>> = _signupResult

    private val _passwordUpdateMessage = MutableLiveData<String?>()
    val passwordUpdateMessage: LiveData<String?> = _passwordUpdateMessage

    private val _passwordResetResult = MutableLiveData<Pair<Boolean, String?>>()
    val passwordResetResult: LiveData<Pair<Boolean, String?>> = _passwordResetResult

    companion object {
        fun getBeltTitle(level: Int): String {
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

        fun calculateProgress(user: User, xpAmount: Int): User {
            var newXp = user.xp + xpAmount
            var newLevel = user.levelNumber
            val newRecipesCompleted = user.recipesCompleted + 1
            
            while (newXp >= user.totalXpNeeded) {
                newXp -= user.totalXpNeeded
                newLevel++
            }

            return user.copy(
                xp = newXp,
                levelNumber = newLevel,
                levelTitle = getBeltTitle(newLevel),
                recipesCompleted = newRecipesCompleted
            )
        }
    }

    fun signUp(email: String, password: String, username: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val firebaseUser = auth.currentUser
                    firebaseUser?.sendEmailVerification()
                        ?.addOnCompleteListener { verificationTask ->
                            if (verificationTask.isSuccessful) {
                                Log.d("UserViewModel", "Verification email sent to $email")
                            }
                        }
                    
                    val uid = firebaseUser?.uid ?: ""
                    val newUser = User(
                        userId = uid, 
                        username = username, 
                        email = email, 
                        displayName = username,
                        levelTitle = "White Belt"
                    )
                    viewModelScope.launch {
                        val success = repository.createUserProfile(newUser)
                        if (success) {
                            _signupResult.postValue(Pair(true, "Please check your email for verification link."))
                        } else {
                            _signupResult.postValue(Pair(false, "Profile creation failed."))
                        }
                    }
                } else {
                    _signupResult.postValue(Pair(false, task.exception?.localizedMessage ?: "Signup Failed"))
                }
            }
    }

    fun signIn(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null && user.isEmailVerified) {
                        _loginResult.postValue(Pair(true, null))
                    } else {
                        auth.signOut()
                        _loginResult.postValue(Pair(false, "Please verify your email before logging in."))
                    }
                } else {
                    _loginResult.postValue(Pair(false, task.exception?.localizedMessage ?: "Login Failed"))
                }
            }
    }

    fun resetPassword(email: String) {
        Log.d("UserViewModel", "Attempting password reset for: $email")
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("UserViewModel", "Reset email successfully sent to $email")
                    _passwordResetResult.postValue(Pair(true, "Reset link sent to $email. Please check your inbox and spam."))
                } else {
                    val error = task.exception?.localizedMessage ?: "Failed to send reset email."
                    Log.e("UserViewModel", "Reset failed: $error")
                    _passwordResetResult.postValue(Pair(false, error))
                }
            }
    }

    fun isUserLoggedIn(): Boolean {
        val user = auth.currentUser
        return user != null && user.isEmailVerified
    }

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
                val updatedUser = calculateProgress(currentData, xpAmount)
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