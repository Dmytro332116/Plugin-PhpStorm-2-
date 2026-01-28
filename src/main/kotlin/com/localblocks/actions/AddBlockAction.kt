package com.localblocks.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.localblocks.ui.AddBlockDialog
import com.localblocks.templates.TemplateDraftService

class AddBlockAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val dialog = AddBlockDialog(e.project)
        if (dialog.showAndGet()) {
            TemplateDraftService.createOrUpdate(
                abbreviation = dialog.getAbbreviation(),
                description = dialog.getDescription(),
                templateText = dialog.getTemplateText(),
                contexts = dialog.getSelectedContexts()
            )
        }
    }
}
