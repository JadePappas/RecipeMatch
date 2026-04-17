package com.example.recipematch.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.recipematch.R
import com.example.recipematch.viewmodel.UserViewModel

class SignupActivity : AppCompatActivity() {

    private lateinit var viewModel: UserViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.signup_activity)

        viewModel = ViewModelProvider(this).get(UserViewModel::class.java)

        val etUsername = findViewById<EditText>(R.id.etUsername)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnSignup = findViewById<Button>(R.id.btnSignup)

        btnSignup.setOnClickListener {
            val username = etUsername.text.toString()
            val email = etEmail.text.toString()
            val password = etPassword.text.toString()

            if (username.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty()) {
                viewModel.signUp(email, password, username)
            } else {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.signupResult.observe(this) { result ->
            val success = result.first
            val message = result.second
            
            if (success) {
                Toast.makeText(this, message ?: "Signup Successful! Check your email.", Toast.LENGTH_LONG).show()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            } else {
                Toast.makeText(this, message ?: "Signup Failed", Toast.LENGTH_SHORT).show()
            }
        }
    }
}