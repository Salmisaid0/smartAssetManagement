package com.etachi.smartassetmanagement.ui.organigramme

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.etachi.smartassetmanagement.databinding.BottomSheetAddDepartmentBinding
import com.etachi.smartassetmanagement.domain.model.Department
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import com.etachi.smartassetmanagement.domain.repository.LocationRepository
import com.etachi.smartassetmanagement.domain.model.Resource

@AndroidEntryPoint
class AddDepartmentSheet : BottomSheetDialogFragment() {

    companion object {
        private const val ARG_DIRECTION_ID = "directionId"
        private const val ARG_DIRECTION_NAME = "directionName"

        fun newInstance(directionId: String, directionName: String): AddDepartmentSheet {
            return AddDepartmentSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_DIRECTION_ID, directionId)
                    putString(ARG_DIRECTION_NAME, directionName)
                }
            }
        }
    }

    @Inject
    lateinit var locationRepository: LocationRepository

    private var _binding: BottomSheetAddDepartmentBinding? = null
    private val binding get() = _binding!!

    private var directionId: String = ""
    private var directionName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        directionId = arguments?.getString(ARG_DIRECTION_ID) ?: ""
        directionName = arguments?.getString(ARG_DIRECTION_NAME) ?: ""

        Timber.d("🏢 AddDepartmentSheet: directionId=$directionId, directionName=$directionName")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetAddDepartmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.textParentName.text = "Parent: $directionName"

        binding.btnSave.setOnClickListener {
            val name = binding.inputName.text?.toString()?.trim() ?: ""
            val code = binding.inputCode.text?.toString()?.trim()?.uppercase() ?: ""

            if (name.isEmpty() || code.isEmpty()) {
                binding.textError.text = "Name and Code are required"
                binding.textError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            binding.btnSave.isEnabled = false
            binding.progressBar.visibility = View.VISIBLE
            binding.textError.visibility = View.GONE

            Timber.d("🏢 Saving department: name=$name, code=$code, dirId=$directionId")

            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val result = locationRepository.createDepartment(
                        directionId,
                        Department(
                            name = name,
                            code = code,
                            directionId = directionId,
                            directionName = directionName,
                            directionCode = "",
                            isActive = true
                        )
                    )

                    when (result) {
                        is Resource.Success -> {
                            Timber.d("✅ [SHEET] Department created: ${result.data}")

                            // Small delay for Firestore propagation
                            kotlinx.coroutines.delay(500)

                            // ✅ CRITICAL: Use parentFragmentManager, NOT childFragmentManager
                            parentFragmentManager.setFragmentResult(
                                "add_result",
                                Bundle().apply {
                                    putBoolean("success", true)
                                    putString("parentId", directionId)
                                    putString("type", "department")
                                }
                            )

                            Timber.d("✅ [SHEET] Fragment result sent")

                            dismiss()
                        }


                        is Resource.Error -> {
                            Timber.e("❌ Department creation failed: ${result.message}")
                            binding.textError.text = result.message ?: "Failed to create department"
                            binding.textError.visibility = View.VISIBLE
                            binding.btnSave.isEnabled = true
                            binding.progressBar.visibility = View.GONE
                        }
                        else -> {}
                    }
                } catch (e: Exception) {
                    Timber.e(e, "❌ Department creation exception")
                    binding.textError.text = e.message ?: "Failed to create department"
                    binding.textError.visibility = View.VISIBLE
                    binding.btnSave.isEnabled = true
                    binding.progressBar.visibility = View.GONE
                }
            }
        }

        binding.btnCancel.setOnClickListener {
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
