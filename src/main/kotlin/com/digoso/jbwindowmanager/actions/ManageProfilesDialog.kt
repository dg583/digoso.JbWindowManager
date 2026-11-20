package com.digoso.jbwindowmanager.actions

import com.digoso.jbwindowmanager.store.ProfileService
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.ui.CollectionListModel
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBList
import javax.swing.Action
import javax.swing.JComponent

/** List of profiles with rename and delete. Changes go straight to the [com.digoso.jbwindowmanager.store.ProfileStore]. */
class ManageProfilesDialog(private val project: Project) : DialogWrapper(project) {
    private val store = service<ProfileService>().store
    private val model = CollectionListModel(store.profiles().map { it.name })
    private val list = JBList(model)

    init {
        title = "Manage Profiles"
        setOKButtonText("Close")
        init()
    }

    override fun createActions(): Array<Action> = arrayOf(okAction)

    override fun createCenterPanel(): JComponent =
        ToolbarDecorator.createDecorator(list)
            .disableAddAction()
            .disableUpDownActions()
            .setEditAction { rename() }
            .setEditActionName("Rename")
            .setRemoveAction { delete() }
            .setRemoveActionName("Delete")
            .createPanel()

    private fun rename() {
        val old = list.selectedValue ?: return
        val new = Messages.showInputDialog(project, "New name:", "Rename Profile", null, old, null)?.trim() ?: return
        run { store.rename(old, new) }
    }

    private fun delete() {
        val name = list.selectedValue ?: return
        if (Messages.showYesNoDialog(project, "Delete profile \"$name\"?", "Delete Profile", null) != Messages.YES) return
        run { store.delete(name) }
    }

    /** Runs a store change, shows errors as a dialog and refreshes the list. */
    private fun run(change: () -> Unit) {
        try {
            change()
        } catch (ex: IllegalArgumentException) {
            Messages.showErrorDialog(project, ex.message ?: "Invalid input", "Manage Profiles")
        } catch (ex: IllegalStateException) {
            Messages.showErrorDialog(project, ex.message ?: "Profiles unreadable", "Manage Profiles")
        }
        model.replaceAll(store.profiles().map { it.name })
    }
}
