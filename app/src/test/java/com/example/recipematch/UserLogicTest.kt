package com.example.recipematch

import com.example.recipematch.model.User
import com.example.recipematch.viewmodel.UserViewModel
import org.junit.Assert.assertEquals
import org.junit.Test

class UserLogicTest {

    /**
     * CORE FUNCTIONALITY TEST 1: Level and XP Logic
     * Verifies that the app's ACTUAL calculateProgress method correctly handles XP overflow.
     */
    @Test
    fun user_levelUp_correctlyCalculatesNewXP() {
        // Given a user with 950 XP and 1000 needed to level up
        val user = User(xp = 950, totalXpNeeded = 1000, levelNumber = 5)
        val xpReward = 150
        
        // When calling the actual calculateProgress method from UserViewModel
        val updatedUser = UserViewModel.calculateProgress(user, xpReward)

        // Then user should be Level 6 with 100 XP remaining
        assertEquals("Level should have incremented to 6", 6, updatedUser.levelNumber)
        assertEquals("Remaining XP should be 100 after overflow", 100, updatedUser.xp)
        assertEquals("Title should update to Purple Belt", "Purple Belt", updatedUser.levelTitle)
    }

    /**
     * CORE FUNCTIONALITY TEST 2: Gamification Titles
     * Verifies that the ACTUAL belt title mapping in the app remains consistent.
     */
    @Test
    fun user_beltTitle_mappingCorrectly() {
        // Calling the actual method from UserViewModel
        assertEquals("Level 1 should be White Belt", "White Belt", UserViewModel.getBeltTitle(1))
        assertEquals("Level 4 should be Green Belt", "Green Belt", UserViewModel.getBeltTitle(4))
        assertEquals("Level 8 should be Brown Belt", "Brown Belt", UserViewModel.getBeltTitle(8))
        assertEquals("Level 9+ should be Black Belt", "Black Belt", UserViewModel.getBeltTitle(9))
    }

    /**
     * CORE FUNCTIONALITY TEST 3: Recipe Completion Tracking
     * Verifies that calculateProgress correctly increments the total count of recipes completed.
     */
    @Test
    fun user_progressCalculation_incrementsRecipesCompleted() {
        // Given a user who has completed 10 recipes
        val originalUser = User(recipesCompleted = 10)
        
        // When they earn XP from a new recipe
        val updatedUser = UserViewModel.calculateProgress(originalUser, 100)
        
        // Then the total count must be exactly 11
        assertEquals("Recipe completion count must increment by 1", 11, updatedUser.recipesCompleted)
    }
}