package com.example.recipematch.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.example.recipematch.model.UserEquipment
import com.example.recipematch.repository.UserEquipmentRepository

class UserEquipmentViewModel : ViewModel() {
    private val repository = UserEquipmentRepository()
    val equipmentItems: LiveData<List<UserEquipment>> = repository.getEquipmentItems()

    fun addEquipmentItem(item: UserEquipment) {
        repository.addEquipmentItem(item)
    }

    fun updateEquipmentItem(item: UserEquipment) {
        repository.updateEquipmentItem(item)
    }

    fun deleteEquipmentItem(itemId: String) {
        repository.deleteEquipmentItem(itemId)
    }
}