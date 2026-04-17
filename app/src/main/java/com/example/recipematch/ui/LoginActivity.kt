package com.example.recipematch.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.recipematch.R
import com.example.recipematch.viewmodel.UserViewModel

class LoginActivity : AppCompatActivity() {

    private lateinit var viewModel: UserViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        viewModel = ViewModelProvider(this).get(UserViewModel::class.java)

        // Check if user is already logged in and verified
        if (viewModel.isUserLoggedIn()) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.login_activity)

        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvForgotPassword = findViewById<TextView>(R.id.tvForgotPassword)
        val tvGoToSignup = findViewById<TextView>(R.id.tvGoToSignup)

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString()
            val password = etPassword.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                viewModel.signIn(email, password)
            } else {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            }
        }

        tvForgotPassword.setOnClickListener {
            val email = etEmail.text.toString().trim()
            if (email.isNotEmpty()) {
                viewModel.resetPassword(email)
            } else {
                Toast.makeText(this, "Please enter your email above to reset password.", Toast.LENGTH_SHORT).show()
            }
        }

        tvGoToSignup.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }

        viewModel.loginResult.observe(this) { result ->
            if (result.first) {
                Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            } else {
                Toast.makeText(this, result.second ?: "Login Failed", Toast.LENGTH_LONG).show()
            }
        }

        viewModel.passwordResetResult.observe(this) { result ->
            Toast.makeText(this, result.second, Toast.LENGTH_LONG).show()
        }
    }
}