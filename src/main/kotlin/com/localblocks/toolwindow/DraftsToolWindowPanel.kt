package com.localblocks.toolwindow

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.OnePixelSplitter
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.util.Alarm
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import com.localblocks.model.DraftNote
import com.localblocks.storage.DraftStorage
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Font
import javax.swing.DefaultListModel
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.event.DocumentEvent
import javax.swing.text.JTextComponent

class DraftsToolWindowPanel(private val project: Project) : JPanel(BorderLayout()), Disposable {
    private val notes: MutableList<DraftNote> = DraftStorage.load()
    private val listModel = DefaultListModel<DraftNote>()
    private val list = JBList(listModel)

    private val titleField = JBTextField()
    private val twigTemplateField = JBTextField()
    private val twigCodeArea = createCodeArea()
    private val preprocessLocationField = JBTextField()
    private val preprocessCodeArea = createCodeArea()
    private val jsLocationField = JBTextField()
    private val jsCodeArea = createCodeArea()
    private val generalNotesArea = createCodeArea(rows = 4)

    private val saveAlarm = Alarm(Alarm.ThreadToUse.SWING_THREAD, this)

    private var updating = false
    private var selectedNote: DraftNote? = null

    init {
        list.setCellRenderer(SimpleListCellRenderer.create("") { note ->
            note.title.ifBlank { "Untitled Draft" }
        })
        list.selectionMode = javax.swing.ListSelectionModel.SINGLE_SELECTION
        list.emptyText.text = "No drafts yet"
        list.visibleRowCount = 12
        notes.forEach { listModel.addElement(it) }
        list.addListSelectionListener { event ->
            if (!event.valueIsAdjusting) {
                selectNote(list.selectedValue)
            }
        }

        val newButton = JButton("New")
        newButton.addActionListener { createNote() }

        val leftPanel = JPanel(BorderLayout(0, JBUI.scale(8))).apply {
            border = JBUI.Borders.empty(8)
            add(newButton, BorderLayout.NORTH)
            add(ScrollPaneFactory.createScrollPane(list), BorderLayout.CENTER)
        }

        val detailsPanel = JPanel().apply {
            layout = javax.swing.BoxLayout(this, javax.swing.BoxLayout.Y_AXIS)
            border = JBUI.Borders.empty(8, 8, 8, 12)
            add(labeled("Title", titleField))
            add(section("Twig Template", twigTemplateField, twigCodeArea))
            add(section("Preprocess", preprocessLocationField, preprocessCodeArea))
            add(section("JavaScript", jsLocationField, jsCodeArea))
            add(labeled("General Notes", wrap(generalNotesArea, 120)))
        }

        val rightPanel = JBScrollPane(detailsPanel).apply {
            border = JBUI.Borders.empty()
            preferredSize = Dimension(640, 520)
        }

        val splitter = OnePixelSplitter(false, 0.3f).apply {
            firstComponent = leftPanel
            secondComponent = rightPanel
        }
        add(splitter, BorderLayout.CENTER)

        bindFields()
        setFieldsEnabled(false)

        if (listModel.size() > 0) {
            list.selectedIndex = 0
        }
    }

    override fun dispose() = Unit

    private fun createNote() {
        val note = DraftNote(title = "New Draft")
        notes.add(note)
        listModel.addElement(note)
        list.selectedIndex = listModel.size() - 1
        scheduleSave()
        IdeFocusManager.getInstance(project).requestFocus(titleField, true)
        titleField.selectAll()
    }

    private fun selectNote(note: DraftNote?) {
        selectedNote = note
        updating = true
        if (note == null) {
            clearFields()
            setFieldsEnabled(false)
        } else {
            titleField.text = note.title
            twigTemplateField.text = note.twigTemplate
            twigCodeArea.text = note.twigCode
            preprocessLocationField.text = note.preprocessLocation
            preprocessCodeArea.text = note.preprocessCode
            jsLocationField.text = note.jsLocation
            jsCodeArea.text = note.jsCode
            generalNotesArea.text = note.generalNotes
            setFieldsEnabled(true)
        }
        updating = false
    }

    private fun clearFields() {
        titleField.text = ""
        twigTemplateField.text = ""
        twigCodeArea.text = ""
        preprocessLocationField.text = ""
        preprocessCodeArea.text = ""
        jsLocationField.text = ""
        jsCodeArea.text = ""
        generalNotesArea.text = ""
    }

    private fun setFieldsEnabled(enabled: Boolean) {
        titleField.isEnabled = enabled
        twigTemplateField.isEnabled = enabled
        twigCodeArea.isEnabled = enabled
        preprocessLocationField.isEnabled = enabled
        preprocessCodeArea.isEnabled = enabled
        jsLocationField.isEnabled = enabled
        jsCodeArea.isEnabled = enabled
        generalNotesArea.isEnabled = enabled
    }

    private fun bindFields() {
        bind(titleField) { note, value ->
            note.title = value
            list.repaint()
        }
        bind(twigTemplateField) { note, value -> note.twigTemplate = value }
        bind(twigCodeArea) { note, value -> note.twigCode = value }
        bind(preprocessLocationField) { note, value -> note.preprocessLocation = value }
        bind(preprocessCodeArea) { note, value -> note.preprocessCode = value }
        bind(jsLocationField) { note, value -> note.jsLocation = value }
        bind(jsCodeArea) { note, value -> note.jsCode = value }
        bind(generalNotesArea) { note, value -> note.generalNotes = value }
    }

    private fun bind(field: JTextComponent, updater: (DraftNote, String) -> Unit) {
        field.document.addDocumentListener(object : DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) {
                if (updating) return
                val note = selectedNote ?: return
                updater(note, field.text)
                note.updatedAt = System.currentTimeMillis()
                scheduleSave()
            }
        })
    }

    private fun scheduleSave() {
        saveAlarm.cancelAllRequests()
        saveAlarm.addRequest({
            val snapshot = notes.map { it.copy() }
            ApplicationManager.getApplication().executeOnPooledThread {
                DraftStorage.save(snapshot)
            }
        }, 300)
    }

    private fun labeled(label: String, component: JComponent): JComponent {
        val panel = JPanel(BorderLayout(0, JBUI.scale(4)))
        panel.border = JBUI.Borders.empty(6, 0)
        panel.add(JLabel(label), BorderLayout.NORTH)
        panel.add(component, BorderLayout.CENTER)
        return panel
    }

    private fun section(title: String, locationField: JBTextField, codeArea: JBTextArea): JComponent {
        val panel = JPanel(BorderLayout(0, JBUI.scale(6)))
        panel.border = JBUI.Borders.empty(6, 0)
        panel.add(JLabel(title), BorderLayout.NORTH)

        val inner = JPanel(BorderLayout(0, JBUI.scale(6)))
        inner.add(labeled("Location / File", locationField), BorderLayout.NORTH)
        inner.add(wrap(codeArea, 160), BorderLayout.CENTER)
        panel.add(inner, BorderLayout.CENTER)
        return panel
    }

    private fun wrap(area: JBTextArea, height: Int): JComponent {
        return JBScrollPane(area).apply {
            preferredSize = Dimension(480, JBUI.scale(height))
        }
    }

    private fun createCodeArea(rows: Int = 6): JBTextArea {
        return JBTextArea().apply {
            lineWrap = false
            wrapStyleWord = false
            this.rows = rows
            font = JBFont.create(Font(Font.MONOSPACED, Font.PLAIN, 12))
        }
    }
}
