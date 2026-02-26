package com.example.recipematch

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

class DiscoverFragment : Fragment() {
    private val tag = "DiscoverFragment"

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
        return inflater.inflate(R.layout.discover_fragment, container, false)
    }

    // TODO: Lifecycle logging
}
