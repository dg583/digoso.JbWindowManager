package com.example.layoutmanager.actions

import com.example.layoutmanager.layout.applyProfile
import com.example.layoutmanager.model.Profile
import com.example.layoutmanager.store.ProfileService
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Separator
import com.intellij.openapi.components.service

/** Untermenü `Window > Profile`: Liste der Profile (Klick wendet an), danach Speichern und Verwalten. */
class ProfilesActionGroup : ActionGroup() {
    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun getChildren(e: AnActionEvent?): Array<AnAction> {
        val store = service<ProfileService>().store
        val items = mutableListOf<AnAction>()
        store.loadError?.let { items += DisabledAction("Profile nicht lesbar: $it (nach Korrektur IDE neu starten)") }
        store.profiles().mapTo(items) { ApplyProfileAction(it) }
        items += Separator.getInstance()
        items += SaveProfileAction()
        items += ManageProfilesAction()
        return items.toTypedArray()
    }
}

private class ApplyProfileAction(private val profile: Profile) : AnAction() {
    init {
        templatePresentation.setText(profile.name, false) // false: kein Mnemonic aus '_' im Namen
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = e.project != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        applyProfile(project, profile)
    }
}

private class DisabledAction(text: String) : AnAction() {
    init {
        templatePresentation.setText(text, false)
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = false
    }

    override fun actionPerformed(e: AnActionEvent) = Unit
}
