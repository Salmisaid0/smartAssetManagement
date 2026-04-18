package com.etachi.smartassetmanagement.ui.organigramme

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.etachi.smartassetmanagement.databinding.FragmentOrganigramBinding
import com.etachi.smartassetmanagement.ui.organigramme.model.NodeType
import com.etachi.smartassetmanagement.ui.organigramme.wizard.WizardBottomSheetFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class OrganigrammeFragment : Fragment() {

    companion object {
        private const val TAG = "OrganigrammeFragment"
    }

    private var _binding: FragmentOrganigramBinding? = null
    private val binding get() = _binding!!

    private val viewModel: OrganigrammeViewModel by viewModels()
    private lateinit var adapter: OrganigrammeAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOrganigramBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerOrganigramme.layoutManager = LinearLayoutManager(requireContext())

        adapter = OrganigrammeAdapter(
            onDirectionClick = { nodeId -> viewModel.toggleNode(nodeId) },
            onDepartmentClick = { nodeId -> viewModel.toggleNode(nodeId) },
            onAddChildClick = { item -> showWizard() }
        )
        binding.recyclerOrganigramme.adapter = adapter

        // ✅ FIXED: Show wizard when FAB clicked
        binding.fabAddDirection.setOnClickListener {
            showWizard()
        }

        setupObservers()
    }

    // ✅ ADDED: Helper method to show wizard
    private fun showWizard() {
        Timber.d("Showing wizard...")
        val wizard = WizardBottomSheetFragment()
        wizard.show(childFragmentManager, TAG)
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    if (state.nodes.isEmpty() && !state.isLoading) {
                        binding.recyclerOrganigramme.visibility = View.GONE
                        binding.emptyStateView.visibility = View.VISIBLE
                    } else {
                        binding.recyclerOrganigramme.visibility = View.VISIBLE
                        binding.emptyStateView.visibility = View.GONE
                    }

                    adapter.submitList(state.nodes)

                    val directions = state.nodes.count { it.type == NodeType.DIRECTION }
                    val departments = state.nodes.count { it.type == NodeType.DEPARTMENT }
                    val rooms = state.nodes.count { it.type == NodeType.ROOM }

                    binding.textDirectionCount.text = "$directions Direction${if (directions != 1) "s" else ""}"
                    binding.textDepartmentCount.text = "$departments Dept${if (departments != 1) "s" else ""}"
                    binding.textRoomCount.text = "$rooms Room${if (rooms != 1) "s" else ""}"
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
