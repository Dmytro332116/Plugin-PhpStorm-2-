package com.localblocks.templates

import com.intellij.codeInsight.template.impl.TemplateImpl
import com.intellij.codeInsight.template.impl.TemplateSettings
import com.intellij.codeInsight.template.impl.TemplateContextTypes

object TemplateDraftService {
    private const val GROUP_NAME = "User Drafts"

    fun createOrUpdate(abbreviation: String, description: String, templateText: String, contexts: Set<String>) {
        val settings = TemplateSettings.getInstance()
        val existing = settings.getTemplate(abbreviation, GROUP_NAME)
        if (existing != null) {
            settings.removeTemplate(existing)
        }

        val template = TemplateImpl(abbreviation, templateText, GROUP_NAME)
        if (description.isNotBlank()) {
            template.description = description
        }

        val allContexts = TemplateContextTypes.getAllContextTypes()
        allContexts.forEach { template.templateContext.setEnabled(it, false) }
        resolveContexts(contexts, allContexts).forEach { template.templateContext.setEnabled(it, true) }

        settings.addTemplate(template)
    }

    private fun resolveContexts(
        selected: Set<String>,
        allContexts: List<com.intellij.codeInsight.template.TemplateContextType>
    ): List<com.intellij.codeInsight.template.TemplateContextType> {
        if (selected.isEmpty()) return emptyList()
        val byIdOrName: (String) -> com.intellij.codeInsight.template.TemplateContextType? = { token ->
            allContexts.firstOrNull { ctx ->
                ctx.contextId.equals(token, ignoreCase = true) ||
                    ctx.presentableName.equals(token, ignoreCase = true)
            }
        }

        val resolved = mutableListOf<com.intellij.codeInsight.template.TemplateContextType>()
        val tokens = selected.flatMap { token ->
            when (token.uppercase()) {
                "JAVASCRIPT" -> listOf("JavaScript", "JS")
                "HTML" -> listOf("HTML")
                "PHP" -> listOf("PHP")
                "TWIG" -> listOf("Twig")
                else -> listOf(token)
            }
        }

        tokens.forEach { token ->
            val match = byIdOrName(token)
            if (match != null && resolved.none { it.contextId == match.contextId }) {
                resolved.add(match)
            }
        }
        return resolved
    }
}
