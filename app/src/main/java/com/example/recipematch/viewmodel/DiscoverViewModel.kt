package com.example.recipematch.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.recipematch.model.PantryItem
import com.example.recipematch.model.Recipe
import com.example.recipematch.model.UserEquipment
import com.example.recipematch.repository.DiscoverRepository
import com.example.recipematch.repository.PantryRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class DiscoverViewModel : ViewModel() {

    private val discoverRepository = DiscoverRepository()
    private val pantryRepository = PantryRepository()
    private val auth = FirebaseAuth.getInstance()

    private val _recipes = MutableLiveData<List<Recipe>>()
    val recipes: LiveData<List<Recipe>> = _recipes

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _selectedRecipe = MutableLiveData<Recipe?>()
    val selectedRecipe: LiveData<Recipe?> = _selectedRecipe

    fun searchRecipes(query: String? = null, diet: String? = null, type: String? = null) {
        val userId = auth.currentUser?.uid ?: return
        
        _isLoading.value = true
        
        // We'll observe pantry items and equipment once to build the search context
        // For simplicity in this implementation, we search with just the query first, 
        // but we can enhance it by fetching pantry/equipment if needed.
        viewModelScope.launch {
            val results = discoverRepository.searchRecipes(
                query = query,
                diet = diet,
                type = type
            )
            _recipes.postValue(results ?: emptyList())
            _isLoading.postValue(false)
        }
    }

    fun getRecipeDetails(recipeId: Int) {
        _isLoading.value = true
        viewModelScope.launch {
            val details = discoverRepository.getRecipeInformation(recipeId)
            _selectedRecipe.postValue(details)
            _isLoading.postValue(false)
        }
    }
    
    fun selectRecipe(recipe: Recipe?) {
        _selectedRecipe.value = recipe
    }
}