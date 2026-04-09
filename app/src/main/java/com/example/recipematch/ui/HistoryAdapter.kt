package com.example.recipematch.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.recipematch.R
import com.example.recipematch.model.RecipeAttempt

class HistoryAdapter(private val onItemClick: (RecipeAttempt) -> Unit) : 
    RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    private var items: List<RecipeAttempt> = emptyList()

    fun updateItems(newItems: List<RecipeAttempt>) {
        this.items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_history_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)
        holder.itemView.setOnClickListener { onItemClick(item) }
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val ivPhoto: ImageView = view.findViewById(R.id.iv_history_photo)
        private val tvName: TextView = view.findViewById(R.id.tv_history_recipe_name)
        private val tvDate: TextView = view.findViewById(R.id.tv_history_date)

        fun bind(attempt: RecipeAttempt) {
            tvName.text = attempt.recipeTitle.ifEmpty { "Recipe #${attempt.recipeApiId}" }
            tvDate.text = "Completed: ${attempt.dateCompleted}"
            
            if (attempt.photoUri.isNotEmpty()) {
                Glide.with(ivPhoto.context).load(attempt.photoUri).into(ivPhoto)
            } else {
                ivPhoto.setImageResource(R.drawable.ic_launcher_foreground)
            }
        }
    }
}