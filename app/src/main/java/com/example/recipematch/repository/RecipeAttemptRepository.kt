package com.example.recipematch.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.recipematch.model.RecipeAttempt
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class RecipeAttemptRepository {
    private val db = FirebaseFirestore.getInstance()
    private val attemptsCollection = db.collection("recipeAttempts")

    fun getCookingHistory(userId: String): LiveData<List<RecipeAttempt>> {
        val history = MutableLiveData<List<RecipeAttempt>>()
        attemptsCollection
            .whereEqualTo("userId", userId)
            .orderBy("dateCompleted", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                if (snapshot != null) {
                    history.value = snapshot.toObjects(RecipeAttempt::class.java)
                }
            }
        return history
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

    fun saveRecipeAttempt(attempt: RecipeAttempt, onComplete: (Boolean) -> Unit) {
        attemptsCollection.add(attempt)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    fun deleteRecipeAttempt(attemptId: String, onComplete: (Boolean) -> Unit) {
        if (attemptId.isNotEmpty()) {
            attemptsCollection.document(attemptId).delete()
                .addOnSuccessListener { onComplete(true) }
                .addOnFailureListener { onComplete(false) }
        }
    }
}