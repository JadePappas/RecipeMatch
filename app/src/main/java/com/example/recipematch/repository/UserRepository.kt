package com.example.recipematch.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.recipematch.model.User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class UserRepository {
    private val db = FirebaseFirestore.getInstance()
    private val usersCollection = db.collection("users")

    fun getUserProfileLive(userId: String): LiveData<User?> {
        val liveData = MutableLiveData<User?>()
        usersCollection.document(userId).addSnapshotListener { snapshot, e ->
            if (e != null) return@addSnapshotListener
            liveData.value = snapshot?.toObject(User::class.java)
        }
        return liveData
    }

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

    suspend fun updateUserProfile(user: User): Boolean {
        return try {
            usersCollection.document(user.userId).set(user).await()
            true
        } catch (e: Exception) {
            false
        }
    }
}