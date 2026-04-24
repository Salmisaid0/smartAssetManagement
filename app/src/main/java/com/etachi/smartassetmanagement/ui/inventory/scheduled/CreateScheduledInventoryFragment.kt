package com.etachi.smartassetmanagement.ui.inventory.scheduled

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.etachi.smartassetmanagement.databinding.FragmentCreateScheduledInventoryBinding
import com.etachi.smartassetmanagement.domain.model.ScheduledInventory
import com.etachi.smartassetmanagement.domain.model.ScheduledInventoryStatus
import com.etachi.smartassetmanagement.utils.UserSessionManager
import dagger.hilt.android.AndroidEntryPoint
import java.util.Calendar
import javax.inject.Inject

@AndroidEntryPoint
class CreateScheduledInventoryFragment : Fragment() {

    private var _binding: FragmentCreateScheduledInventoryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ScheduledInventoryViewModel by viewModels()

    @Inject
    lateinit var sessionManager: UserSessionManager

    private var startDateMillis = System.currentTimeMillis()
    private var endDateMillis = System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000) // 7 days later

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateScheduledInventoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupDatePickers()
        setupSaveButton()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupDatePickers() {
        binding.btnSelectDate.setOnClickListener {
            showDatePicker()
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = startDateMillis

        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val selectedCalendar = Calendar.getInstance()
                selectedCalendar.set(year, month, dayOfMonth)
                startDateMillis = selectedCalendar.timeInMillis
                endDateMillis = startDateMillis + (7 * 24 * 60 * 60 * 1000)
                binding.btnSelectDate.text = "${dayOfMonth}/${month + 1}/$year"
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun setupSaveButton() {
        binding.btnSave.setOnClickListener {
            createScheduledInventory()
        }
    }

    private fun createScheduledInventory() {
        val title = binding.inputTitle.text?.toString()?.trim() ?: ""
        val description = binding.inputDescription.text?.toString()?.trim() ?: ""

        if (title.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter a title", Toast.LENGTH_SHORT).show()
            return
        }

        val currentUser = sessionManager.getCurrentUser()

        val inventory = ScheduledInventory(
            title = title,
            description = description,
            startDateMillis = startDateMillis,
            endDateMillis = endDateMillis,
            status = ScheduledInventoryStatus.SCHEDULED,
            roomIds = emptyList(),
            auditorIds = currentUser?.uid?.let { listOf(it) } ?: emptyList(),
            createdBy = currentUser?.uid ?: ""
        )

        viewModel.createScheduledInventory(inventory)

        Toast.makeText(requireContext(), "Inventory scheduled!", Toast.LENGTH_SHORT).show()
        findNavController().navigateUp()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
