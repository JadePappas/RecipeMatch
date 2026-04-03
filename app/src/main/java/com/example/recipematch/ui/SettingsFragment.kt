package com.example.recipematch.ui

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.recipematch.R
import com.example.recipematch.viewmodel.UserViewModel

class SettingsFragment : Fragment() {

    private lateinit var userViewModel: UserViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userViewModel = ViewModelProvider(requireActivity()).get(UserViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.settings_fragment, container, false)

        val btnBack = view.findViewById<ImageButton>(R.id.btn_settings_back)
        val btnLogout = view.findViewById<Button>(R.id.btn_logout)
        val btnSave = view.findViewById<Button>(R.id.btn_save_settings)
        val btnChangePassword = view.findViewById<Button>(R.id.btn_change_password)

        val editDisplayName = view.findViewById<EditText>(R.id.edit_display_name)
        val editEmail = view.findViewById<EditText>(R.id.edit_email)

        // Pre-fill fields with current user data
        userViewModel.userData.observe(viewLifecycleOwner) { user ->
            user?.let {
                editDisplayName.setText(it.displayName)
                editEmail.setText(it.email)
            }
        }

        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        btnSave.setOnClickListener {
            val dName = editDisplayName.text.toString()
            val email = editEmail.text.toString()
            userViewModel.updateProfile(dName, email)
        }

        userViewModel.updateResult.observe(viewLifecycleOwner) { success ->
            if (success) {
                Toast.makeText(context, "Profile Updated Successfully", Toast.LENGTH_SHORT).show()
            }
        }

        btnChangePassword.setOnClickListener {
            showChangePasswordDialog()
        }

        btnLogout.setOnClickListener {
            showLogoutDialog()
        }

        return view
    }

    private fun showChangePasswordDialog() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_change_password, null)
        val builder = AlertDialog.Builder(context).setView(dialogView).setCancelable(true)
        val dialog = builder.create()

        val editNewPassword = dialogView.findViewById<EditText>(R.id.edit_new_password)
        val editConfirmPassword = dialogView.findViewById<EditText>(R.id.edit_confirm_password)

        dialogView.findViewById<Button>(R.id.btn_confirm_password_change).setOnClickListener {
            val newPass = editNewPassword.text.toString()
            val confirmPass = editConfirmPassword.text.toString()

            if (newPass.length < 6) {
                Toast.makeText(context, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
            } else if (newPass == confirmPass) {
                userViewModel.updatePassword(newPass)
                dialog.dismiss()
            } else {
                Toast.makeText(context, "Passwords do not match", Toast.LENGTH_SHORT).show()
            }
        }

        dialogView.findViewById<Button>(R.id.btn_cancel_password).setOnClickListener {
            dialog.dismiss()
        }

        userViewModel.passwordUpdateMessage.observe(viewLifecycleOwner) { message ->
            if (message != null) {
                if (message == "Success") {
                    Toast.makeText(context, "Password Updated Successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                }
                // Clear the message so it doesn't show again on next open
                // userViewModel.passwordUpdateMessage.value = null // This would require a public setter or function
            }
        }

        dialog.show()
    }

    private fun showLogoutDialog() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_logout, null)
        val builder = AlertDialog.Builder(context).setView(dialogView).setCancelable(true)
        val dialog = builder.create()

        dialogView.findViewById<Button>(R.id.btn_confirm_logout).setOnClickListener {
            userViewModel.signOut()
            dialog.dismiss()
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish()
        }

        dialogView.findViewById<Button>(R.id.btn_stay_logged_in).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}