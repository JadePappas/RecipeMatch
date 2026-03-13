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
import com.example.recipematch.model.UserEquipment
import com.example.recipematch.viewmodel.UserEquipmentViewModel

class EquipmentFragment : Fragment() {

    private val tag = "EquipmentFragment"
    private lateinit var viewModel: UserEquipmentViewModel
    private var lastItems: List<UserEquipment> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this).get(UserEquipmentViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.equipment_fragment, container, false)

        val editName = view.findViewById<EditText>(R.id.edit_equipment_name)
        val itemsTextView = view.findViewById<TextView>(R.id.equipment_items_list)

        val btnAdd = view.findViewById<Button>(R.id.btn_add_equip)
        val btnUpdate = view.findViewById<Button>(R.id.btn_update_equip)
        val btnDelete = view.findViewById<Button>(R.id.btn_delete_equip)

        btnAdd.setOnClickListener {
            val name = editName.text.toString()
            if (name.isNotEmpty()) {
                viewModel.addEquipmentItem(UserEquipment(equipmentName = name))
                editName.setText("")
                Toast.makeText(context, "Added $name", Toast.LENGTH_SHORT).show()
            }
        }

        btnUpdate.setOnClickListener {
            val name = editName.text.toString()
            val itemToUpdate = lastItems.find { it.equipmentName == name }
            if (itemToUpdate != null) {
                // In this simple case, update might just re-save the same name, 
                // but this shows the CRUD pattern.
                viewModel.updateEquipmentItem(itemToUpdate)
                Toast.makeText(context, "Updated $name", Toast.LENGTH_SHORT).show()
            }
        }

        btnDelete.setOnClickListener {
            val name = editName.text.toString()
            val itemToDelete = lastItems.find { it.equipmentName == name }
            if (itemToDelete != null) {
                viewModel.deleteEquipmentItem(itemToDelete.id)
                Toast.makeText(context, "Deleted $name", Toast.LENGTH_SHORT).show()
                editName.setText("")
            }
        }

        viewModel.equipmentItems.observe(viewLifecycleOwner) { items ->
            lastItems = items
            val displayString = items.joinToString("\n") { it.equipmentName }
            itemsTextView.text = if (items.isEmpty()) "No equipment added" else displayString
        }

        return view
    }
}