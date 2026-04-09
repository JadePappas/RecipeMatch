package com.example.recipematch.model

import com.google.firebase.firestore.DocumentId

data class RecipeAttempt(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val recipeApiId: String = "",
    val recipeTitle: String = "",
    val notes: String = "",
    val photoUri: String = "",
    val dateCompleted: String = ""
)