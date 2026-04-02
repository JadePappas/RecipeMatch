package com.example.recipematch.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.recipematch.R
import com.example.recipematch.model.Ingredient
import com.example.recipematch.model.Recipe
import com.example.recipematch.viewmodel.DiscoverViewModel
import com.example.recipematch.viewmodel.PantryViewModel

class DiscoverFragment : Fragment() {

    private lateinit var discoverViewModel: DiscoverViewModel
    private lateinit var pantryViewModel: PantryViewModel
    private lateinit var recipeAdapter: RecipeAdapter
    
    private lateinit var progressBar: ProgressBar
    private lateinit var detailContainer: FrameLayout
    private lateinit var rvRecipes: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.discover_fragment, container, false)
        
        discoverViewModel = ViewModelProvider(this).get(DiscoverViewModel::class.java)
        pantryViewModel = ViewModelProvider(requireActivity()).get(PantryViewModel::class.java)

        progressBar = view.findViewById(R.id.pb_discover_loading)
        detailContainer = view.findViewById(R.id.recipe_detail_container)
        rvRecipes = view.findViewById(R.id.rv_discover_recipes)

        rvRecipes.layoutManager = GridLayoutManager(requireContext(), 2)
        recipeAdapter = RecipeAdapter { recipe ->
            showRecipeDetail(recipe)
        }
        rvRecipes.adapter = recipeAdapter

        val etSearch = view.findViewById<EditText>(R.id.et_search_recipe)
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s.isNullOrBlank()) {
                    discoverViewModel.searchRecipes()
                } else {
                    discoverViewModel.searchRecipes(query = s.toString())
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        discoverViewModel.recipes.observe(viewLifecycleOwner) { recipes ->
            recipeAdapter.submitList(recipes)
        }

        discoverViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        // Initial search
        discoverViewModel.searchRecipes()

        return view
    }

    private fun showRecipeDetail(recipe: Recipe) {
        detailContainer.visibility = View.VISIBLE
        val detailView = layoutInflater.inflate(R.layout.recipe_detail_fragment, detailContainer, false)
        detailContainer.removeAllViews()
        detailContainer.addView(detailView)

        val btnBack = detailView.findViewById<ImageButton>(R.id.btn_back)
        val tvName = detailView.findViewById<TextView>(R.id.tv_detail_recipe_name)
        val ivImage = detailView.findViewById<ImageView>(R.id.iv_detail_image)
        val tvTime = detailView.findViewById<TextView>(R.id.tv_detail_time)
        val tvServings = detailView.findViewById<TextView>(R.id.tv_detail_servings)
        val tvRating = detailView.findViewById<TextView>(R.id.tv_detail_rating)
        val tvInstructions = detailView.findViewById<TextView>(R.id.tv_detail_instructions)
        val ingredientsContainer = detailView.findViewById<LinearLayout>(R.id.ll_ingredients_container)

        btnBack.setOnClickListener {
            detailContainer.visibility = View.GONE
        }

        tvName.text = recipe.title
        tvTime.text = "${recipe.readyInMinutes} min"
        tvServings.text = recipe.servings.toString()
        tvRating.text = "★ ${String.format("%.1f", recipe.spoonacularScore / 20.0)}"
        
        Glide.with(this).load(recipe.image).into(ivImage)

        // Clean summary for instructions if dedicated instructions are missing
        val instructions = recipe.instructions?.replace(Regex("<[^>]*>"), "") 
            ?: recipe.analyzedInstructions?.firstOrNull()?.steps?.joinToString("\n") { "${it.number}. ${it.step}" }
            ?: "No instructions provided."
        tvInstructions.text = instructions

        // Populate ingredients
        recipe.extendedIngredients?.forEach { ingredient ->
            val item = layoutInflater.inflate(R.layout.item_recipe_ingredient, ingredientsContainer, false)
            val tvIngName = item.findViewById<TextView>(R.id.tv_ingredient_name)
            val btnStatus = item.findViewById<Button>(R.id.btn_ingredient_status)
            
            tvIngName.text = "${ingredient.original}"
            
            // Logic to check if user has ingredient (simplified)
            pantryViewModel.pantryItems.value?.any { 
                it.ingredientName.contains(ingredient.name, ignoreCase = true) 
            }?.let { hasIt ->
                if (hasIt) {
                    btnStatus.text = "In Kitchen"
                    btnStatus.setBackgroundColor(resources.getColor(android.R.color.holo_green_light, null))
                } else {
                    btnStatus.text = "Need to buy"
                    btnStatus.setBackgroundColor(resources.getColor(android.R.color.holo_red_light, null))
                }
            }
            
            ingredientsContainer.addView(item)
        }
        
        // Save/Like logic
        val btnLike = detailView.findViewById<ImageButton>(R.id.btn_like)
        val btnDislike = detailView.findViewById<ImageButton>(R.id.btn_dislike)
        
        btnLike.setOnClickListener {
            Toast.makeText(context, "Liked!", Toast.LENGTH_SHORT).show()
        }
        btnDislike.setOnClickListener {
            Toast.makeText(context, "Disliked", Toast.LENGTH_SHORT).show()
        }
    }
}