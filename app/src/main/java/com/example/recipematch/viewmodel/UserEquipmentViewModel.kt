package com.example.recipematch.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.switchMap
import com.example.recipematch.model.UserEquipment
import com.example.recipematch.repository.UserEquipmentRepository
import com.google.firebase.auth.FirebaseAuth

class UserEquipmentViewModel : ViewModel() {
    private val repository = UserEquipmentRepository()
    private val auth = FirebaseAuth.getInstance()

    private val _userId = MutableLiveData<String?>()

    init {
        _userId.value = auth.currentUser?.uid
    }

    val equipmentItems: LiveData<List<UserEquipment>> = _userId.switchMap { uid ->
        if (uid != null) {
            repository.getEquipmentItems(uid)
        } else {
            MutableLiveData(emptyList())
        }
    }

    fun addEquipmentItem(name: String) {
        val uid = auth.currentUser?.uid ?: return
        val newItem = UserEquipment(
            userId = uid,
            equipmentName = name
        )
        repository.addEquipmentItem(newItem)
    }

    fun updateEquipmentItem(item: UserEquipment) {
        repository.updateEquipmentItem(item)
    }

    fun deleteEquipmentItem(itemId: String) {
        repository.deleteEquipmentItem(itemId)
    }
}