package com.example.recipematch.network

import com.example.recipematch.model.Recipe
import com.example.recipematch.model.RecipeResponse
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
        @Query("addRecipeInformation") addRecipeInfo: Boolean = true,
        @Query("fillIngredients") fillIngredients: Boolean = true,
        @Query("number") number: Int = 20
    ): Response<RecipeResponse>

    @GET("recipes/{id}/information")
    suspend fun getRecipeInformation(
        @Path("id") id: Int,
        @Query("apiKey") apiKey: String,
        @Query("includeNutrition") includeNutrition: Boolean = false
    ): Response<Recipe>
}