package com.example.recipematch

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment

class DiscoverFragment : Fragment() {

    // inflate fragment's xml layout
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.discover_fragment, container, false)

        // Temporary button to navigate back to HomeFragment
        view.findViewById<Button>(R.id.back_button).setOnClickListener {
            // Pop this fragment off the back stack and return to the previous fragment
            parentFragmentManager.popBackStack()
        }

        return view
    }

    // TODO: Lifecycle logging
}