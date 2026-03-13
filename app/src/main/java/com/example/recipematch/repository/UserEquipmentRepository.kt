package com.example.recipematch.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.recipematch.model.UserEquipment
import com.google.firebase.firestore.FirebaseFirestore

class UserEquipmentRepository {
    private val db = FirebaseFirestore.getInstance()
    private val equipmentCollection = db.collection("userEquipment")

    fun getEquipmentItems(): LiveData<List<UserEquipment>> {
        val equipmentItems = MutableLiveData<List<UserEquipment>>()
        equipmentCollection.addSnapshotListener { snapshot, e ->
            if (e != null) return@addSnapshotListener
            if (snapshot != null) {
                equipmentItems.value = snapshot.toObjects(UserEquipment::class.java)
            }
        }
        return equipmentItems
    }

    fun addEquipmentItem(item: UserEquipment) {
        equipmentCollection.add(item)
    }

    fun updateEquipmentItem(item: UserEquipment) {
        if (item.id.isNotEmpty()) {
            equipmentCollection.document(item.id).set(item)
        }
    }

    fun deleteEquipmentItem(itemId: String) {
        if (itemId.isNotEmpty()) {
            equipmentCollection.document(itemId).delete()
        }
    }
}