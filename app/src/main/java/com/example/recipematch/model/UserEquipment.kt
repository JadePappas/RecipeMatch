package com.example.recipematch.model

import com.google.firebase.firestore.DocumentId

data class UserEquipment(
    @DocumentId
    val id: String = "",
    val equipmentName: String = ""
)