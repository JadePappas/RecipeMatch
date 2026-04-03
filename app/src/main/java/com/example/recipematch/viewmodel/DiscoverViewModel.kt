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
    
    // LiveData for the Discover tab (affected by user filters)
    private val _recipes = MutableLiveData<List<Recipe>>()
    val recipes: LiveData<List<Recipe>> = _recipes

    // New LiveData for the Home tab (stays consistent/unfiltered)
    private val _homeRecipes = MutableLiveData<List<Recipe>>()
    val homeRecipes: LiveData<List<Recipe>> = _homeRecipes

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _selectedRecipe = MutableLiveData<Recipe?>()
    val selectedRecipe: LiveData<Recipe?> = _selectedRecipe

    private var searchJob: Job? = null
    private var homeJob: Job? = null

    fun searchRecipes(
        query: String? = null,
        cuisine: String? = null,
        diet: String? = null,
        type: String? = null
    ) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _isLoading.value = true
            Log.d("DiscoverViewModel", "Discover Search: $query")
            
            val results = discoverRepository.searchRecipes(
                query = query,
                cuisine = cuisine,
                diet = diet,
                type = type
            )
            
            if (results != null) {
                _recipes.postValue(results)
            } else {
                _recipes.postValue(emptyList())
            }
            _isLoading.postValue(false)
        }
    }

    // Fetches a broad set of recipes specifically for Home page recommendations
    fun fetchHomeRecipes() {
        if (!_homeRecipes.value.isNullOrEmpty()) return // Don't re-fetch if we already have them
        
        homeJob?.cancel()
        homeJob = viewModelScope.launch {
            Log.d("DiscoverViewModel", "Fetching Home Recommendations...")
            val results = discoverRepository.searchRecipes() // No filters
            if (results != null) {
                _homeRecipes.postValue(results)
            }
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