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
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

class IngredientsFragment : Fragment() {

    private lateinit var viewModel: PantryViewModel
    private lateinit var inStockAdapter: PantryInStockAdapter
    private lateinit var addItemsAdapter: PantryAddAdapter
    
    private lateinit var tvInStockTitle: TextView
    private lateinit var rvInStock: RecyclerView
    private var isStockExpanded = false

    private val allCommonIngredients = listOf("Pasta", "Rice", "Chicken", "Beef", "Tomato", "Onion", "Garlic", "Salt", "Pepper", "Olive Oil", "Flour", "Sugar", "Milk", "Eggs", "Butter", "Cheese", "Potato", "Carrot", "Lemon", "Ginger", "Cilantro")
    
    private val ingredientCategories = mapOf(
        "Vegan" to listOf("Pasta", "Rice", "Tomato", "Onion", "Garlic", "Salt", "Pepper", "Olive Oil", "Flour", "Sugar", "Potato", "Carrot", "Lemon", "Ginger", "Cilantro"),
        "Protein" to listOf("Chicken", "Beef", "Eggs", "Milk", "Cheese"),
        "Dairy" to listOf("Milk", "Butter", "Cheese", "Eggs"),
        "Spice" to listOf("Salt", "Pepper", "Garlic", "Ginger")
    )

    private val ingredientUnits = mapOf(
        "Milk" to "cups",
        "Eggs" to "count",
        "Butter" to "tbsp",
        "Olive Oil" to "tbsp",
        "Pasta" to "grams",
        "Rice" to "grams",
        "Chicken" to "grams",
        "Beef" to "grams",
        "Flour" to "cups",
        "Sugar" to "cups",
        "Salt" to "tsp",
        "Pepper" to "tsp"
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.ingredients_fragment, container, false)
        viewModel = ViewModelProvider(requireActivity()).get(PantryViewModel::class.java)

        tvInStockTitle = view.findViewById(R.id.tv_in_stock_title)
        rvInStock = view.findViewById(R.id.rv_in_stock)
        val rvAddItems = view.findViewById<RecyclerView>(R.id.rv_add_items)
        val chipGroup = view.findViewById<ChipGroup>(R.id.chip_group_filters)
        val btnViewAllStock = view.findViewById<Button>(R.id.btn_view_all_stock)

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
        searchBar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterIngredients(s.toString(), chipGroup)
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        chipGroup.setOnCheckedStateChangeListener { group, _ ->
            filterIngredients(searchBar.text.toString(), group)
        }

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

    private fun filterIngredients(query: String, chipGroup: ChipGroup) {
        val checkedId = chipGroup.checkedChipId
        val category = if (checkedId != View.NO_ID) {
            chipGroup.findViewById<Chip>(checkedId).text.toString()
        } else "All"

        var filtered = if (category == "All") {
            allCommonIngredients
        } else {
            ingredientCategories[category] ?: allCommonIngredients
        }

        if (query.isNotEmpty()) {
            filtered = filtered.filter { it.contains(query, ignoreCase = true) }
        }
        addItemsAdapter.updateItems(filtered)
    }

    private fun showEditDialog(item: PantryItem) {
        val dialog = Dialog(requireContext())
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_pantry_item, null)
        dialog.setContentView(dialogView)

        val tvName = dialogView.findViewById<TextView>(R.id.tv_item_name)
        val etQty = dialogView.findViewById<EditText>(R.id.et_quantity)
        val tvUnit = dialogView.findViewById<TextView>(R.id.tv_unit)
        val btnMinus = dialogView.findViewById<ImageButton>(R.id.btn_minus)
        val btnPlus = dialogView.findViewById<ImageButton>(R.id.btn_plus)
        val btnUpdate = dialogView.findViewById<Button>(R.id.btn_update)
        val btnDelete = dialogView.findViewById<ImageButton>(R.id.btn_delete)
        val btnClose = dialogView.findViewById<ImageButton>(R.id.btn_close)

        tvName.text = item.ingredientName
        etQty.setText(item.quantity.toString())
        val unit = item.unit.ifEmpty { ingredientUnits[item.ingredientName] ?: "units" }
        tvUnit.text = unit

        btnMinus.setOnClickListener {
            val current = etQty.text.toString().toDoubleOrNull() ?: 0.0
            if (current > 0) {
                etQty.setText((current - 1).toString())
            }
        }

        btnPlus.setOnClickListener {
            val current = etQty.text.toString().toDoubleOrNull() ?: 0.0
            etQty.setText((current + 1).toString())
        }

        btnUpdate.setOnClickListener {
            val newQty = etQty.text.toString().toDoubleOrNull() ?: item.quantity
            viewModel.updatePantryItem(item.copy(quantity = newQty, unit = unit))
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
        val btnMinus = dialogView.findViewById<ImageButton>(R.id.btn_minus)
        val btnPlus = dialogView.findViewById<ImageButton>(R.id.btn_plus)
        val btnAdd = dialogView.findViewById<Button>(R.id.btn_add_to_pantry)
        val btnClose = dialogView.findViewById<ImageButton>(R.id.btn_close)

        tvName.text = name
        val unit = ingredientUnits[name] ?: "units"
        tvUnit.text = unit
        etQty.setText("1")

        btnMinus.setOnClickListener {
            val current = etQty.text.toString().toDoubleOrNull() ?: 0.0
            if (current > 0) {
                etQty.setText((current - 1).toString())
            }
        }

        btnPlus.setOnClickListener {
            val current = etQty.text.toString().toDoubleOrNull() ?: 0.0
            etQty.setText((current + 1).toString())
        }

        btnAdd.setOnClickListener {
            val qty = etQty.text.toString().toDoubleOrNull() ?: 1.0
            viewModel.addPantryItem(name, qty, unit)
            dialog.dismiss()
        }

        btnClose.setOnClickListener { dialog.dismiss() }

        dialog.show()
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }
}