package com.localblocks.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.SearchTextField
import com.localblocks.storage.BlockStorage
import com.localblocks.storage.CodeBlock
import java.awt.BorderLayout
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.DefaultListModel
import javax.swing.JMenuItem
import javax.swing.JPanel
import javax.swing.JPopupMenu

class InsertBlockAction : AnAction(), DumbAware {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = true
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = FileEditorManager.getInstance(project).selectedTextEditor
        if (editor == null) {
            Messages.showInfoMessage(
                project,
                "Open a file in the editor to insert a block.",
                "Insert Block"
            )
            return
        }
        val document = editor.document
        val blocks = BlockStorage.loadBlocks().toMutableList()
        if (blocks.isEmpty()) {
            Messages.showErrorDialog(e.project, "No saved blocks found.", "Insert Block")
            return
        }

        data class BlockItem(val index: Int, val block: CodeBlock)

        val listModel = DefaultListModel<BlockItem>()
        val list = com.intellij.ui.components.JBList(listModel)
        list.cellRenderer = SimpleListCellRenderer.create("") { item ->
            val tags = if (item.block.tags.isEmpty()) "" else " [${item.block.tags.joinToString(", ")}]"
            val pin = if (item.block.pinned) "[PIN] " else ""
            "$pin${item.block.name}$tags"
        }

        val searchField = SearchTextField()
        fun applyFilter(pattern: String) {
            val normalized = pattern.trim().lowercase()
            listModel.clear()
            val filtered = blocks.withIndex().filter { entry ->
                if (normalized.isEmpty()) {
                    true
                } else {
                    val nameMatch = entry.value.name.lowercase().contains(normalized)
                    val tagMatch = entry.value.tags.any { it.lowercase().contains(normalized) }
                    nameMatch || tagMatch
                }
            }
            val pinned = filtered.filter { it.value.pinned }
            val others = filtered.filterNot { it.value.pinned }
            (pinned + others).forEach { entry ->
                listModel.addElement(BlockItem(entry.index, entry.value))
            }
            if (listModel.size() > 0) {
                list.selectedIndex = 0
            }
        }

        searchField.addDocumentListener(object : com.intellij.ui.DocumentAdapter() {
            override fun textChanged(e: javax.swing.event.DocumentEvent) {
                applyFilter(searchField.text)
            }
        })

        applyFilter("")

        val panel = JPanel(BorderLayout(0, 8))
        panel.add(searchField, BorderLayout.NORTH)
        panel.add(ScrollPaneFactory.createScrollPane(list), BorderLayout.CENTER)

        val popup = JBPopupFactory.getInstance()
            .createComponentPopupBuilder(panel, searchField)
            .setTitle("Insert Block")
            .setResizable(true)
            .setRequestFocus(true)
            .createPopup()

        fun insertSelected() {
            val selected = list.selectedValue ?: return
            val caretOffset = editor.caretModel.offset
            WriteCommandAction.runWriteCommandAction(project) {
                document.insertString(caretOffset, selected.block.content)
            }
            popup.closeOk(null)
        }

        fun refresh() {
            applyFilter(searchField.text)
        }

        val contextMenu = JPopupMenu()
        val editItem = JMenuItem("Edit")
        val deleteItem = JMenuItem("Delete")
        val pinItem = JMenuItem("Pin/Unpin")
        contextMenu.add(editItem)
        contextMenu.add(pinItem)
        contextMenu.add(deleteItem)

        editItem.addActionListener {
            val selected = list.selectedValue ?: return@addActionListener
            val dialog = com.localblocks.ui.AddBlockDialog(project, selected.block)
            if (dialog.showAndGet()) {
                val updated = selected.block.copy(
                    name = dialog.getBlockName(),
                    content = dialog.getBlockContent(),
                    tags = dialog.getTags(),
                    pinned = selected.block.pinned
                )
                blocks[selected.index] = updated
                BlockStorage.saveBlocks(blocks)
                refresh()
            }
        }

        deleteItem.addActionListener {
            val selected = list.selectedValue ?: return@addActionListener
            val confirm = Messages.showYesNoDialog(
                project,
                "Delete block \"${selected.block.name}\"?",
                "Delete Block",
                null
            )
            if (confirm == Messages.YES) {
                blocks.removeAt(selected.index)
                BlockStorage.saveBlocks(blocks)
                refresh()
            }
        }

        pinItem.addActionListener {
            val selected = list.selectedValue ?: return@addActionListener
            val updated = selected.block.copy(pinned = !selected.block.pinned)
            blocks[selected.index] = updated
            BlockStorage.saveBlocks(blocks)
            refresh()
        }

        list.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount == 2) {
                    insertSelected()
                }
            }

            override fun mousePressed(e: MouseEvent) {
                if (e.isPopupTrigger) {
                    showContextMenu(e)
                }
            }

            override fun mouseReleased(e: MouseEvent) {
                if (e.isPopupTrigger) {
                    showContextMenu(e)
                }
            }

            private fun showContextMenu(e: MouseEvent) {
                val index = list.locationToIndex(e.point)
                if (index >= 0) {
                    list.selectedIndex = index
                    val selected = list.selectedValue
                    if (selected != null) {
                        pinItem.text = if (selected.block.pinned) "Unpin" else "Pin"
                    }
                }
                contextMenu.show(e.component, e.x, e.y)
            }
        })

        list.addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                if (e.keyCode == KeyEvent.VK_ENTER) {
                    insertSelected()
                }
            }
        })

        popup.showInBestPositionFor(editor)
    }
}
