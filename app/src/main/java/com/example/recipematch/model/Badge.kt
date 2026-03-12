package com.example.recipematch.model

data class Badge(
    val badgeId: String = "",
    val badgeName: String = "",
    val description: String = "",
    val requiredRecipeCount: Int = 0,
    val requiredCuisineType: String = ""
)