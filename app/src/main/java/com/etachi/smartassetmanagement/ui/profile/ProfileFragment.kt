package com.etachi.smartassetmanagement.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.etachi.smartassetmanagement.data.repository.AuthRepository
import com.etachi.smartassetmanagement.databinding.FragmentProfileBinding
import com.etachi.smartassetmanagement.domain.model.Permission
import com.etachi.smartassetmanagement.ui.admin.EditRoleActivity
import com.etachi.smartassetmanagement.ui.login.LoginActivity
import com.etachi.smartassetmanagement.utils.UserSessionManager
import com.etachi.smartassetmanagement.utils.showIfHasPermission
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var authRepository: AuthRepository

    @Inject
    lateinit var sessionManager: UserSessionManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUserData()
        setupAdminControls()
        setupLogoutButton()
    }

    private fun setupUserData() {
        lifecycleScope.launch {
            val firebaseUser = authRepository.getCurrentFirebaseUser()
            val currentUser = sessionManager.getCurrentUser()

            // 1. Set Email
            binding.textEmail.text = firebaseUser?.email ?: "Unknown"

            // 2. Set User ID
            binding.textUserId.text = firebaseUser?.uid ?: "Unknown"

            // 3. Set Role
            val roleName = currentUser?.roleId
                ?.replace("role_", "")
                ?.replaceFirstChar { it.uppercase() }
                ?: "Unknown"
            binding.textUserRole.text = roleName

            // 4. Set Avatar Initial
            val emailFirstChar = firebaseUser?.email?.firstOrNull()?.uppercaseChar() ?: "?"
            binding.textUserInitial.text = emailFirstChar.toString()
        }
    }

    private fun setupAdminControls() {
        // 1. Hide/Show button based on RBAC Permission
        binding.btnManageRoles.showIfHasPermission(sessionManager, Permission.ROLE_MANAGE)

        // 2. Hide/Show the divider below it accordingly
        if (sessionManager.hasPermission(Permission.ROLE_MANAGE)) {
            binding.dividerRoles.visibility = View.VISIBLE
        }

        // 3. Navigate to Create Role Activity
        binding.btnManageRoles.setOnClickListener {
            // No "ROLE_OBJ" extra passed -> EditRoleActivity opens in CREATE NEW mode
            val intent = Intent(requireContext(), EditRoleActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupLogoutButton() {
        binding.btnLogout.setOnClickListener {
            authRepository.logout()
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}