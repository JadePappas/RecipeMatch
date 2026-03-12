package com.example.recipematch.model

data class Album(
    val albumId: String = "",
    val albumName: String = "",
    val recipes: List<String> = emptyList()   // stores recipe_api_ids
)