package com.example.recipematch.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.recipematch.model.PantryItem
import com.example.recipematch.model.UserEquipment
import com.google.firebase.firestore.FirebaseFirestore

class PantryRepository {
    private val db = FirebaseFirestore.getInstance()
    private val pantryCollection = db.collection("pantryItems")
    private val equipmentCollection = db.collection("userEquipment")

    fun getPantryItems(userId: String): LiveData<List<PantryItem>> {
        val pantryItems = MutableLiveData<List<PantryItem>>()
        pantryCollection.whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val items = snapshot.toObjects(PantryItem::class.java)
                    pantryItems.value = items
                }
            }
        return pantryItems
    }

    fun addPantryItem(item: PantryItem) {
        pantryCollection.add(item)
    }

    fun updatePantryItem(item: PantryItem) {
        if (item.id.isNotEmpty()) {
            pantryCollection.document(item.id).set(item)
        }
    }

    fun deletePantryItem(itemId: String) {
        if (itemId.isNotEmpty()) {
            pantryCollection.document(itemId).delete()
        }
    }

    fun getUserEquipment(userId: String): LiveData<List<UserEquipment>> {
        val equipment = MutableLiveData<List<UserEquipment>>()
        equipmentCollection.whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val items = snapshot.toObjects(UserEquipment::class.java)
                    equipment.value = items
                }
            }
        return equipment
    }

    fun addUserEquipment(equipment: UserEquipment) {
        equipmentCollection.add(equipment)
    }

    fun deleteUserEquipment(equipmentId: String) {
        if (equipmentId.isNotEmpty()) {
            equipmentCollection.document(equipmentId).delete()
        }
    }
}