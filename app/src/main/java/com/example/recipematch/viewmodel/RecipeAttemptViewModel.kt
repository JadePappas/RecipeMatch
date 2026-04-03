package com.example.recipematch.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.example.recipematch.model.RecipeAttempt
import com.example.recipematch.repository.RecipeAttemptRepository
import com.google.firebase.auth.FirebaseAuth

class RecipeAttemptViewModel : ViewModel() {
    private val repository = RecipeAttemptRepository()
    private val auth = FirebaseAuth.getInstance()

    val cookingHistory: LiveData<List<RecipeAttempt>> = 
        repository.getCookingHistory(auth.currentUser?.uid ?: "")

    fun saveAttempt(recipeApiId: String, notes: String, photoUri: String, dateCompleted: String) {
        val userId = auth.currentUser?.uid ?: return
        val attempt = RecipeAttempt(
            userId = userId,
            recipeApiId = recipeApiId,
            notes = notes,
            photoUri = photoUri,
            dateCompleted = dateCompleted
        )
        repository.saveRecipeAttempt(attempt) { success ->
            // Handle success/failure if needed (e.g., via another LiveData)
        }
    }
}