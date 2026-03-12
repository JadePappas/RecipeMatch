package com.example.recipematch.model

data class UserRecipe(
    val recipeApiId: String = "",
    val isFavorite: Boolean = false,
    val likeStatus: String = ""    // "liked", "disliked", or ""
)