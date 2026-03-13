package com.example.recipematch.model

import com.google.firebase.firestore.DocumentId

data class PantryItem(
    @DocumentId
    val id: String = "",
    val ingredientName: String = "",
    val quantity: Double = 0.0,
    val unit: String = ""
)