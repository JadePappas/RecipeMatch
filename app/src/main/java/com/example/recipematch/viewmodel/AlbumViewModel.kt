package com.example.recipematch.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.recipematch.model.Album
import com.example.recipematch.model.Recipe
import com.example.recipematch.repository.AlbumRepository
import com.example.recipematch.repository.DiscoverRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class AlbumViewModel : ViewModel() {
    private val repository = AlbumRepository()
    private val discoverRepository = DiscoverRepository()
    private val auth = FirebaseAuth.getInstance()

    val albums: LiveData<List<Album>> = 
        repository.getAlbums(auth.currentUser?.uid ?: "")

    private val _albumRecipes = MutableLiveData<List<Recipe>>()
    val albumRecipes: LiveData<List<Recipe>> = _albumRecipes

    fun createAlbum(albumName: String) {
        val userId = auth.currentUser?.uid ?: return
        val newAlbum = Album(
            userId = userId,
            albumName = albumName,
            recipes = emptyList()
        )
        repository.addAlbum(newAlbum) { _ -> }
    }

    fun addRecipeToAlbum(album: Album, recipeId: String, imageUrl: String) {
        if (!album.recipes.contains(recipeId)) {
            val updatedRecipes = album.recipes.toMutableList().apply { add(recipeId) }
            // If this is the first recipe, set it as the cover image
            val coverImage = if (album.coverImageUrl.isEmpty()) imageUrl else album.coverImageUrl
            val updatedAlbum = album.copy(recipes = updatedRecipes, coverImageUrl = coverImage)
            repository.updateAlbum(updatedAlbum) { _ -> }
        }
    }

    fun fetchRecipesForAlbum(recipeIds: List<String>) {
        viewModelScope.launch {
            val recipeList = mutableListOf<Recipe>()
            recipeIds.forEach { id ->
                discoverRepository.getRecipeInformation(id.toInt())?.let {
                    recipeList.add(it)
                }
            }
            _albumRecipes.postValue(recipeList)
        }
    }
}