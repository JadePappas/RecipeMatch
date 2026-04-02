package com.example.recipematch.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.recipematch.R
import com.example.recipematch.model.PantryItem

class PantryInStockAdapter(private val onEditClick: (PantryItem) -> Unit) :
    ListAdapter<PantryItem, PantryInStockAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pantry_in_stock, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvInfo: TextView = view.findViewById(R.id.tv_item_info)
        private val btnEdit: ImageButton = view.findViewById(R.id.btn_edit_item)

        fun bind(item: PantryItem) {
            tvInfo.text = "${item.ingredientName} ${item.quantity.toInt()} ${item.unit}"
            btnEdit.setOnClickListener { onEditClick(item) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<PantryItem>() {
        override fun areItemsTheSame(oldItem: PantryItem, newItem: PantryItem): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: PantryItem, newItem: PantryItem): Boolean =
            oldItem == newItem
    }
}