package com.example.recipematch

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

class PantryFragment : Fragment() {

    private val tag = "PantryFragment"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(tag, "onCreate called")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d(tag, "onCreateView called")
        return inflater.inflate(R.layout.pantry_fragment, container, false)
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

    override fun onDestroyView() {
        Log.d(tag, "onDestroyView called")
        super.onDestroyView()
    }

    override fun onDestroy() {
        Log.d(tag, "onDestroy called")
        super.onDestroy()
    }
}