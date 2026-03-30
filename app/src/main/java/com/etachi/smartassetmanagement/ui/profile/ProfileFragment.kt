package com.etachi.smartassetmanagement.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.etachi.smartassetmanagement.R
import com.etachi.smartassetmanagement.data.repository.AuthRepository
import com.etachi.smartassetmanagement.ui.login.LoginActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ProfileFragment : Fragment() {

    @Inject
    lateinit var authRepository: AuthRepository

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val textEmail = view.findViewById<TextView>(R.id.textEmail)
        val textRole = view.findViewById<TextView>(R.id.textUserRole)
        val btnLogout = view.findViewById<Button>(R.id.btnLogout)

        // Fetch current user data
        lifecycleScope.launch {
            val user = authRepository.getCurrentFirebaseUser()
            textEmail.text = user?.email ?: "Unknown"
        }

        // TODO: You can fetch the Role from UserSessionManager if needed

        btnLogout.setOnClickListener {
            authRepository.logout()
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish()
        }
    }
}