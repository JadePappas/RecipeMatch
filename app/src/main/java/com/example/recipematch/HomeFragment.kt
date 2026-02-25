package com.example.recipematch

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment

class HomeFragment : Fragment() {
    private val tag = "HomeFragment"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(tag, "onCreate called")
    }

    // inflate fragment's xml layout
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d(tag, "onCreateView called")
        val view = inflater.inflate(R.layout.home_fragment, container, false)

        // temporary button to navigate to DiscoverFragment
        view.findViewById<Button>(R.id.go_to_discover).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, DiscoverFragment())
                .addToBackStack(null)  // allow back navigation
                .commit()
        }

        return view
    }

    // TODO: Lifecycle logging
}