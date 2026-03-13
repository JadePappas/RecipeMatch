package com.example.recipematch.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.example.recipematch.model.PantryItem
import com.example.recipematch.repository.PantryRepository

class PantryViewModel : ViewModel() {
    private val pantryRepository = PantryRepository()
    val pantryItems: LiveData<List<PantryItem>> = pantryRepository.getPantryItems()

    fun addPantryItem(item: PantryItem) {
        pantryRepository.addPantryItem(item)
    }

    fun updatePantryItem(item: PantryItem) {
        pantryRepository.updatePantryItem(item)
    }

    fun deletePantryItem(itemId: String) {
        pantryRepository.deletePantryItem(itemId)
    }
}