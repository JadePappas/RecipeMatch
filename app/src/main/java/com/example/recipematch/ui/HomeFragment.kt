package com.example.recipematch.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.recipematch.R
import com.example.recipematch.model.Recipe
import com.example.recipematch.viewmodel.DiscoverViewModel
import com.example.recipematch.viewmodel.PantryViewModel
import com.example.recipematch.viewmodel.UserViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.chip.Chip

class HomeFragment : Fragment() {
    private val tag = "HomeFragment"
    private lateinit var userViewModel: UserViewModel
    private lateinit var pantryViewModel: PantryViewModel
    private lateinit var discoverViewModel: DiscoverViewModel
    private lateinit var recommendedAdapter: RecipeAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.home_fragment, container, false)
        
        userViewModel = ViewModelProvider(requireActivity()).get(UserViewModel::class.java)
        pantryViewModel = ViewModelProvider(requireActivity()).get(PantryViewModel::class.java)
        discoverViewModel = ViewModelProvider(requireActivity()).get(DiscoverViewModel::class.java)

        val tvGreeting = view.findViewById<TextView>(R.id.tv_greeting)
        val tvItemsAvailable = view.findViewById<TextView>(R.id.tv_items_available)
        val tvRecipesFound = view.findViewById<TextView>(R.id.tv_recipes_found)
        val btnUpdatePantry = view.findViewById<Button>(R.id.btn_update_pantry)
        val rvRecommended = view.findViewById<RecyclerView>(R.id.rv_recommended)
        val btnSeeAll = view.findViewById<TextView>(R.id.see_all_recommended)

        // Setup horizontal recommended list
        rvRecommended.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        recommendedAdapter = RecipeAdapter { recipe -> showRecipeDetail(recipe) }
        rvRecommended.adapter = recommendedAdapter

        // Personalization
        userViewModel.userData.observe(viewLifecycleOwner) { user ->
            user?.let {
                tvGreeting.text = "Hi, ${it.displayName.ifEmpty { it.username }}!"
            }
        }

        // Pantry Stats & Equipment
        pantryViewModel.pantryItems.observe(viewLifecycleOwner) { items ->
            tvItemsAvailable.text = "You have ${items.size} items available"
            recommendedAdapter.updateUserData(items, pantryViewModel.equipment.value ?: emptyList())
            sortAndSubmitRecommended()
            updateMatchCount()
        }

        pantryViewModel.equipment.observe(viewLifecycleOwner) { equipment ->
            recommendedAdapter.updateUserData(pantryViewModel.pantryItems.value ?: emptyList(), equipment)
            sortAndSubmitRecommended()
            updateMatchCount()
        }

        discoverViewModel.recipes.observe(viewLifecycleOwner) { recipes ->
            sortAndSubmitRecommended()
            updateMatchCount()
        }

        // Navigation
        btnUpdatePantry.setOnClickListener {
            navigateToTab(R.id.navigation_pantry)
        }

        btnSeeAll.setOnClickListener {
            navigateToTab(R.id.navigation_discover)
        }

        setupExploreButtons(view)

        // Automatically trigger search if it hasn't been done yet
        if (discoverViewModel.recipes.value.isNullOrEmpty()) {
            discoverViewModel.searchRecipes()
        }

        return view
    }

    private fun sortAndSubmitRecommended() {
        val recipes = discoverViewModel.recipes.value ?: return
        val userIngs = pantryViewModel.pantryItems.value ?: emptyList()
        val userEqs = pantryViewModel.equipment.value ?: emptyList()

        val sortedList = recipes.sortedByDescending { recipe ->
            calculateMatchPercentage(recipe, userIngs, userEqs)
        }
        recommendedAdapter.submitList(sortedList.take(5))
    }

    private fun updateMatchCount() {
        val recipes = discoverViewModel.recipes.value ?: return
        val userIngs = pantryViewModel.pantryItems.value ?: emptyList()
        val userEqs = pantryViewModel.equipment.value ?: emptyList()

        if (recipes.isEmpty()) {
            view?.findViewById<TextView>(R.id.tv_recipes_found)?.text = "Finding recipes you can make..."
            return
        }

        // Calculate all matches and sort
        val recipeMatchPairs = recipes.map { it to calculateMatchPercentage(it, userIngs, userEqs) }
            .sortedByDescending { it.second }
        
        // Take top 3
        val top3Matches = recipeMatchPairs.take(3)
        val count = top3Matches.size

        val tvFound = view?.findViewById<TextView>(R.id.tv_recipes_found)
        tvFound?.text = "We found your top $count matches →"

        // Make the text clickable to show these specific top 3 matches
        tvFound?.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, BestMatchesFragment.newInstance(top3Matches.map { it.first }))
                .addToBackStack(null)
                .commit()
        }
    }

    private fun setupExploreButtons(view: View) {
        view.findViewById<Chip>(R.id.chip_breakfast).setOnClickListener { searchAndNavigate("Breakfast") }
        view.findViewById<Chip>(R.id.chip_lunch).setOnClickListener { searchAndNavigate("Lunch") }
        view.findViewById<Chip>(R.id.chip_dinner).setOnClickListener { searchAndNavigate("Dinner") }
        view.findViewById<Chip>(R.id.chip_healthy).setOnClickListener { searchAndNavigate("Healthy") }
        view.findViewById<Chip>(R.id.chip_dessert).setOnClickListener { searchAndNavigate("Dessert") }
        view.findViewById<Chip>(R.id.chip_sweet).setOnClickListener { searchAndNavigate("Sweet") }
    }

    private fun searchAndNavigate(category: String) {
        discoverViewModel.searchRecipes(query = category)
        navigateToTab(R.id.navigation_discover)
    }

    private fun navigateToTab(itemId: Int) {
        val nav = requireActivity().findViewById<BottomNavigationView>(R.id.bottom_navigation)
        nav.selectedItemId = itemId
    }

    private fun showRecipeDetail(recipe: Recipe) {
        discoverViewModel.selectRecipe(recipe)
        navigateToTab(R.id.navigation_discover)
    }

    private fun calculateMatchPercentage(recipe: Recipe, userIngs: List<com.example.recipematch.model.PantryItem>, userEqs: List<com.example.recipematch.model.UserEquipment>): Int {
        val recipeIngredients = recipe.extendedIngredients ?: emptyList()
        val recipeEquipment = recipe.analyzedInstructions?.flatMap { it.steps }?.flatMap { it.equipment ?: emptyList() }?.distinctBy { it.id } ?: emptyList()

        if (recipeIngredients.isEmpty() && recipeEquipment.isEmpty()) return 0

        var matchedCount = 0
        recipeIngredients.forEach { recipeIng ->
            if (userIngs.any { it.ingredientName.contains(recipeIng.name, true) || recipeIng.name.contains(it.ingredientName, true) }) matchedCount++
        }
        recipeEquipment.forEach { reqEq ->
            if (userEqs.any { it.equipmentName.contains(reqEq.name, true) || reqEq.name.contains(it.equipmentName, true) }) matchedCount++
        }

        val totalRequired = recipeIngredients.size + recipeEquipment.size
        return if (totalRequired > 0) (matchedCount * 100) / totalRequired else 0
    }
}