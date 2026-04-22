package com.etachi.smartassetmanagement.ui.organigramme

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.etachi.smartassetmanagement.databinding.BottomSheetAddDirectionBinding
import com.etachi.smartassetmanagement.domain.model.Direction
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import com.etachi.smartassetmanagement.domain.repository.LocationRepository
import com.etachi.smartassetmanagement.domain.model.Resource

@AndroidEntryPoint
class AddDirectionSheet : BottomSheetDialogFragment() {

    @Inject
    lateinit var locationRepository: LocationRepository

    private var _binding: BottomSheetAddDirectionBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetAddDirectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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

            Timber.d("🏛️ Saving direction: name=$name, code=$code")

            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val result = locationRepository.createDirection(
                        Direction(
                            name = name,
                            code = code,
                            isActive = true
                        )
                    )

                    when (result) {
                        is Resource.Success -> {
                            Timber.d("✅ Direction created successfully: ${result.data}")

                            // ✅ FIXED: Add small delay to ensure Firestore propagates
                            kotlinx.coroutines.delay(500)

                            // ✅ Notify parent fragment to refresh
                            parentFragmentManager.setFragmentResult(
                                "add_result",
                                Bundle().apply {
                                    putBoolean("success", true)
                                    putString("parentId", "")
                                    putString("type", "direction")
                                }
                            )
                            binding.progressBar.visibility = View.GONE
                            dismiss()
                        }

                        is Resource.Error -> {
                            Timber.e("❌ Direction creation failed: ${result.message}")
                            binding.textError.text = result.message ?: "Failed to create direction"
                            binding.textError.visibility = View.VISIBLE
                            binding.btnSave.isEnabled = true
                            binding.progressBar.visibility = View.GONE
                        }
                        else -> {}
                    }
                } catch (e: Exception) {
                    Timber.e(e, "❌ Direction creation exception")
                    binding.textError.text = e.message ?: "Failed to create direction"
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
