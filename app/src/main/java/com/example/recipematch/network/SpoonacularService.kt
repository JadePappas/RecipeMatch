package com.example.recipematch.network

import com.example.recipematch.model.Recipe
import com.example.recipematch.model.RecipeResponse
import com.example.recipematch.model.AnalyzedInstruction
import com.example.recipematch.model.IngredientSearchResponse
import com.example.recipematch.model.IngredientSearchResult
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface SpoonacularService {

    @GET("recipes/complexSearch")
    suspend fun searchRecipes(
        @Query("apiKey") apiKey: String,
        @Query("query") query: String?,
        @Query("includeIngredients") ingredients: String?,
        @Query("equipment") equipment: String?,
        @Query("type") type: String?,
        @Query("diet") diet: String?,
        @Query("cuisine") cuisine: String?,
        @Query("addRecipeInformation") addRecipeInfo: Boolean = true,
        @Query("fillIngredients") fillIngredients: Boolean = true,
        @Query("number") number: Int = 20,
        @Query("offset") offset: Int = 0 // Added for pagination
    ): Response<RecipeResponse>

    @GET("recipes/{id}/information")
    suspend fun getRecipeInformation(
        @Path("id") id: Int,
        @Query("apiKey") apiKey: String,
        @Query("includeNutrition") includeNutrition: Boolean = false
    ): Response<Recipe>

    @GET("recipes/{id}/analyzedInstructions")
    suspend fun getAnalyzedInstructions(
        @Path("id") id: Int,
        @Query("apiKey") apiKey: String
    ): Response<List<AnalyzedInstruction>>

    @GET("food/ingredients/{id}/information")
    suspend fun getIngredientInformation(
        @Path("id") id: Int,
        @Query("apiKey") apiKey: String,
        @Query("amount") amount: Double = 1.0
    ): Response<IngredientSearchResult>

    @GET("food/ingredients/search")
    suspend fun searchIngredients(
        @Query("apiKey") apiKey: String,
        @Query("query") query: String,
        @Query("number") number: Int = 15
    ): Response<IngredientSearchResponse>

    @GET("food/equipment/search")
    suspend fun searchEquipment(
        @Query("apiKey") apiKey: String,
        @Query("query") query: String,
        @Query("number") number: Int = 15
    ): Response<IngredientSearchResponse>
}