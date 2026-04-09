package com.example.recipematch.model

import com.google.gson.annotations.SerializedName

data class RecipeResponse(
    val results: List<Recipe>,
    val offset: Int,
    val number: Int,
    val totalResults: Int
)

data class Recipe(
    val id: Int,
    val title: String,
    val image: String,
    val summary: String? = null,
    val readyInMinutes: Int = 0,
    val servings: Int = 0,
    val spoonacularScore: Double = 0.0,
    val healthScore: Double = 0.0,
    val aggregateLikes: Int = 0,
    val extendedIngredients: List<Ingredient>? = null,
    val instructions: String? = null,
    val analyzedInstructions: List<AnalyzedInstruction>? = null
)

data class Ingredient(
    val id: Int,
    val original: String,
    val name: String,
    val amount: Double,
    val unit: String
)

data class AnalyzedInstruction(
    val name: String,
    val steps: List<Step>
)

data class Step(
    val number: Int,
    val step: String,
    val ingredients: List<Entity>? = null,
    val equipment: List<Entity>? = null
)

data class Entity(
    val id: Int,
    val name: String,
    val localizedName: String,
    val image: String
)

data class IngredientSearchResponse(
    val results: List<IngredientSearchResult>,
    val offset: Int,
    val number: Int,
    val totalResults: Int
)

data class IngredientSearchResult(
    val id: Int,
    val name: String,
    val image: String,
    val possibleUnits: List<String>? = emptyList()
)