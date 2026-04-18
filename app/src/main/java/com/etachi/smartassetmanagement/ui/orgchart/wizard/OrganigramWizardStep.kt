package com.etachi.smartassetmanagement.ui.organigramme.wizard

sealed class OrganigrammeWizardStep {
    data class Idle(
        val directionId: String = "",
        val directionName: String = "",
        val departmentId: String = "",
        val departmentName: String = ""
    ) : OrganigrammeWizardStep()

    data class AddDirection(
        val directionId: String = "",
        val directionName: String = "",
        val departmentId: String = "",
        val departmentName: String = ""
    ) : OrganigrammeWizardStep()

    data class PromptAddDepartment(
        val directionId: String,
        val directionName: String,
        val departmentId: String = "",
        val departmentName: String = ""
    ) : OrganigrammeWizardStep()

    data class AddDepartment(
        val directionId: String,
        val directionName: String,
        val departmentId: String = "",
        val departmentName: String = ""
    ) : OrganigrammeWizardStep()

    data class PromptAddRoom(
        val directionId: String,
        val directionName: String,
        val departmentId: String,
        val departmentName: String
    ) : OrganigrammeWizardStep()

    data class AddRoom(
        val directionId: String,
        val directionName: String,
        val departmentId: String,
        val departmentName: String
    ) : OrganigrammeWizardStep()
}
