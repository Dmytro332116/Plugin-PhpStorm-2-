package com.localblocks.templates

import com.intellij.codeInsight.template.TemplateContextType
import com.intellij.codeInsight.template.TemplateSettings
import com.intellij.codeInsight.template.impl.TemplateImpl

object TemplateDraftService {
    private const val GROUP_NAME = "User Drafts"

    fun createOrUpdate(abbreviation: String, description: String, templateText: String, contexts: Set<String>) {
        val settings = TemplateSettings.getInstance()
        val existing = settings.getTemplate(abbreviation, GROUP_NAME)
        if (existing != null) {
            settings.removeTemplate(existing, GROUP_NAME)
        }

        val template = TemplateImpl(abbreviation, templateText, GROUP_NAME)
        if (description.isNotBlank()) {
            template.description = description
        }

        val allContexts = TemplateContextType.getAllContextTypes()
        allContexts.forEach { template.templateContext.setEnabled(it, false) }
        resolveContexts(contexts, allContexts).forEach { template.templateContext.setEnabled(it, true) }

        settings.addTemplate(template, GROUP_NAME)
    }

    private fun resolveContexts(
        selected: Set<String>,
        allContexts: List<TemplateContextType>
    ): List<TemplateContextType> {
        if (selected.isEmpty()) return emptyList()
        val byIdOrName: (String) -> TemplateContextType? = { token ->
            allContexts.firstOrNull { ctx ->
                ctx.contextId.equals(token, ignoreCase = true) ||
                    ctx.presentableName.equals(token, ignoreCase = true)
            }
        }

        val resolved = mutableListOf<TemplateContextType>()
        val tokens = selected.flatMap { token ->
            when (token.uppercase()) {
                "JS" -> listOf("JavaScript", "JS")
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
