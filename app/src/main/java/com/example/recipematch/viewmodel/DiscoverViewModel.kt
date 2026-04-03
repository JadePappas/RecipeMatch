package com.example.recipematch.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.recipematch.model.Recipe
import com.example.recipematch.repository.DiscoverRepository
import com.example.recipematch.util.Config
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class DiscoverViewModel : ViewModel() {

    private val discoverRepository = DiscoverRepository()
    private val auth = FirebaseAuth.getInstance()

    private val _recipes = MutableLiveData<List<Recipe>>()
    val recipes: LiveData<List<Recipe>> = _recipes

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _selectedRecipe = MutableLiveData<Recipe?>()
    val selectedRecipe: LiveData<Recipe?> = _selectedRecipe

    private var currentQuery: String? = null
    private var currentCuisine: String? = null
    private var currentDiet: String? = null
    private var currentType: String? = null
    
    private var searchJob: Job? = null

    fun searchRecipes(
        query: String? = currentQuery,
        cuisine: String? = currentCuisine,
        diet: String? = currentDiet,
        type: String? = currentType
    ) {
        // Only trigger search if parameters have actually changed
        if (query == currentQuery && cuisine == currentCuisine && diet == currentDiet && type == currentType && _recipes.value != null) {
            return
        }

        currentQuery = query
        currentCuisine = cuisine
        currentDiet = diet
        currentType = type

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            // Debounce search to avoid too many API calls while typing
            delay(500)
            
            _isLoading.value = true
            val results = discoverRepository.searchRecipes(
                query = query,
                cuisine = cuisine,
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