package com.example.recipematch.model

data class User(
    val userId: String = "",
    val username: String = "",
    val displayName: String = "",
    val email: String = "",
    val profilePictureUrl: String = "",
    val levelNumber: Int = 1,
    val levelTitle: String = "Novice Cook",
    val xp: Int = 0,
    val totalXpNeeded: Int = 1000,
    val recipesCompleted: Int = 0,
    val dietaryPreferences: String = ""
)