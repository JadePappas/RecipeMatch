package com.example.recipematch.ui

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.recipematch.R
import com.example.recipematch.model.Badge
import com.example.recipematch.model.RecipeAttempt
import com.example.recipematch.viewmodel.AlbumViewModel
import com.example.recipematch.viewmodel.RecipeAttemptViewModel
import com.example.recipematch.viewmodel.UserViewModel
import kotlin.math.ceil

class ProfileFragment : Fragment() {

    private val tag = "ProfileFragment"
    private lateinit var userViewModel: UserViewModel
    private lateinit var attemptViewModel: RecipeAttemptViewModel
    private lateinit var albumViewModel: AlbumViewModel
    
    private lateinit var historyAdapter: HistoryAdapter
    private lateinit var albumAdapter: AlbumAdapter
    
    private var fullHistory: List<RecipeAttempt> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userViewModel = ViewModelProvider(requireActivity()).get(UserViewModel::class.java)
        attemptViewModel = ViewModelProvider(this).get(RecipeAttemptViewModel::class.java)
        albumViewModel = ViewModelProvider(requireActivity()).get(AlbumViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.profile_fragment, container, false)

        val btnSettings = view.findViewById<ImageButton>(R.id.settings_button)
        val btnNewAlbum = view.findViewById<Button>(R.id.new_album_button)
        val btnSeeAllHistory = view.findViewById<TextView>(R.id.see_all_history)
        val btnSeeAllAchievements = view.findViewById<TextView>(R.id.see_all_achievements)
        
        val ivBelt = view.findViewById<ImageView>(R.id.gi_belt)
        val tvName = view.findViewById<TextView>(R.id.profile_name)
        val tvLevelTitle = view.findViewById<TextView>(R.id.profile_level_title)
        val tvXpText = view.findViewById<TextView>(R.id.xp_text)
        val xpProgressBar = view.findViewById<ProgressBar>(R.id.xp_progress_bar)
        val tvNextLevel = view.findViewById<TextView>(R.id.next_level_text)
        val tvRecipesCompleted = view.findViewById<TextView>(R.id.recipes_completed_text)

        // --- User Stats Observation ---
        userViewModel.userData.observe(viewLifecycleOwner) { user ->
            user?.let {
                tvName.text = it.displayName.ifEmpty { it.username.ifEmpty { it.email.substringBefore("@") } }
                tvLevelTitle.text = it.levelTitle
                tvXpText.text = "XP Progress ${it.xp}/${it.totalXpNeeded} xp"
                xpProgressBar.progress = ((it.xp.toFloat() / it.totalXpNeeded) * 100).toInt()
                tvRecipesCompleted.text = "Recipes Completed: ${it.recipesCompleted}"

                val xpRemaining = it.totalXpNeeded - it.xp
                val recipesNeeded = ceil(xpRemaining.toDouble() / 100.0).toInt()
                
                // Set Belt Color based on Level Title
                val beltColor = when (it.levelTitle) {
                    "White Belt" -> "#FFFFFF"
                    "Yellow Belt" -> "#FFEB3B"
                    "Orange Belt" -> "#FF9800"
                    "Green Belt" -> "#4CAF50"
                    "Blue Belt" -> "#2196F3"
                    "Purple Belt" -> "#9C27B0"
                    "Red Belt" -> "#F44336"
                    "Brown Belt" -> "#795548"
                    "Black Belt" -> "#000000"
                    else -> "#FFFFFF"
                }
                ivBelt.setColorFilter(Color.parseColor(beltColor))

                if (it.levelTitle == "Black Belt") {
                    tvNextLevel.text = "You are a Master!"
                } else {
                    tvNextLevel.text = if (recipesNeeded <= 1) "Cook 1 more recipe to level up!" else "Cook $recipesNeeded more recipes to level up!"
                }
                
                setupAchievements(view, it.recipesCompleted, it.levelTitle)
            }
        }

        // --- Cooking History Setup (Short View) ---
        val rvHistory = view.findViewById<RecyclerView>(R.id.history_recycler)
        rvHistory.layoutManager = LinearLayoutManager(context)
        historyAdapter = HistoryAdapter { attempt -> showHistoryDetailDialog(attempt) }
        rvHistory.adapter = historyAdapter
        
        attemptViewModel.cookingHistory.observe(viewLifecycleOwner) { history ->
            fullHistory = history
            btnSeeAllHistory?.visibility = if (history.size > 3) View.VISIBLE else View.GONE
            historyAdapter.updateItems(history.take(3))
        }

        btnSeeAllHistory?.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, CookingHistoryFragment())
                .addToBackStack(null)
                .commit()
        }

        btnSeeAllAchievements?.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, AchievementsFragment())
                .addToBackStack(null)
                .commit()
        }

        // --- Albums Setup ---
        val rvAlbums = view.findViewById<RecyclerView>(R.id.albums_recycler)
        rvAlbums.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        albumAdapter = AlbumAdapter { album -> 
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, AlbumDetailFragment.newInstance(album))
                .addToBackStack(null)
                .commit()
        }
        rvAlbums.adapter = albumAdapter

        albumViewModel.albums.observe(viewLifecycleOwner) { albums ->
            albumAdapter.submitList(albums)
        }

        btnSettings.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, SettingsFragment())
                .addToBackStack(null)
                .commit()
        }

        btnNewAlbum.setOnClickListener { showNewAlbumDialog() }

        return view
    }

    private fun setupAchievements(view: View, recipesCount: Int, beltTitle: String) {
        val rvAchievements = view.findViewById<RecyclerView>(R.id.achievements_recycler)
        rvAchievements.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        
        val allBadges = listOf(
            Badge("early_bird", "Early Bird", "Complete your first recipe!", 1),
            Badge("italian_master", "Italian Master", "Cook 5 Italian recipes", 5, "Italian"),
            Badge("mexican_master", "Mexican Master", "Cook 5 Mexican recipes", 5, "Mexican"),
            Badge("asian_master", "Asian Master", "Cook 5 Asian recipes", 5, "Asian"),
            Badge("master_rank", "Black Belt", "Reach 8,000 total XP", 80)
        )

        val unlockedIds = mutableSetOf<String>()
        if (recipesCount >= 1) unlockedIds.add("early_bird")
        if (recipesCount >= 5) unlockedIds.add("italian_master")
        if (beltTitle == "Black Belt") unlockedIds.add("master_rank")
        
        // Only show 4 badges on the profile preview
        rvAchievements.adapter = AchievementAdapter(allBadges.take(4), unlockedIds)
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
            com.bumptech.glide.Glide.with(ivPhoto.context).load(attempt.photoUri).into(ivPhoto)
        } else {
            ivPhoto.visibility = View.GONE
        }

        btnClose.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun showNewAlbumDialog() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_new_album, null)
        val builder = AlertDialog.Builder(context).setView(dialogView).setCancelable(true)
        val dialog = builder.create()

        dialogView.findViewById<ImageButton>(R.id.btn_close_dialog).setOnClickListener { dialog.dismiss() }
        dialogView.findViewById<Button>(R.id.btn_cancel_album).setOnClickListener { dialog.dismiss() }

        dialogView.findViewById<Button>(R.id.btn_create_album).setOnClickListener {
            val albumName = dialogView.findViewById<EditText>(R.id.edit_album_name).text.toString()
            if (albumName.isNotEmpty()) {
                albumViewModel.createAlbum(albumName)
                Toast.makeText(context, "Album '$albumName' Created", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
        }

        dialog.show()
    }
}