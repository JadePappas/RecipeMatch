package com.example.recipematch.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.example.recipematch.model.IngredientSearchResult
import com.example.recipematch.model.PantryItem
import com.example.recipematch.model.UserEquipment
import com.example.recipematch.network.SpoonacularService
import com.example.recipematch.repository.PantryRepository
import com.example.recipematch.util.Config
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class PantryViewModel : ViewModel() {
    private val pantryRepository = PantryRepository()
    private val auth = FirebaseAuth.getInstance()
    
    private val retrofit = Retrofit.Builder()
        .baseUrl(Config.BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    private val spoonacularService = retrofit.create(SpoonacularService::class.java)
    
    private val _userId = MutableLiveData<String?>()
    
    private val _ingredientSearchResults = MutableLiveData<List<IngredientSearchResult>>()
    val ingredientSearchResults: LiveData<List<IngredientSearchResult>> = _ingredientSearchResults

    private val _equipmentSearchResults = MutableLiveData<List<IngredientSearchResult>>()
    val equipmentSearchResults: LiveData<List<IngredientSearchResult>> = _equipmentSearchResults

    private var ingredientSearchJob: Job? = null
    private var equipmentSearchJob: Job? = null

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

    val equipment: LiveData<List<UserEquipment>> = _userId.switchMap { uid ->
        if (uid != null) {
            pantryRepository.getUserEquipment(uid)
        } else {
            MutableLiveData(emptyList())
        }
    }

    fun searchIngredients(query: String) {
        if (query.isBlank()) {
            _ingredientSearchResults.value = emptyList()
            return
        }
        ingredientSearchJob?.cancel()
        ingredientSearchJob = viewModelScope.launch {
            delay(500) // Debounce
            try {
                val response = spoonacularService.searchIngredients(Config.SPOONACULAR_API_KEY, query)
                if (response.isSuccessful) {
                    _ingredientSearchResults.postValue(response.body()?.results ?: emptyList())
                }
            } catch (e: Exception) {}
        }
    }

    fun searchEquipment(query: String) {
        if (query.isBlank()) {
            _equipmentSearchResults.value = emptyList()
            return
        }
        equipmentSearchJob?.cancel()
        equipmentSearchJob = viewModelScope.launch {
            delay(500) // Debounce
            try {
                val response = spoonacularService.searchEquipment(Config.SPOONACULAR_API_KEY, query)
                if (response.isSuccessful) {
                    _equipmentSearchResults.postValue(response.body()?.results ?: emptyList())
                }
            } catch (e: Exception) {}
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

    fun addEquipment(name: String) {
        val uid = auth.currentUser?.uid ?: return
        val newEquipment = UserEquipment(
            userId = uid,
            equipmentName = name
        )
        pantryRepository.addUserEquipment(newEquipment)
    }

    fun deleteEquipment(equipmentId: String) {
        pantryRepository.deleteUserEquipment(equipmentId)
    }
}