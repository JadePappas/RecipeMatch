package com.example.recipematch.ui

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.example.recipematch.R
import com.example.recipematch.util.NetworkMonitor
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private val tag = "MainActivity"

    // Use fragment tags for finding existing fragments after rotation
    private val HOME_TAG = "home_fragment"
    private val PANTRY_TAG = "pantry_fragment"
    private val DISCOVER_TAG = "discover_fragment"
    private val PROFILE_TAG = "profile_fragment"

    private lateinit var networkMonitor: NetworkMonitor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(tag, "onCreate called")

        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        val tvNoInternet = findViewById<TextView>(R.id.tv_no_internet)

        networkMonitor = NetworkMonitor(this)
        networkMonitor.observe(this) { isConnected ->
            tvNoInternet.visibility = if (isConnected) View.GONE else View.VISIBLE
        }

        if (savedInstanceState == null) {
            val homeFragment = HomeFragment()
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, homeFragment, HOME_TAG)
                .commit()
        }

        bottomNavigation.setOnItemSelectedListener { item ->
            val (fragment, fragmentTag) = when (item.itemId) {
                R.id.navigation_home -> HomeFragment() to HOME_TAG
                R.id.navigation_pantry -> PantryFragment() to PANTRY_TAG
                R.id.navigation_discover -> DiscoverFragment() to DISCOVER_TAG
                R.id.navigation_profile -> ProfileFragment() to PROFILE_TAG
                else -> return@setOnItemSelectedListener false
            }
            
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment, fragmentTag)
                .commit()
            true
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.fragment_container)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    override fun onStart() {
        super.onStart()
        Log.d(tag, "onStart called")
    }

    override fun onResume() {
        super.onResume()
        Log.d(tag, "onResume called")
    }

    override fun onPause() {
        Log.d(tag, "onPause called")
        super.onPause()
    }

    override fun onStop() {
        Log.d(tag, "onStop called")
        super.onStop()
    }

    override fun onDestroy() {
        Log.d(tag, "onDestroy called")
        super.onDestroy()
    }
}