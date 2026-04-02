package com.example.recipematch.ui

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.example.recipematch.R
import com.example.recipematch.model.PantryItem
import com.example.recipematch.viewmodel.PantryViewModel

class IngredientsFragment : Fragment() {

    private lateinit var viewModel: PantryViewModel
    private lateinit var inStockAdapter: PantryInStockAdapter
    private lateinit var addItemsAdapter: PantryAddAdapter
    
    private lateinit var tvInStockTitle: TextView
    private val allCommonIngredients = listOf("Pasta", "Rice", "Chicken", "Beef", "Tomato", "Onion", "Garlic", "Salt", "Pepper", "Olive Oil", "Flour", "Sugar", "Milk", "Eggs", "Butter")

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.ingredients_fragment, container, false)
        viewModel = ViewModelProvider(requireActivity()).get(PantryViewModel::class.java)

        tvInStockTitle = view.findViewById(R.id.tv_in_stock_title)
        val rvInStock = view.findViewById<RecyclerView>(R.id.rv_in_stock)
        val rvAddItems = view.findViewById<RecyclerView>(R.id.rv_add_items)

        // In Stock Adapter
        inStockAdapter = PantryInStockAdapter { item ->
            showEditDialog(item)
        }
        rvInStock.adapter = inStockAdapter

        // Add Items Adapter
        addItemsAdapter = PantryAddAdapter(allCommonIngredients) { name ->
            showAddDialog(name)
        }
        rvAddItems.adapter = addItemsAdapter

        viewModel.pantryItems.observe(viewLifecycleOwner) { items ->
            inStockAdapter.submitList(items)
            tvInStockTitle.text = "In Stock (${items.size})"
        }

        val searchBar = view.findViewById<EditText>(R.id.search_ingredients)
        searchBar.setOnEditorActionListener { _, _, _ ->
            val query = searchBar.text.toString().lowercase()
            val filtered = allCommonIngredients.filter { it.lowercase().contains(query) }
            addItemsAdapter.updateItems(filtered)
            true
        }

        return view
    }

    private fun showEditDialog(item: PantryItem) {
        val dialog = Dialog(requireContext())
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_pantry_item, null)
        dialog.setContentView(dialogView)

        val tvName = dialogView.findViewById<TextView>(R.id.tv_item_name)
        val tvQty = dialogView.findViewById<TextView>(R.id.tv_quantity)
        val btnMinus = dialogView.findViewById<ImageButton>(R.id.btn_minus)
        val btnPlus = dialogView.findViewById<ImageButton>(R.id.btn_plus)
        val btnUpdate = dialogView.findViewById<Button>(R.id.btn_update)
        val btnDelete = dialogView.findViewById<ImageButton>(R.id.btn_delete)
        val btnClose = dialogView.findViewById<ImageButton>(R.id.btn_close)

        tvName.text = item.ingredientName
        var currentQty = item.quantity.toInt()
        val unit = item.unit.ifEmpty { "grams" }
        tvQty.text = "$currentQty $unit"

        btnMinus.setOnClickListener {
            if (currentQty > 0) {
                currentQty -= 100
                if (currentQty < 0) currentQty = 0
                tvQty.text = "$currentQty $unit"
            }
        }

        btnPlus.setOnClickListener {
            currentQty += 100
            tvQty.text = "$currentQty $unit"
        }

        btnUpdate.setOnClickListener {
            viewModel.updatePantryItem(item.copy(quantity = currentQty.toDouble()))
            dialog.dismiss()
        }

        btnDelete.setOnClickListener {
            viewModel.deletePantryItem(item.id)
            dialog.dismiss()
        }

        btnClose.setOnClickListener { dialog.dismiss() }

        dialog.show()
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    private fun showAddDialog(name: String) {
        val dialog = Dialog(requireContext())
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_pantry_item, null)
        dialog.setContentView(dialogView)

        val tvName = dialogView.findViewById<TextView>(R.id.tv_item_name)
        val tvQty = dialogView.findViewById<TextView>(R.id.tv_quantity)
        val btnMinus = dialogView.findViewById<ImageButton>(R.id.btn_minus)
        val btnPlus = dialogView.findViewById<ImageButton>(R.id.btn_plus)
        val btnAdd = dialogView.findViewById<Button>(R.id.btn_add_to_pantry)
        val btnClose = dialogView.findViewById<ImageButton>(R.id.btn_close)

        tvName.text = name
        var currentQty = 100
        val unit = "grams"
        tvQty.text = "$currentQty $unit"

        btnMinus.setOnClickListener {
            if (currentQty > 0) {
                currentQty -= 100
                if (currentQty < 0) currentQty = 0
                tvQty.text = "$currentQty $unit"
            }
        }

        btnPlus.setOnClickListener {
            currentQty += 100
            tvQty.text = "$currentQty $unit"
        }

        btnAdd.setOnClickListener {
            viewModel.addPantryItem(name, currentQty.toDouble(), unit)
            dialog.dismiss()
        }

        btnClose.setOnClickListener { dialog.dismiss() }

        dialog.show()
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }
}