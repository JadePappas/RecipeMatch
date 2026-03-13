package com.example.recipematch.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.recipematch.R
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class PantryFragment : Fragment() {

    private val tag = "PantryFragment"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d(tag, "onCreateView called")
        val view = inflater.inflate(R.layout.pantry_fragment, container, false)

        val tabLayout = view.findViewById<TabLayout>(R.id.pantry_tabs)
        val viewPager = view.findViewById<ViewPager2>(R.id.pantry_view_pager)

        // Set up the adapter
        val adapter = PantryPagerAdapter(this)
        viewPager.adapter = adapter
        
        // FIX for java.lang.IllegalStateException: Fragment no longer exists for key f#0
        // This prevents the ViewPager2 from trying to restore fragments that were destroyed.
        viewPager.isSaveEnabled = false

        // Connect the TabLayout and ViewPager2
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = if (position == 0) "Ingredients" else "Equipment"
        }.attach()

        return view
    }

    private class PantryPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
        override fun getItemCount(): Int = 2

        override fun createFragment(position: Int): Fragment {
            return if (position == 0) IngredientsFragment() else EquipmentFragment()
        }
    }

    // --- Lifecycle Logs ---
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); Log.d(tag, "onCreate called") }
    override fun onStart() { super.onStart(); Log.d(tag, "onStart called") }
    override fun onResume() { super.onResume(); Log.d(tag, "onResume called") }
    override fun onPause() { Log.d(tag, "onPause called"); super.onPause() }
    override fun onStop() { Log.d(tag, "onStop called"); super.onStop() }
    override fun onDestroyView() { Log.d(tag, "onDestroyView called"); super.onDestroyView() }
    override fun onDestroy() { Log.d(tag, "onDestroy called"); super.onDestroy() }
}