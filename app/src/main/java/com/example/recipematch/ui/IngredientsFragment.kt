package com.example.recipematch.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.recipematch.R
import com.example.recipematch.model.PantryItem
import com.example.recipematch.viewmodel.PantryViewModel

class IngredientsFragment : Fragment() {

    private val tag = "IngredientsFragment"
    private lateinit var viewModel: PantryViewModel
    private var lastItems: List<PantryItem> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this).get(PantryViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.ingredients_fragment, container, false)

        val editName = view.findViewById<EditText>(R.id.edit_ingredient_name)
        val editQuantity = view.findViewById<EditText>(R.id.edit_quantity)
        val editUnit = view.findViewById<EditText>(R.id.edit_unit)
        val itemsTextView = view.findViewById<TextView>(R.id.pantry_items_list)

        val btnAdd = view.findViewById<Button>(R.id.btn_add)
        val btnUpdate = view.findViewById<Button>(R.id.btn_update)
        val btnDelete = view.findViewById<Button>(R.id.btn_delete)

        btnAdd.setOnClickListener {
            val name = editName.text.toString()
            val qty = editQuantity.text.toString().toDoubleOrNull() ?: 0.0
            val unit = editUnit.text.toString()
            if (name.isNotEmpty()) {
                viewModel.addPantryItem(PantryItem(ingredientName = name, quantity = qty, unit = unit))
                clearFields(editName, editQuantity, editUnit)
            }
        }

        btnUpdate.setOnClickListener {
            val name = editName.text.toString()
            val itemToUpdate = lastItems.find { it.ingredientName == name }
            if (itemToUpdate != null) {
                viewModel.updatePantryItem(itemToUpdate.copy(quantity = editQuantity.text.toString().toDoubleOrNull() ?: 0.0, unit = editUnit.text.toString()))
            }
        }

        btnDelete.setOnClickListener {
            val name = editName.text.toString()
            val itemToDelete = lastItems.find { it.ingredientName == name }
            if (itemToDelete != null) {
                viewModel.deletePantryItem(itemToDelete.id)
            }
        }

        viewModel.pantryItems.observe(viewLifecycleOwner) { items ->
            lastItems = items
            val displayString = items.joinToString("\n") { "${it.ingredientName}: ${it.quantity} ${it.unit}" }
            itemsTextView.text = if (items.isEmpty()) "Pantry is empty" else displayString
        }

        return view
    }

    private fun clearFields(vararg edits: EditText) { edits.forEach { it.setText("") } }
}