package com.example.recipematch.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.recipematch.R
import com.example.recipematch.model.Badge

class AchievementAdapter(
    private val badges: List<Badge>,
    private val unlockedBadgeIds: Set<String>
) : RecyclerView.Adapter<AchievementAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivBadge: ImageView = view.findViewById(R.id.iv_badge_icon)
        val tvName: TextView = view.findViewById(R.id.tv_badge_name)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_achievement, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val badge = badges[position]
        val isUnlocked = unlockedBadgeIds.contains(badge.badgeId)

        holder.tvName.text = badge.badgeName
        
        // Ensure we are using the new star icons
        holder.ivBadge.setImageResource(
            if (isUnlocked) R.drawable.ic_achievement_unlocked 
            else R.drawable.ic_achievement_locked
        )

        holder.itemView.setOnClickListener {
            val status = if (isUnlocked) "Unlocked!" else "Locked"
            Toast.makeText(holder.itemView.context, "$status: ${badge.description}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun getItemCount(): Int = badges.size
}