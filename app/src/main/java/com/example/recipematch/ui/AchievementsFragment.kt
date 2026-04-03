package com.example.recipematch.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.recipematch.R
import com.example.recipematch.model.Badge
import com.example.recipematch.viewmodel.UserViewModel

class AchievementsFragment : Fragment() {

    private lateinit var userViewModel: UserViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.achievements_fragment, container, false)
        userViewModel = ViewModelProvider(requireActivity()).get(UserViewModel::class.java)

        val btnBack = view.findViewById<ImageButton>(R.id.btn_achievements_back)
        val rvAchievements = view.findViewById<RecyclerView>(R.id.rv_full_achievements)

        btnBack.setOnClickListener { parentFragmentManager.popBackStack() }

        rvAchievements.layoutManager = GridLayoutManager(context, 3)

        userViewModel.userData.observe(viewLifecycleOwner) { user ->
            user?.let {
                val allBadges = listOf(
                    Badge("early_bird", "Early Bird", "Complete your first recipe!", 1),
                    Badge("italian_master", "Italian Master", "Cook 5 Italian recipes", 5, "Italian"),
                    Badge("mexican_master", "Mexican Master", "Cook 5 Mexican recipes", 5, "Mexican"),
                    Badge("asian_master", "Asian Master", "Cook 5 Asian recipes", 5, "Asian"),
                    Badge("french_master", "French Master", "Cook 5 French recipes", 5, "French"),
                    Badge("american_master", "American Master", "Cook 5 American recipes", 5, "American"),
                    Badge("indian_master", "Indian Master", "Cook 5 Indian recipes", 5, "Indian"),
                    Badge("mediterranean_master", "Mediterranean Master", "Cook 5 Mediterranean recipes", 5, "Mediterranean"),
                    Badge("master_rank", "Black Belt", "Reach 8,000 total XP", 80)
                )

                val unlockedIds = mutableSetOf<String>()
                if (it.recipesCompleted >= 1) unlockedIds.add("early_bird")
                if (it.recipesCompleted >= 5) unlockedIds.add("italian_master") // Simplified logic
                if (it.levelTitle == "Black Belt") unlockedIds.add("master_rank")
                
                rvAchievements.adapter = AchievementAdapter(allBadges, unlockedIds)
            }
        }

        return view
    }
}