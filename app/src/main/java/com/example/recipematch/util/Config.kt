package com.example.recipematch.util

import com.example.recipematch.BuildConfig

object Config {
    // API Key is now pulled from local.properties via BuildConfig
    val SPOONACULAR_API_KEY = BuildConfig.SPOONACULAR_API_KEY
    const val BASE_URL = "https://api.spoonacular.com/"
}