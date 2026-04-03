package com.example.recipematch.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.recipematch.R
import com.example.recipematch.model.Album
import com.example.recipematch.model.Recipe
import com.example.recipematch.viewmodel.AlbumViewModel
import com.example.recipematch.viewmodel.PantryViewModel

class AlbumDetailFragment : Fragment() {

    private lateinit var albumViewModel: AlbumViewModel
    private lateinit var pantryViewModel: PantryViewModel
    private lateinit var recipeAdapter: RecipeAdapter
    
    private lateinit var detailContainer: FrameLayout
    private var currentAlbum: Album? = null

    companion object {
        fun newInstance(album: Album): AlbumDetailFragment {
            val fragment = AlbumDetailFragment()
            fragment.currentAlbum = album
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.album_detail_fragment, container, false)
        albumViewModel = ViewModelProvider(requireActivity()).get(AlbumViewModel::class.java)
        pantryViewModel = ViewModelProvider(requireActivity()).get(PantryViewModel::class.java)

        val btnBack = view.findViewById<ImageButton>(R.id.btn_album_back)
        val tvTitle = view.findViewById<TextView>(R.id.tv_album_detail_title)
        val rvRecipes = view.findViewById<RecyclerView>(R.id.rv_album_recipes)
        detailContainer = view.findViewById(R.id.album_recipe_detail_container)

        tvTitle.text = currentAlbum?.albumName ?: "Album Details"
        btnBack.setOnClickListener { parentFragmentManager.popBackStack() }

        rvRecipes.layoutManager = GridLayoutManager(requireContext(), 2)
        recipeAdapter = RecipeAdapter { recipe -> showRecipeDetail(recipe) }
        rvRecipes.adapter = recipeAdapter

        pantryViewModel.pantryItems.observe(viewLifecycleOwner) { items ->
            recipeAdapter.updateUserData(items, pantryViewModel.equipment.value ?: emptyList())
        }

        albumViewModel.albumRecipes.observe(viewLifecycleOwner) { recipes ->
            recipeAdapter.submitList(recipes)
        }

        currentAlbum?.let {
            albumViewModel.fetchRecipesForAlbum(it.recipes)
        }

        return view
    }

    private fun showRecipeDetail(recipe: Recipe) {
        detailContainer.visibility = View.VISIBLE
        val detailView = layoutInflater.inflate(R.layout.recipe_detail_fragment, detailContainer, false)
        detailContainer.removeAllViews()
        detailContainer.addView(detailView)

        detailView.findViewById<ImageButton>(R.id.btn_back).setOnClickListener { 
            detailContainer.visibility = View.GONE 
        }

        detailView.findViewById<TextView>(R.id.tv_detail_recipe_name).text = recipe.title
        detailView.findViewById<TextView>(R.id.tv_detail_time).text = "${recipe.readyInMinutes} min"
        detailView.findViewById<TextView>(R.id.tv_detail_servings).text = recipe.servings.toString()
        detailView.findViewById<TextView>(R.id.tv_detail_rating).text = "★ ${String.format("%.1f", recipe.spoonacularScore / 20.0)}"
        
        val ivImage = detailView.findViewById<ImageView>(R.id.iv_detail_image)
        Glide.with(this).load(recipe.image).into(ivImage)

        // Hide completion/album UI for simplicity in this view
        detailView.findViewById<View>(R.id.btn_save_attempt).visibility = View.GONE
        detailView.findViewById<View>(R.id.btn_save_to_album).visibility = View.GONE
        
        populateIngredientsAndInstructions(recipe, detailView)
    }

    private fun populateIngredientsAndInstructions(recipe: Recipe, view: View) {
        val ingContainer = view.findViewById<LinearLayout>(R.id.ll_ingredients_container)
        val insContainer = view.findViewById<LinearLayout>(R.id.ll_instructions_container)

        recipe.extendedIngredients?.forEach { ingredient ->
            val item = layoutInflater.inflate(R.layout.item_recipe_ingredient, ingContainer, false)
            item.findViewById<TextView>(R.id.tv_ingredient_name).text = ingredient.original
            
            val btnStatus = item.findViewById<Button>(R.id.btn_ingredient_status)
            val userHas = pantryViewModel.pantryItems.value?.any { 
                ingredient.name.contains(it.ingredientName, true) || it.ingredientName.contains(ingredient.name, true)
            } ?: false
            
            btnStatus.text = if (userHas) "In Kitchen" else "Need to buy"
            btnStatus.setBackgroundColor(ContextCompat.getColor(requireContext(), 
                if (userHas) android.R.color.holo_green_light else android.R.color.holo_red_light))

            ingContainer.addView(item)
        }

        recipe.analyzedInstructions?.firstOrNull()?.steps?.forEach { step ->
            val stepView = layoutInflater.inflate(R.layout.item_instruction_step, insContainer, false)
            stepView.findViewById<TextView>(R.id.tv_step_number).text = "${step.number}."
            stepView.findViewById<TextView>(R.id.tv_step_description).text = step.step
            insContainer.addView(stepView)
        }
    }
}