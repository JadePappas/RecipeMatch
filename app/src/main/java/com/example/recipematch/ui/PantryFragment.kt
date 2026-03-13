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

class PantryFragment : Fragment() {

    private val tag = "PantryFragment"
    private lateinit var viewModel: PantryViewModel
    private var lastItems: List<PantryItem> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(tag, "onCreate called")
        viewModel = ViewModelProvider(this).get(PantryViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d(tag, "onCreateView called")
        val view = inflater.inflate(R.layout.pantry_fragment, container, false)

        val editName = view.findViewById<EditText>(R.id.edit_ingredient_name)
        val editQuantity = view.findViewById<EditText>(R.id.edit_quantity)
        val editUnit = view.findViewById<EditText>(R.id.edit_unit)
        val itemsTextView = view.findViewById<TextView>(R.id.pantry_items_list)

        val btnAdd = view.findViewById<Button>(R.id.btn_add)
        val btnUpdate = view.findViewById<Button>(R.id.btn_update)
        val btnDelete = view.findViewById<Button>(R.id.btn_delete)

        // CREATE
        btnAdd.setOnClickListener {
            val name = editName.text.toString()
            val qty = editQuantity.text.toString().toDoubleOrNull() ?: 0.0
            val unit = editUnit.text.toString()

            if (name.isNotEmpty()) {
                val newItem = PantryItem(ingredientName = name, quantity = qty, unit = unit)
                viewModel.addPantryItem(newItem)
                Toast.makeText(context, "Added $name", Toast.LENGTH_SHORT).show()
                clearFields(editName, editQuantity, editUnit)
            }
        }

        // UPDATE (For demo: Updates the last added item matching the name)
        btnUpdate.setOnClickListener {
            val name = editName.text.toString()
            val qty = editQuantity.text.toString().toDoubleOrNull() ?: 0.0
            val unit = editUnit.text.toString()
            
            val itemToUpdate = lastItems.find { it.ingredientName == name }
            if (itemToUpdate != null) {
                val updatedItem = itemToUpdate.copy(quantity = qty, unit = unit)
                viewModel.updatePantryItem(updatedItem)
                Toast.makeText(context, "Updated $name", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Item not found to update", Toast.LENGTH_SHORT).show()
            }
        }

        // DELETE (For demo: Deletes the item matching the name in the text field)
        btnDelete.setOnClickListener {
            val name = editName.text.toString()
            val itemToDelete = lastItems.find { it.ingredientName == name }
            if (itemToDelete != null) {
                viewModel.deletePantryItem(itemToDelete.id)
                Toast.makeText(context, "Deleted $name", Toast.LENGTH_SHORT).show()
                clearFields(editName, editQuantity, editUnit)
            } else {
                Toast.makeText(context, "Item not found to delete", Toast.LENGTH_SHORT).show()
            }
        }

        // READ: Observe and display items
        viewModel.pantryItems.observe(viewLifecycleOwner) { items ->
            if (items != null) {
                lastItems = items
                val displayString = items.joinToString("\n") { 
                    "${it.ingredientName}: ${it.quantity} ${it.unit} (ID: ${it.id.take(5)}...)" 
                }
                itemsTextView.text = if (items.isEmpty()) "Pantry is empty" else displayString
                Log.d(tag, "Updated list: ${items.size} items")
            }
        }

        return view
    }

    private fun clearFields(vararg edits: EditText) {
        edits.forEach { it.setText("") }
    }

    // --- Lifecycle Logs ---
    override fun onStart() { super.onStart(); Log.d(tag, "onStart called") }
    override fun onResume() { super.onResume(); Log.d(tag, "onResume called") }
    override fun onPause() { Log.d(tag, "onPause called"); super.onPause() }
    override fun onStop() { Log.d(tag, "onStop called"); super.onStop() }
    override fun onDestroyView() { Log.d(tag, "onDestroyView called"); super.onDestroyView() }
    override fun onDestroy() { Log.d(tag, "onDestroy called"); super.onDestroy() }
}