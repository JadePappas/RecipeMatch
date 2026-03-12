package com.example.recipematch.model

data class RecipeAttempt(
    val attemptId: String = "",
    val recipeApiId: String = "",
    val notes: String = "",
    val photoUri: String = "",
    val dateCompleted: String = ""
)