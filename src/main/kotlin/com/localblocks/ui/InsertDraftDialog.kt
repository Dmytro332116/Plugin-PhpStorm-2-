package com.localblocks.ui

import com.intellij.codeInsight.template.impl.TemplateImpl
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.SearchTextField
import com.intellij.ui.components.JBList
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.DefaultListModel
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.ListSelectionModel
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class InsertDraftDialog(
    project: Project?,
    templates: List<TemplateImpl>
) : DialogWrapper(project) {
    private val listModel = DefaultListModel<TemplateImpl>()
    private val list = JBList(listModel)
    private val searchField = SearchTextField()

    init {
        title = "Insert Draft Template"
        list.selectionMode = ListSelectionModel.SINGLE_SELECTION
        list.visibleRowCount = 12
        list.setCellRenderer { _, value, _, _, _ ->
            val name = value.key
            val desc = value.description ?: ""
            javax.swing.JLabel(
                if (desc.isBlank()) name else "$name — $desc"
            )
        }

        applyFilter(templates, "")
        searchField.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent) = applyFilter(templates, searchField.text)
            override fun removeUpdate(e: DocumentEvent) = applyFilter(templates, searchField.text)
            override fun changedUpdate(e: DocumentEvent) = applyFilter(templates, searchField.text)
        })

        init()
    }

    fun getSelectedTemplate(): TemplateImpl? = list.selectedValue

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(BorderLayout(0, 8))
        panel.minimumSize = Dimension(420, 300)
        panel.add(searchField, BorderLayout.NORTH)
        panel.add(ScrollPaneFactory.createScrollPane(list), BorderLayout.CENTER)
        return panel
    }

    override fun doOKAction() {
        if (list.selectedValue != null) {
            super.doOKAction()
        }
    }

    override fun getPreferredFocusedComponent(): JComponent = searchField.textEditor

    private fun applyFilter(templates: List<TemplateImpl>, raw: String) {
        val query = raw.trim().lowercase()
        listModel.clear()
        val filtered = if (query.isEmpty()) {
            templates
        } else {
            templates.filter { t ->
                t.key.lowercase().contains(query) ||
                    (t.description ?: "").lowercase().contains(query)
            }
        }
        filtered.forEach { listModel.addElement(it) }
        if (listModel.size() > 0) {
            list.selectedIndex = 0
        }
    }
}
