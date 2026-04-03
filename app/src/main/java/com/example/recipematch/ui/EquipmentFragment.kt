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
import com.example.recipematch.model.UserEquipment
import com.example.recipematch.viewmodel.PantryViewModel

class EquipmentFragment : Fragment() {

    private lateinit var viewModel: PantryViewModel
    private lateinit var inKitchenAdapter: EquipmentInStockAdapter
    private lateinit var addEquipmentAdapter: PantryAddAdapter
    
    private lateinit var tvInKitchenTitle: TextView
    private lateinit var tvAddItemsTitle: TextView
    private lateinit var rvInKitchen: RecyclerView
    private var isKitchenExpanded = false

    private val commonEquipment = listOf(
        "Frying Pan", "Saucepan", "Stock Pot", "Baking Sheet", "Oven Mitts",
        "Spatula", "Whisk", "Chef's Knife", "Cutting Board", "Measuring Cups",
        "Measuring Spoons", "Mixing Bowl", "Colander", "Tongs", "Blender",
        "Food Processor", "Toaster", "Microwave", "Air Fryer", "Slow Cooker"
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.equipment_fragment, container, false)
        viewModel = ViewModelProvider(requireActivity()).get(PantryViewModel::class.java)

        tvInKitchenTitle = view.findViewById(R.id.tv_in_kitchen_title)
        tvAddItemsTitle = view.findViewById(R.id.tv_add_items_title)
        rvInKitchen = view.findViewById(R.id.rv_in_kitchen)
        val rvAddEquipment = view.findViewById<RecyclerView>(R.id.rv_add_equipment)
        val btnViewAllKitchen = view.findViewById<Button>(R.id.btn_view_all_kitchen)

        // In Kitchen Adapter
        inKitchenAdapter = EquipmentInStockAdapter { item ->
            showEditDialog(item)
        }
        rvInKitchen.adapter = inKitchenAdapter

        // Add Items Adapter (initially showing common equipment)
        addEquipmentAdapter = PantryAddAdapter(commonEquipment) { name ->
            showAddDialog(name)
        }
        rvAddEquipment.adapter = addEquipmentAdapter
        tvAddItemsTitle.text = "Common Equipment"

        viewModel.equipment.observe(viewLifecycleOwner) { items ->
            inKitchenAdapter.submitList(items)
            tvInKitchenTitle.text = "In Kitchen (${items.size})"
        }

        // Observe search results from Spoonacular API
        viewModel.equipmentSearchResults.observe(viewLifecycleOwner) { results ->
            if (results.isNotEmpty()) {
                val names = results.map { it.name.replaceFirstChar { char -> char.uppercase() } }
                addEquipmentAdapter.updateItems(names)
                tvAddItemsTitle.text = "Search Results (${names.size})"
            } else {
                addEquipmentAdapter.updateItems(commonEquipment)
                tvAddItemsTitle.text = "Common Equipment"
            }
        }

        val searchBar = view.findViewById<EditText>(R.id.search_equipment)
        searchBar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString()
                if (query.length > 2) {
                    viewModel.searchEquipment(query)
                } else if (query.isEmpty()) {
                    addEquipmentAdapter.updateItems(commonEquipment)
                    tvAddItemsTitle.text = "Common Equipment"
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        btnViewAllKitchen.setOnClickListener {
            isKitchenExpanded = !isKitchenExpanded
            if (isKitchenExpanded) {
                rvInKitchen.layoutManager = GridLayoutManager(requireContext(), 2)
                btnViewAllKitchen.text = "Show Less"
            } else {
                rvInKitchen.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
                btnViewAllKitchen.text = "View All"
            }
        }

        return view
    }

    private fun showEditDialog(item: UserEquipment) {
        val dialog = Dialog(requireContext())
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_equipment, null)
        dialog.setContentView(dialogView)

        val tvName = dialogView.findViewById<TextView>(R.id.tv_item_name)
        val btnDelete = dialogView.findViewById<ImageButton>(R.id.btn_delete)
        val btnClose = dialogView.findViewById<ImageButton>(R.id.btn_close)

        tvName.text = item.equipmentName

        btnDelete.setOnClickListener {
            viewModel.deleteEquipment(item.id)
            dialog.dismiss()
        }

        btnClose.setOnClickListener { dialog.dismiss() }

        dialog.show()
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    private fun showAddDialog(name: String) {
        val dialog = Dialog(requireContext())
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_equipment, null)
        dialog.setContentView(dialogView)

        val tvName = dialogView.findViewById<TextView>(R.id.tv_item_name)
        val btnAdd = dialogView.findViewById<Button>(R.id.btn_add_to_kitchen)
        val btnClose = dialogView.findViewById<ImageButton>(R.id.btn_close)

        tvName.text = name

        btnAdd.setOnClickListener {
            viewModel.addEquipment(name)
            dialog.dismiss()
        }

        btnClose.setOnClickListener { dialog.dismiss() }

        dialog.show()
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }
}