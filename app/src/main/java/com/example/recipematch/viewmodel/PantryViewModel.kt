package com.example.recipematch.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.switchMap
import com.example.recipematch.model.PantryItem
import com.example.recipematch.repository.PantryRepository
import com.google.firebase.auth.FirebaseAuth

class PantryViewModel : ViewModel() {
    private val pantryRepository = PantryRepository()
    private val auth = FirebaseAuth.getInstance()
    
    private val _userId = MutableLiveData<String?>()
    
    init {
        _userId.value = auth.currentUser?.uid
    }

    val pantryItems: LiveData<List<PantryItem>> = _userId.switchMap { uid ->
        if (uid != null) {
            pantryRepository.getPantryItems(uid)
        } else {
            MutableLiveData(emptyList())
        }
    }

    fun addPantryItem(name: String, quantity: Double, unit: String) {
        val uid = auth.currentUser?.uid ?: return
        val newItem = PantryItem(
            userId = uid,
            ingredientName = name,
            quantity = quantity,
            unit = unit
        )
        pantryRepository.addPantryItem(newItem)
    }

    fun updatePantryItem(item: PantryItem) {
        pantryRepository.updatePantryItem(item)
    }

    fun deletePantryItem(itemId: String) {
        pantryRepository.deletePantryItem(itemId)
    }
}