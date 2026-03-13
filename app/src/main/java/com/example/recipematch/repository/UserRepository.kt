package com.example.recipematch.repository

import com.example.recipematch.model.User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

// Handles all Firestore CRUD operations for user profile data
class UserRepository {
    private val db = FirebaseFirestore.getInstance()
    private val usersCollection = db.collection("users")

    suspend fun createUserProfile(user: User): Boolean {
        return try {
            usersCollection.document(user.userId).set(user).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getUserProfile(userId: String): User? {
        return try {
            val document = usersCollection.document(userId).get().await()
            document.toObject(User::class.java)
        } catch (e: Exception) {
            null
        }
    }
}