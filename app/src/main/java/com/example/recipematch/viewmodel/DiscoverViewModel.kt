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

    private var currentOffset = 0
    private val pageSize = 20
    private var isLastPage = false

    private var searchJob: Job? = null
    private var homeJob: Job? = null

    fun searchRecipes(
        query: String? = null,
        cuisine: String? = null,
        diet: String? = null,
        type: String? = null,
        isLoadMore: Boolean = false
    ) {
        if (!isLoadMore) {
            currentOffset = 0
            isLastPage = false
        } else if (isLastPage || _isLoading.value == true) {
            return
        }

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _isLoading.value = true
            
            val results = discoverRepository.searchRecipes(
                query = query,
                cuisine = cuisine,
                diet = diet,
                type = type,
                offset = currentOffset,
                number = pageSize
            )
            
            if (results != null) {
                if (results.size < pageSize) isLastPage = true
                
                val currentList = if (isLoadMore) _recipes.value ?: emptyList() else emptyList()
                _recipes.postValue(currentList + results)
                currentOffset += pageSize
            } else if (!isLoadMore) {
                _recipes.postValue(emptyList())
            }
            
            _isLoading.postValue(false)
        }
    }

    // UPDATED: Now accepts optional ingredients string to find BEST matches for Home
    fun fetchHomeRecipes(ingredients: String? = null) {
        if (!_homeRecipes.value.isNullOrEmpty() && ingredients == null) return 
        
        homeJob?.cancel()
        homeJob = viewModelScope.launch {
            Log.d("DiscoverViewModel", "Fetching the absolute BEST matches for Home...")
            // We search using the user's pantry items to ensure the highest matches are returned first
            val results = discoverRepository.searchRecipes(
                ingredients = ingredients,
                number = 100,
                offset = 0
            )
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