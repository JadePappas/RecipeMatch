package com.example.recipematch.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.recipematch.R

class PantryAddAdapter(
    private var items: List<String>,
    private val onAddClick: (String) -> Unit
) : RecyclerView.Adapter<PantryAddAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pantry_add, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    fun updateItems(newItems: List<String>) {
        items = newItems
        notifyDataSetChanged()
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvName: TextView = view.findViewById(R.id.tv_item_name)
        private val btnAdd: ImageButton = view.findViewById(R.id.btn_add_item)

        fun bind(name: String) {
            tvName.text = name
            btnAdd.setOnClickListener { onAddClick(name) }
        }
    }
}