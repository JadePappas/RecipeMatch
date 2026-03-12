package com.example.recipematch.model

data class Level(
    val levelNumber: Int = 0,
    val levelName: String = "",       // e.g. "White Apron", "Blue Apron"
    val apronColor: String = "",      // e.g. "white", "blue", "black"
    val requiredRecipeCount: Int = 0
)