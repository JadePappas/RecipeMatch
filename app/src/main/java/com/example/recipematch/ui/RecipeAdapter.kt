package com.example.recipematch.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.recipematch.R
import com.example.recipematch.model.PantryItem
import com.example.recipematch.model.Recipe
import com.example.recipematch.model.UserEquipment

class RecipeAdapter(private val onRecipeClick: (Recipe) -> Unit) :
    ListAdapter<Recipe, RecipeAdapter.ViewHolder>(DiffCallback()) {

    private var userIngredients: List<PantryItem> = emptyList()
    private var userEquipment: List<UserEquipment> = emptyList()

    fun updateUserData(ingredients: List<PantryItem>, equipment: List<UserEquipment>) {
        this.userIngredients = ingredients
        this.userEquipment = equipment
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recipe_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val ivImage: ImageView = view.findViewById(R.id.iv_recipe_image)
        private val tvName: TextView = view.findViewById(R.id.tv_recipe_name)
        private val tvDescription: TextView = view.findViewById(R.id.tv_recipe_description)
        private val tvCookTime: TextView = view.findViewById(R.id.tv_cook_time)
        private val tvRating: TextView = view.findViewById(R.id.tv_star_rating)
        private val tvMatch: TextView = view.findViewById(R.id.tv_match_percentage)

        fun bind(recipe: Recipe) {
            tvName.text = recipe.title
            tvDescription.text = recipe.summary?.replace(Regex("<[^>]*>"), "") ?: "No description available."
            tvCookTime.text = "Cook time: ${recipe.readyInMinutes} mins"
            tvRating.text = "★ ${String.format("%.1f", recipe.spoonacularScore / 20.0)}"
            
            val matchPercentage = calculateMatchPercentage(recipe)
            tvMatch.text = "$matchPercentage% Match"

            Glide.with(ivImage.context)
                .load(recipe.image)
                .placeholder(android.R.color.darker_gray)
                .into(ivImage)

            itemView.setOnClickListener { onRecipeClick(recipe) }
        }

        private fun calculateMatchPercentage(recipe: Recipe): Int {
            val recipeIngredients = recipe.extendedIngredients ?: emptyList()
            val recipeEquipment = recipe.analyzedInstructions?.flatMap { it.steps }?.flatMap { it.equipment ?: emptyList() }?.distinctBy { it.id } ?: emptyList()

            if (recipeIngredients.isEmpty() && recipeEquipment.isEmpty()) return 0

            var matchedCount = 0
            
            recipeIngredients.forEach { recipeIng ->
                val hasIng = userIngredients.any { userIng ->
                    recipeIng.name.contains(userIng.ingredientName, ignoreCase = true) ||
                    userIng.ingredientName.contains(recipeIng.name, ignoreCase = true)
                }
                if (hasIng) matchedCount++
            }

            recipeEquipment.forEach { reqEq ->
                val hasEq = userEquipment.any { userEq ->
                    reqEq.name.contains(userEq.equipmentName, ignoreCase = true) ||
                    userEq.equipmentName.contains(reqEq.name, ignoreCase = true)
                }
                if (hasEq) matchedCount++
            }

            val totalRequired = recipeIngredients.size + recipeEquipment.size
            return if (totalRequired > 0) {
                (matchedCount * 100) / totalRequired
            } else 0
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Recipe>() {
        override fun areItemsTheSame(oldItem: Recipe, newItem: Recipe): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Recipe, newItem: Recipe): Boolean = oldItem == newItem
    }
}