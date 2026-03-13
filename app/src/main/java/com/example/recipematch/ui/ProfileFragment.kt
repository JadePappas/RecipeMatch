package com.example.recipematch.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.recipematch.R
import com.example.recipematch.viewmodel.UserViewModel

class ProfileFragment : Fragment() {

    private lateinit var viewModel: UserViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.profile_fragment, container, false)
        
        viewModel = ViewModelProvider(this).get(UserViewModel::class.java)

        val tvUsername = view.findViewById<TextView>(R.id.tvProfileUsername)
        val btnLogout = view.findViewById<Button>(R.id.btnLogout)

        // Fetch and observe user data
        viewModel.fetchUserProfile()
        viewModel.userData.observe(viewLifecycleOwner) { user ->
            if (user != null) {
                tvUsername.text = "Hello, ${user.username}!"
            }
        }

        btnLogout.setOnClickListener {
            viewModel.signOut()
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

        return view
    }
}