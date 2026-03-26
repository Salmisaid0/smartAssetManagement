package com.etachi.smartassetmanagement.ui.admin

import android.os.Bundle
import android.widget.CheckBox
import android.widget.LinearLayout
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.etachi.smartassetmanagement.R
import com.etachi.smartassetmanagement.data.model.Role
import com.etachi.smartassetmanagement.domain.model.Permission
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class EditRoleActivity : AppCompatActivity() {

    private val viewModel: RoleManagementViewModel by viewModels()
    private var currentRole: Role? = null
    private val permissionCheckBoxes = mutableListOf<CheckBox>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_role)

        // Get Role from Intent (if editing existing)
        @Suppress("DEPRECATION")
        currentRole = intent.getSerializableExtra("ROLE_OBJ") as? Role

        setupUI()
    }

    private fun setupUI() {
        val inputName = findViewById<TextInputEditText>(R.id.inputRoleName)
        val containerPermissions = findViewById<LinearLayout>(R.id.containerPermissions)

        // 1. Pre-fill name if editing
        currentRole?.let {
            inputName.setText(it.name)
        }

        // 2. Dynamically Generate Checkboxes for every Permission
        Permission.values().forEach { permission ->
            val checkBox = CheckBox(this).apply {
                text = permission.name.replace("_", " ") // Make it readable
                tag = permission.key

                // Check if this permission exists in currentRole
                currentRole?.permissions?.contains(permission.key)?.let { isChecked = it }
            }
            containerPermissions.addView(checkBox)
            permissionCheckBoxes.add(checkBox)
        }

        // 3. Save Button
        findViewById<android.widget.Button>(R.id.btnSaveRole).setOnClickListener {
            val name = inputName.text.toString().trim()
            if (name.isEmpty()) return@setOnClickListener

            // Collect checked permissions
            val selectedPermissions = permissionCheckBoxes
                .filter { it.isChecked }
                .mapNotNull { it.tag as String }

            val roleToSave = Role(
                id = currentRole?.id ?: "", // Keep ID if editing
                name = name,
                permissions = selectedPermissions
            )

            viewModel.saveRole(roleToSave)
            finish()
        }
    }
}