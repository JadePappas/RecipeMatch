package com.example.recipematch.ui

import android.app.Dialog
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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.recipematch.R
import com.example.recipematch.model.PantryItem
import com.example.recipematch.viewmodel.PantryViewModel

class IngredientsFragment : Fragment() {

    private lateinit var viewModel: PantryViewModel
    private lateinit var inStockAdapter: PantryInStockAdapter
    private lateinit var addItemsAdapter: PantryAddAdapter
    
    private lateinit var tvInStockTitle: TextView
    private lateinit var tvAddItemsTitle: TextView
    private lateinit var rvInStock: RecyclerView
    private var isStockExpanded = false

    private val commonIngredients = listOf(
        "Pasta", "Rice", "Chicken Breast", "Ground Beef", "Tomato", "Yellow Onion", 
        "Garlic", "Salt", "Black Pepper", "Olive Oil", "All-Purpose Flour", "Sugar", 
        "Whole Milk", "Large Eggs", "Unsalted Butter", "Cheddar Cheese", "Russet Potato", 
        "Carrot", "Lemon", "Fresh Ginger", "Cilantro", "Soy Sauce", "Honey", "Red Pepper Flakes",
        "Greek Yogurt", "Spinach", "Bell Pepper", "Cucumber", "Bread", "Chicken Stock"
    )

    private val ingredientUnits = mapOf(
        "Milk" to "cups", "Whole Milk" to "cups", "Large Eggs" to "count", "Unsalted Butter" to "tbsp",
        "Olive Oil" to "tbsp", "Pasta" to "grams", "Rice" to "grams", "Chicken Breast" to "grams",
        "Ground Beef" to "grams", "All-Purpose Flour" to "cups", "Sugar" to "cups", "Salt" to "tsp",
        "Black Pepper" to "tsp", "Chicken Stock" to "ml", "Honey" to "tbsp", "Greek Yogurt" to "cups"
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.ingredients_fragment, container, false)
        viewModel = ViewModelProvider(requireActivity()).get(PantryViewModel::class.java)

        tvInStockTitle = view.findViewById(R.id.tv_in_stock_title)
        tvAddItemsTitle = view.findViewById(R.id.tv_add_items_title)
        rvInStock = view.findViewById(R.id.rv_in_stock)
        val rvAddItems = view.findViewById<RecyclerView>(R.id.rv_add_items)
        val btnViewAllStock = view.findViewById<Button>(R.id.btn_view_all_stock)

        inStockAdapter = PantryInStockAdapter { item -> showEditDialog(item) }
        rvInStock.adapter = inStockAdapter

        addItemsAdapter = PantryAddAdapter(commonIngredients) { name -> showAddDialog(name) }
        rvAddItems.adapter = addItemsAdapter

        viewModel.pantryItems.observe(viewLifecycleOwner) { items ->
            inStockAdapter.submitList(items)
            tvInStockTitle.text = "In Stock (${items.size})"
        }

        viewModel.ingredientSearchResults.observe(viewLifecycleOwner) { results ->
            if (results.isNotEmpty()) {
                val names = results.map { it.name.replaceFirstChar { char -> char.uppercase() } }
                addItemsAdapter.updateItems(names)
                tvAddItemsTitle.text = "Search Results (${names.size})"
            } else {
                addItemsAdapter.updateItems(commonIngredients)
                tvAddItemsTitle.text = "Common Ingredients"
            }
        }

        val searchBar = view.findViewById<EditText>(R.id.search_ingredients)
        searchBar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString()
                if (query.length > 2) {
                    viewModel.searchIngredients(query)
                } else if (query.isEmpty()) {
                    addItemsAdapter.updateItems(commonIngredients)
                    tvAddItemsTitle.text = "Common Ingredients"
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        btnViewAllStock.setOnClickListener {
            isStockExpanded = !isStockExpanded
            if (isStockExpanded) {
                rvInStock.layoutManager = GridLayoutManager(requireContext(), 2)
                btnViewAllStock.text = "Show Less"
            } else {
                rvInStock.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
                btnViewAllStock.text = "View All"
            }
        }

        return view
    }

    private fun showEditDialog(item: PantryItem) {
        val dialog = Dialog(requireContext())
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_pantry_item, null)
        dialog.setContentView(dialogView)

        val tvName = dialogView.findViewById<TextView>(R.id.tv_item_name)
        val etQty = dialogView.findViewById<EditText>(R.id.et_quantity)
        val tvUnit = dialogView.findViewById<TextView>(R.id.tv_unit)
        val btnUpdate = dialogView.findViewById<Button>(R.id.btn_update)
        val btnDelete = dialogView.findViewById<ImageButton>(R.id.btn_delete)
        val btnClose = dialogView.findViewById<ImageButton>(R.id.btn_close)

        tvName.text = item.ingredientName
        etQty.setText(item.quantity.toString())
        tvUnit.text = item.unit.ifEmpty { "units" }

        dialogView.findViewById<ImageButton>(R.id.btn_minus).setOnClickListener {
            val current = etQty.text.toString().toDoubleOrNull() ?: 0.0
            if (current > 0) etQty.setText((current - 1).toString())
        }
        dialogView.findViewById<ImageButton>(R.id.btn_plus).setOnClickListener {
            val current = etQty.text.toString().toDoubleOrNull() ?: 0.0
            etQty.setText((current + 1).toString())
        }

        btnUpdate.setOnClickListener {
            val newQty = etQty.text.toString().toDoubleOrNull() ?: item.quantity
            viewModel.updatePantryItem(item.copy(quantity = newQty))
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
        val etQty = dialogView.findViewById<EditText>(R.id.et_quantity)
        val tvUnit = dialogView.findViewById<TextView>(R.id.tv_unit)
        val btnAdd = dialogView.findViewById<Button>(R.id.btn_add_to_pantry)

        tvName.text = name
        val unit = ingredientUnits[name] ?: "units"
        tvUnit.text = unit
        etQty.setText("1")

        dialogView.findViewById<ImageButton>(R.id.btn_minus).setOnClickListener {
            val current = etQty.text.toString().toDoubleOrNull() ?: 0.0
            if (current > 0) etQty.setText((current - 1).toString())
        }
        dialogView.findViewById<ImageButton>(R.id.btn_plus).setOnClickListener {
            val current = etQty.text.toString().toDoubleOrNull() ?: 0.0
            etQty.setText((current + 1).toString())
        }

        btnAdd.setOnClickListener {
            val qty = etQty.text.toString().toDoubleOrNull() ?: 1.0
            viewModel.addPantryItem(name, qty, unit)
            dialog.dismiss()
        }
        dialogView.findViewById<ImageButton>(R.id.btn_close).setOnClickListener { dialog.dismiss() }

        dialog.show()
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }
}