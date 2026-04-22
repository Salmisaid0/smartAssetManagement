package com.etachi.smartassetmanagement.ui.organigramme

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.etachi.smartassetmanagement.databinding.BottomSheetAddRoomBinding
import com.etachi.smartassetmanagement.domain.model.Room
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import com.etachi.smartassetmanagement.domain.repository.LocationRepository
import com.etachi.smartassetmanagement.domain.model.Resource

@AndroidEntryPoint
class AddRoomSheet : BottomSheetDialogFragment() {

    companion object {
        private const val ARG_DEPARTMENT_ID = "departmentId"
        private const val ARG_DEPARTMENT_NAME = "departmentName"

        fun newInstance(departmentId: String, departmentName: String): AddRoomSheet {
            return AddRoomSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_DEPARTMENT_ID, departmentId)
                    putString(ARG_DEPARTMENT_NAME, departmentName)
                }
            }
        }
    }

    @Inject
    lateinit var locationRepository: LocationRepository

    private var _binding: BottomSheetAddRoomBinding? = null
    private val binding get() = _binding!!

    private var departmentId: String = ""
    private var departmentName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        departmentId = arguments?.getString(ARG_DEPARTMENT_ID) ?: ""
        departmentName = arguments?.getString(ARG_DEPARTMENT_NAME) ?: ""

        Timber.d("🏠 AddRoomSheet: departmentId=$departmentId, departmentName=$departmentName")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetAddRoomBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.textParentName.text = "Parent: $departmentName"

        binding.btnSave.setOnClickListener {
            val name = binding.inputName.text?.toString()?.trim() ?: ""
            val code = binding.inputCode.text?.toString()?.trim()?.uppercase() ?: ""

            if (name.isEmpty() || code.isEmpty()) {
                binding.textError.text = "Name and Code are required"
                binding.textError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            // ✅ Disable button during save
            binding.btnSave.isEnabled = false
            binding.progressBar.visibility = View.VISIBLE
            binding.textError.visibility = View.GONE

            Timber.d("🏠 Saving room: name=$name, code=$code, deptId=$departmentId")

            // ✅ Save room to Firestore
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val result = locationRepository.createRoom(
                        departmentId,
                        Room(
                            name = name,
                            code = code,
                            departmentId = departmentId,
                            departmentCode = "",  // Will be filled by repository
                            departmentName = departmentName,
                            directionId = "",     // Will be filled by repository
                            directionCode = "",   // Will be filled by repository
                            directionName = "",   // Will be filled by repository
                            isActive = true
                        )
                    )

                    when (result) {
                        is Resource.Success -> {
                            Timber.d("✅ [SHEET] Room created: ${result.data}")

                            kotlinx.coroutines.delay(500)

                            parentFragmentManager.setFragmentResult(
                                "add_result",
                                Bundle().apply {
                                    putBoolean("success", true)
                                    putString("parentId", departmentId)
                                    putString("type", "room")
                                }
                            )

                            Timber.d("✅ [SHEET] Fragment result sent")

                            dismiss()
                        }


                        is Resource.Error -> {
                            Timber.e("❌ Room creation failed: ${result.message}")
                            binding.textError.text = result.message ?: "Failed to create room"
                            binding.textError.visibility = View.VISIBLE
                            binding.btnSave.isEnabled = true
                            binding.progressBar.visibility = View.GONE
                        }
                        else -> {}
                    }
                } catch (e: Exception) {
                    Timber.e(e, "❌ Room creation exception")
                    binding.textError.text = e.message ?: "Failed to create room"
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
