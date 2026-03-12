package com.example.recipematch.model

data class User(
    val userId: String = "",
    val username: String = "",
    val email: String = "",
    val dietaryPreferences: String = "",
    val levelNumber: Int = 1,
    val recipesCompleted: Int = 0
)