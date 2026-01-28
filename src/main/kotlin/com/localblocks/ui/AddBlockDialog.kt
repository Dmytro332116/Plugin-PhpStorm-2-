package com.localblocks.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.editor.event.DocumentEvent as EditorDocumentEvent
import com.intellij.openapi.editor.event.DocumentListener as EditorDocumentListener
import com.intellij.ui.EditorTextField
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.GridLayout
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class AddBlockDialog(project: Project?) : DialogWrapper(project) {
    private val abbreviationField = JBTextField()
    private val descriptionField = JBTextField()
    private val templateField = EditorTextField()
    private val contextChecks = linkedMapOf(
        "HTML" to JBCheckBox("HTML", true),
        "JavaScript" to JBCheckBox("JavaScript", true),
        "PHP" to JBCheckBox("PHP", true),
        "Twig" to JBCheckBox("Twig", true)
    )

    init {
        title = "Create Draft Template"
        init()
        updateOkState()
        val textListener = object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent) {
                updateOkState()
            }

            override fun removeUpdate(e: DocumentEvent) {
                updateOkState()
            }

            override fun changedUpdate(e: DocumentEvent) {
                updateOkState()
            }
        }
        val editorListener = object : EditorDocumentListener {
            override fun documentChanged(event: EditorDocumentEvent) {
                updateOkState()
            }
        }
        abbreviationField.document.addDocumentListener(textListener)
        templateField.document.addDocumentListener(editorListener)
    }

    fun getAbbreviation(): String = abbreviationField.text.trim()
    fun getDescription(): String = descriptionField.text.trim()
    fun getTemplateText(): String = templateField.text.trim()
    fun getSelectedContexts(): Set<String> =
        contextChecks.filter { it.value.isSelected }.keys

    override fun createCenterPanel(): JComponent {
        templateField.minimumSize = Dimension(400, 200)
        templateField.preferredSize = Dimension(400, 200)

        val contextPanel = JPanel(BorderLayout())
        val contextList = JPanel()
        contextList.layout = GridLayout(0, 2, 8, 4)
        contextChecks.values.forEach { contextList.add(it) }
        contextPanel.add(contextList, BorderLayout.CENTER)

        return FormBuilder.createFormBuilder()
            .addLabeledComponent("Abbreviation", abbreviationField)
            .addLabeledComponent("Description", descriptionField)
            .addLabeledComponentFillVertically("Template Text", templateField)
            .addLabeledComponent("Contexts", contextPanel)
            .panel
    }

    private fun updateOkState() {
        val valid = abbreviationField.text.trim().isNotEmpty() && templateField.text.trim().isNotEmpty()
        isOKActionEnabled = valid
    }
}
