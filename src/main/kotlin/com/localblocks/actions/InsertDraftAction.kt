package com.localblocks.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.ui.Messages
import com.localblocks.templates.TemplateDraftService
import com.localblocks.ui.InsertDraftDialog

class InsertDraftAction : AnAction(), DumbAware {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = FileEditorManager.getInstance(project).selectedTextEditor
        if (editor == null) {
            Messages.showInfoMessage(project, "Open a file to insert a draft.", "Insert Draft Template")
            return
        }

        val templates = TemplateDraftService.getUserDraftTemplates()
        if (templates.isEmpty()) {
            Messages.showInfoMessage(project, "No draft templates found.", "Insert Draft Template")
            return
        }

        val dialog = InsertDraftDialog(project, templates)
        if (dialog.showAndGet()) {
            val selected = dialog.getSelectedTemplate() ?: return
            TemplateDraftService.insertTemplate(project, editor, selected)
        }
    }
}
