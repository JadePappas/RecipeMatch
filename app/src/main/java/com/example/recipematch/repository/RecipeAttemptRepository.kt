package com.example.recipematch.repository

import com.example.recipematch.model.RecipeAttempt
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class RecipeAttemptRepository {
    private val db = FirebaseFirestore.getInstance()
    private val attemptsCollection = db.collection("recipeAttempts")

    fun saveRecipeAttempt(attempt: RecipeAttempt, onComplete: (Boolean) -> Unit) {
        attemptsCollection.add(attempt)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    suspend fun getRecipeAttempt(userId: String, recipeId: String): RecipeAttempt? {
        return try {
            val snapshot = attemptsCollection
                .whereEqualTo("userId", userId)
                .whereEqualTo("recipeApiId", recipeId)
                .limit(1)
                .get()
                .await()
            
            if (!snapshot.isEmpty) {
                snapshot.documents.first().toObject(RecipeAttempt::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun deleteRecipeAttempt(attemptId: String, onComplete: (Boolean) -> Unit) {
        attemptsCollection.document(attemptId).delete()
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }
}