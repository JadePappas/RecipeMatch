package com.example.recipematch.repository

import android.util.Log
import com.example.recipematch.model.AnalyzedInstruction
import com.example.recipematch.model.Recipe
import com.example.recipematch.network.SpoonacularService
import com.example.recipematch.util.Config
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class DiscoverRepository {

    private val retrofit = Retrofit.Builder()
        .baseUrl(Config.BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val service = retrofit.create(SpoonacularService::class.java)

    suspend fun searchRecipes(
        query: String? = null,
        ingredients: String? = null,
        equipment: String? = null,
        diet: String? = null,
        type: String? = null,
        cuisine: String? = null
    ): List<Recipe>? {
        return try {
            Log.d("DiscoverRepository", "API Key used: ${Config.SPOONACULAR_API_KEY.take(5)}***")
            val response = service.searchRecipes(
                apiKey = Config.SPOONACULAR_API_KEY,
                query = query,
                ingredients = ingredients,
                equipment = equipment,
                type = type,
                diet = diet,
                cuisine = cuisine
            )
            if (response.isSuccessful) {
                response.body()?.results
            } else {
                Log.e("DiscoverRepository", "Search error: ${response.code()} ${response.message()}")
                Log.e("DiscoverRepository", "Error body: ${response.errorBody()?.string()}")
                null
            }
        } catch (e: Exception) {
            Log.e("DiscoverRepository", "Exception during search: ${e.message}")
            null
        }
    }

    suspend fun getRecipeInformation(id: Int): Recipe? {
        return try {
            val response = service.getRecipeInformation(id, Config.SPOONACULAR_API_KEY)
            if (response.isSuccessful) {
                response.body()
            } else {
                Log.e("DiscoverRepository", "Info error: ${response.code()} ${response.message()}")
                null
            }
        } catch (e: Exception) {
            Log.e("DiscoverRepository", "Exception during info fetch: ${e.message}")
            null
        }
    }

    suspend fun getAnalyzedInstructions(id: Int): List<AnalyzedInstruction>? {
        return try {
            val response = service.getAnalyzedInstructions(id, Config.SPOONACULAR_API_KEY)
            if (response.isSuccessful) {
                response.body()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}