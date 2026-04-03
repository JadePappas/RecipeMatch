package com.example.recipematch.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.recipematch.model.Recipe
import com.example.recipematch.repository.DiscoverRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class DiscoverViewModel : ViewModel() {

    private val discoverRepository = DiscoverRepository()
    
    private val _recipes = MutableLiveData<List<Recipe>>()
    val recipes: LiveData<List<Recipe>> = _recipes

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _selectedRecipe = MutableLiveData<Recipe?>()
    val selectedRecipe: LiveData<Recipe?> = _selectedRecipe

    private var searchJob: Job? = null

    fun searchRecipes(
        query: String? = null,
        cuisine: String? = null,
        diet: String? = null,
        type: String? = null
    ) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _isLoading.value = true
            Log.d("DiscoverViewModel", "Starting search for: $query")
            
            val results = discoverRepository.searchRecipes(
                query = query,
                cuisine = cuisine,
                diet = diet,
                type = type
            )
            
            if (results != null) {
                Log.d("DiscoverViewModel", "Search successful: ${results.size} recipes found")
                _recipes.postValue(results)
            } else {
                Log.e("DiscoverViewModel", "Search failed - results are null")
                _recipes.postValue(emptyList())
            }
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