package com.example.layoutmanager.actions

import com.example.layoutmanager.store.ProfileService
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.ui.CollectionListModel
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBList
import javax.swing.Action
import javax.swing.JComponent

/** Liste der Profile mit Umbenennen und Löschen. Änderungen gehen sofort in den [com.example.layoutmanager.store.ProfileStore]. */
class ManageProfilesDialog(private val project: Project) : DialogWrapper(project) {
    private val store = service<ProfileService>().store
    private val model = CollectionListModel(store.profiles().map { it.name })
    private val list = JBList(model)

    init {
        title = "Profile verwalten"
        setOKButtonText("Schließen")
        init()
    }

    override fun createActions(): Array<Action> = arrayOf(okAction)

    override fun createCenterPanel(): JComponent =
        ToolbarDecorator.createDecorator(list)
            .disableAddAction()
            .disableUpDownActions()
            .setEditAction { rename() }
            .setEditActionName("Umbenennen")
            .setRemoveAction { delete() }
            .setRemoveActionName("Löschen")
            .createPanel()

    private fun rename() {
        val old = list.selectedValue ?: return
        val new = Messages.showInputDialog(project, "Neuer Name:", "Profil umbenennen", null, old, null)?.trim() ?: return
        run { store.rename(old, new) }
    }

    private fun delete() {
        val name = list.selectedValue ?: return
        if (Messages.showYesNoDialog(project, "Profil „$name“ löschen?", "Profil löschen", null) != Messages.YES) return
        run { store.delete(name) }
    }

    /** Führt eine Store-Änderung aus, zeigt Fehler als Dialog und aktualisiert die Liste. */
    private fun run(change: () -> Unit) {
        try {
            change()
        } catch (ex: IllegalArgumentException) {
            Messages.showErrorDialog(project, ex.message ?: "Ungültige Eingabe", "Profile verwalten")
        } catch (ex: IllegalStateException) {
            Messages.showErrorDialog(project, ex.message ?: "Profile nicht lesbar", "Profile verwalten")
        }
        model.replaceAll(store.profiles().map { it.name })
    }
}
