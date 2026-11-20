package com.digoso.jbwindowmanager.actions

import com.digoso.jbwindowmanager.layout.captureProfile
import com.digoso.jbwindowmanager.store.ProfileService
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.ui.Messages

class SaveProfileAction : AnAction("Save as…") {
    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = e.project != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val store = service<ProfileService>().store
        val name = Messages.showInputDialog(project, "Profile name:", "Save Profile", null)?.trim() ?: return
        if (store.profiles().any { it.name.equals(name, ignoreCase = true) }) {
            val answer = Messages.showYesNoDialog(project, "The profile \"$name\" already exists. Overwrite?", "Save Profile", null)
            if (answer != Messages.YES) return
        }
        try {
            store.save(captureProfile(project, name))
        } catch (ex: IllegalArgumentException) {
            Messages.showErrorDialog(project, ex.message ?: "Invalid name", "Save Profile")
        } catch (ex: IllegalStateException) {
            Messages.showErrorDialog(project, ex.message ?: "Profiles unreadable", "Save Profile")
        }
    }
}
