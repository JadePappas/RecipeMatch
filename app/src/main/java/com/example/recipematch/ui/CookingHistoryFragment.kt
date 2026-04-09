package com.example.recipematch.ui

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.recipematch.R
import com.example.recipematch.model.RecipeAttempt
import com.example.recipematch.viewmodel.RecipeAttemptViewModel

class CookingHistoryFragment : Fragment() {

    private lateinit var attemptViewModel: RecipeAttemptViewModel
    private lateinit var historyAdapter: HistoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.cooking_history_fragment, container, false)
        attemptViewModel = ViewModelProvider(requireActivity()).get(RecipeAttemptViewModel::class.java)

        val btnBack = view.findViewById<ImageButton>(R.id.btn_history_back)
        val rvHistory = view.findViewById<RecyclerView>(R.id.rv_full_history)

        btnBack.setOnClickListener { parentFragmentManager.popBackStack() }

        rvHistory.layoutManager = LinearLayoutManager(context)
        historyAdapter = HistoryAdapter { attempt -> showHistoryDetailDialog(attempt) }
        rvHistory.adapter = historyAdapter

        attemptViewModel.cookingHistory.observe(viewLifecycleOwner) { history ->
            historyAdapter.updateItems(history)
        }

        return view
    }

    private fun showHistoryDetailDialog(attempt: RecipeAttempt) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_history_detail, null)
        val builder = AlertDialog.Builder(context).setView(dialogView).setCancelable(true)
        val dialog = builder.create()

        val tvTitle = dialogView.findViewById<TextView>(R.id.tv_detail_title)
        val ivPhoto = dialogView.findViewById<ImageView>(R.id.iv_detail_photo)
        val tvNotes = dialogView.findViewById<TextView>(R.id.tv_detail_notes)
        val tvDate = dialogView.findViewById<TextView>(R.id.tv_detail_date)
        val btnClose = dialogView.findViewById<ImageButton>(R.id.btn_close_history_detail)

        tvTitle.text = attempt.recipeTitle.ifEmpty { "Recipe #${attempt.recipeApiId}" }
        tvNotes.text = attempt.notes.ifEmpty { "No notes provided." }
        tvDate.text = "Completed on: ${attempt.dateCompleted}"

        if (attempt.photoUri.isNotEmpty()) {
            ivPhoto.visibility = View.VISIBLE
            Glide.with(ivPhoto.context).load(attempt.photoUri).into(ivPhoto)
        } else {
            ivPhoto.visibility = View.GONE
        }

        btnClose.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }
}