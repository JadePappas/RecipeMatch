package com.example.recipematch.model

import com.google.firebase.firestore.DocumentId

data class Album(
    @DocumentId
    val albumId: String = "",
    val userId: String = "",
    val albumName: String = "",
    val recipes: List<String> = emptyList() // stores recipe_api_ids
)