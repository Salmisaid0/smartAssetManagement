package com.etachi.smartassetmanagement.ui.admin

import android.os.Bundle
import android.widget.LinearLayout
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.etachi.smartassetmanagement.data.model.Role
import com.etachi.smartassetmanagement.databinding.ActivityEditRoleBinding
import com.etachi.smartassetmanagement.domain.model.Permission
import com.google.android.material.checkbox.MaterialCheckBox
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class EditRoleActivity : AppCompatActivity() {

    // 1. Use ViewBinding instead of findViewById
    private lateinit var binding: ActivityEditRoleBinding

    private val viewModel: RoleManagementViewModel by viewModels()
    private var currentRole: Role? = null

    // 2. Use MaterialCheckBox list instead of standard CheckBox
    private val permissionCheckBoxes = mutableListOf<MaterialCheckBox>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inflate binding
        binding = ActivityEditRoleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Safe extraction of Serializable (No @Suppress needed with safe cast)
        currentRole = intent.getSerializableExtra("ROLE_OBJ") as? Role

        setupUI()
    }

    private fun setupUI() {
        // Pre-fill name if editing
        currentRole?.let {
            binding.inputRoleName.setText(it.name)
        }

        // Dynamically Generate Material Checkboxes for every Permission
        Permission.values().forEach { permission ->

            // 3. Create MaterialCheckBox
            val checkBox = MaterialCheckBox(this).apply {
                text = permission.name.replace("_", " ") // Make it readable: "ASSET_VIEW" -> "ASSET VIEW"
                tag = permission.key

                // Check if this permission exists in currentRole
                currentRole?.permissions?.contains(permission.key)?.let { isChecked = it }

                // 4. Add spacing between dynamically added checkboxes
                val layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                val density = resources.displayMetrics.density
                layoutParams.topMargin = (8 * density).toInt()   // 8dp top margin
                layoutParams.bottomMargin = (8 * density).toInt() // 8dp bottom margin
                this.layoutParams = layoutParams
            }

            binding.containerPermissions.addView(checkBox)
            permissionCheckBoxes.add(checkBox)
        }

        // 5. Save Button via binding
        binding.btnSaveRole.setOnClickListener {
            val name = binding.inputRoleName.text.toString().trim()
            if (name.isEmpty()) return@setOnClickListener

            // Collect checked permissions
            val selectedPermissions = permissionCheckBoxes
                .filter { it.isChecked }
                .mapNotNull { it.tag as? String }

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