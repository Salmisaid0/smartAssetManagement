package com.etachi.smartassetmanagement.ui.organigramme.wizard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.etachi.smartassetmanagement.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.textview.MaterialTextView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class WizardBottomSheetFragment : BottomSheetDialogFragment() {

    private val viewModel: WizardViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewModel.resetState()
        return inflater.inflate(R.layout.bottom_sheet_wizard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val container = view.findViewById<FrameLayout>(R.id.wizardContentContainer)
        val btnNext = view.findViewById<MaterialButton>(R.id.btnNext)
        val btnSkip = view.findViewById<MaterialButton>(R.id.btnSkip)
        val textError = view.findViewById<TextView>(R.id.textError)

        Timber.d("WizardBottomSheetFragment: onViewCreated called")

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                Timber.d("Wizard state collected: ${state.currentStep}, isComplete: ${state.isComplete}")

                if (state.isComplete) {
                    Timber.d("Wizard complete, dismissing...")
                    dismiss()
                    return@collect
                }

                updateProgressBar(state.currentStep)
                swapContent(container, state.currentStep)

                textError.text = state.errorMessage ?: ""
                textError.visibility = if (state.errorMessage != null) View.VISIBLE else View.GONE

                when (state.currentStep) {
                    is OrganigrammeWizardStep.PromptAddDepartment -> {
                        btnNext.visibility = View.GONE
                        btnSkip.text = "No, Finish Here"
                    }
                    is OrganigrammeWizardStep.PromptAddRoom -> {
                        btnNext.visibility = View.GONE
                        btnSkip.text = "No, Finish Here"
                    }
                    is OrganigrammeWizardStep.AddRoom -> {
                        btnNext.text = "Save & Finish"
                        btnNext.visibility = View.VISIBLE
                        btnSkip.visibility = View.GONE
                    }
                    else -> {
                        btnNext.text = "Next"
                        btnNext.visibility = View.VISIBLE
                        btnSkip.visibility = View.VISIBLE
                    }
                }
            }
        }

        btnNext.setOnClickListener {
            Timber.d("Next button clicked")
            extractAndSubmit()
        }
        btnSkip.setOnClickListener {
            Timber.d("Skip button clicked")
            viewModel.onActionSkip()
        }
    }

    private fun extractAndSubmit() {
        val container = view?.findViewById<FrameLayout>(R.id.wizardContentContainer) ?: return

        val inputName = container.findViewWithTag("input_name") as? TextInputEditText
        val inputCode = container.findViewWithTag("input_code") as? TextInputEditText

        val name: String
        val code: String

        if (inputName == null || inputCode == null) {
            val currentStep = viewModel.uiState.value.currentStep
            when (currentStep) {
                is OrganigrammeWizardStep.PromptAddDepartment -> {
                    viewModel.onActionNext(currentStep.directionName, "DEPT")
                    return
                }
                is OrganigrammeWizardStep.PromptAddRoom -> {
                    viewModel.onActionNext(currentStep.departmentName, "ROOM")
                    return
                }
                // ✅ FIXED: Added else branch for exhaustive when
                else -> {
                    name = ""
                    code = ""
                }
            }
        } else {
            name = inputName.text?.toString() ?: ""
            code = inputCode.text?.toString() ?: ""
        }

        Timber.d("Extracted - Name: $name, Code: $code")

        viewModel.clearError()
        viewModel.onActionNext(name, code)
    }

    private fun swapContent(container: FrameLayout, step: OrganigrammeWizardStep) {
        Timber.d("🔧 swapContent called: ${step::class.simpleName}")
        container.removeAllViews()

        val view = when (step) {
            is OrganigrammeWizardStep.AddDirection -> {
                Timber.d("✅ Creating AddDirection card")
                createInputCard(requireContext(), "Create Direction")
            }
            is OrganigrammeWizardStep.AddDepartment -> {
                Timber.d("✅ Creating AddDepartment card for: ${step.directionName}")
                createInputCard(requireContext(), "Create Department for '${step.directionName}'")
            }
            is OrganigrammeWizardStep.PromptAddDepartment -> {
                Timber.d("✅ Creating PromptAddDepartment card for: ${step.directionName}")
                createPromptCard(requireContext(), "Direction Created Successfully",
                    "'${step.directionName}' is ready. Would you like to add a Department?")
            }
            is OrganigrammeWizardStep.AddRoom -> {
                Timber.d("✅ Creating AddRoom card for: ${step.departmentName}")
                createInputCard(requireContext(), "Create Room for '${step.departmentName}'")
            }
            is OrganigrammeWizardStep.PromptAddRoom -> {
                Timber.d("✅ Creating PromptAddRoom card for: ${step.departmentName}")
                createPromptCard(requireContext(), "Department Created Successfully",
                    "'${step.departmentName}' is ready. Add a Room to it?")
            }

            else -> {}
        }
        container.addView(view as View?)
        Timber.d("✅ View added, Child Count: ${container.childCount}")
    }

    private fun createInputCard(context: android.content.Context, title: String): View {
        val card = MaterialCardView(context)
        card.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        card.radius = 16f * context.resources.displayMetrics.density
        card.cardElevation = 4f
        card.strokeWidth = (1f * context.resources.displayMetrics.density).toInt()
        card.strokeColor = context.getColor(R.color.dash_border)
        card.setCardBackgroundColor(context.getColor(R.color.dash_surface))
        card.setPadding(
            (24f * context.resources.displayMetrics.density).toInt(),
            (24f * context.resources.displayMetrics.density).toInt(),
            (24f * context.resources.displayMetrics.density).toInt(),
            (24f * context.resources.displayMetrics.density).toInt()
        )

        val linearLayout = LinearLayout(context)
        linearLayout.orientation = LinearLayout.VERTICAL
        linearLayout.setBackgroundColor(context.getColor(R.color.dash_surface))

        val tvTitle = MaterialTextView(context)
        tvTitle.text = title
        tvTitle.textSize = 20f
        // ✅ FIXED: Use setTypeface instead of textStyle
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD)
        tvTitle.setTextColor(context.getColor(R.color.dash_text_primary))
        tvTitle.setPadding(0, 0, 0, (16f * context.resources.displayMetrics.density).toInt())
        linearLayout.addView(tvTitle)

        val tilName = TextInputLayout(context)
        tilName.boxStrokeColor = context.getColor(R.color.dash_border)
        tilName.hint = "Name *"
        // ✅ FIXED: Use ColorStateList.valueOf() for setHintTextColor
        tilName.setHintTextColor(android.content.res.ColorStateList.valueOf(context.getColor(R.color.dash_text_muted)))
        tilName.boxBackgroundColor = context.getColor(R.color.dash_surface)
        val etName = TextInputEditText(context)
        etName.inputType = android.text.InputType.TYPE_TEXT_FLAG_CAP_WORDS
        etName.tag = "input_name"
        etName.setTextColor(context.getColor(R.color.dash_text_primary))
        tilName.addView(etName)
        linearLayout.addView(tilName)

        val tilCode = TextInputLayout(context)
        tilCode.boxStrokeColor = context.getColor(R.color.dash_border)
        tilCode.hint = "Code *"
        // ✅ FIXED: Use ColorStateList.valueOf() for setHintTextColor
        tilCode.setHintTextColor(android.content.res.ColorStateList.valueOf(context.getColor(R.color.dash_text_muted)))
        tilCode.boxBackgroundColor = context.getColor(R.color.dash_surface)
        val etCode = TextInputEditText(context)
        etCode.inputType = android.text.InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
        etCode.isAllCaps = true
        etCode.tag = "input_code"
        etCode.setTextColor(context.getColor(R.color.dash_text_primary))
        tilCode.addView(etCode)
        linearLayout.addView(tilCode)

        card.addView(linearLayout)
        return card
    }

    private fun createPromptCard(context: android.content.Context, title: String, body: String): View {
        val card = MaterialCardView(context)
        card.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        card.radius = 16f * context.resources.displayMetrics.density
        card.cardElevation = 4f
        card.strokeWidth = (1f * context.resources.displayMetrics.density).toInt()
        card.strokeColor = context.getColor(R.color.dash_border)
        card.setCardBackgroundColor(context.getColor(R.color.dash_surface))
        card.setPadding(
            (32f * context.resources.displayMetrics.density).toInt(),
            (32f * context.resources.displayMetrics.density).toInt(),
            (32f * context.resources.displayMetrics.density).toInt(),
            (32f * context.resources.displayMetrics.density).toInt()
        )

        val linearLayout = LinearLayout(context)
        linearLayout.orientation = LinearLayout.VERTICAL
        linearLayout.gravity = android.view.Gravity.CENTER
        linearLayout.setBackgroundColor(context.getColor(R.color.dash_surface))

        val icon = ImageView(context)
        icon.setImageResource(R.drawable.check_mark_circle_2_svgrepo_com)
        icon.imageTintList = android.content.res.ColorStateList.valueOf(context.getColor(R.color.dash_teal))
        val px = (48f * context.resources.displayMetrics.density).toInt()
        icon.layoutParams = LinearLayout.LayoutParams(px, px)
        icon.setPadding(0, 0, 0, (16f * context.resources.displayMetrics.density).toInt())
        linearLayout.addView(icon)

        val tvTitle = MaterialTextView(context)
        tvTitle.text = title
        tvTitle.textSize = 20f
        // ✅ FIXED: Use setTypeface instead of textStyle
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD)
        tvTitle.setTextColor(context.getColor(R.color.dash_text_primary))
        tvTitle.setPadding(0, 0, 0, (8f * context.resources.displayMetrics.density).toInt())
        linearLayout.addView(tvTitle)

        val tvBody = MaterialTextView(context)
        tvBody.text = body
        tvBody.textSize = 16f
        tvBody.setTextColor(context.getColor(R.color.dash_text_muted))
        tvBody.gravity = android.view.Gravity.CENTER
        tvBody.setPadding(0, 0, 0, (24f * context.resources.displayMetrics.density).toInt())
        linearLayout.addView(tvBody)

        card.addView(linearLayout)
        return card
    }

    private fun updateProgressBar(step: OrganigrammeWizardStep) {
        val active = requireContext().getColor(R.color.dash_teal)
        val inactive = requireContext().getColor(R.color.dash_border)

        view?.findViewById<View>(R.id.ind1)?.setBackgroundColor(
            if (step is OrganigrammeWizardStep.PromptAddDepartment ||
                step is OrganigrammeWizardStep.AddDepartment ||
                step is OrganigrammeWizardStep.PromptAddRoom ||
                step is OrganigrammeWizardStep.AddRoom) active else inactive
        )
        view?.findViewById<View>(R.id.ind2)?.setBackgroundColor(
            if (step is OrganigrammeWizardStep.PromptAddRoom ||
                step is OrganigrammeWizardStep.AddRoom) active else inactive
        )
        view?.findViewById<View>(R.id.ind3)?.setBackgroundColor(
            if (step is OrganigrammeWizardStep.AddRoom) active else inactive
        )
    }
}
