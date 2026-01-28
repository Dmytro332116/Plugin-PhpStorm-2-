package com.localblocks.storage

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.intellij.openapi.application.PathManager
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

object BlockStorage {
    private val gson = Gson()
    private val storagePath: Path by lazy {
        val dir = Path.of(PathManager.getConfigPath(), "local-blocks")
        if (!Files.exists(dir)) {
            Files.createDirectories(dir)
        }
        dir.resolve("blocks.json")
    }

    fun loadBlocks(): List<CodeBlock> {
        if (!Files.exists(storagePath)) {
            return emptyList()
        }
        val content = Files.readString(storagePath, StandardCharsets.UTF_8).trim()
        if (content.isEmpty()) {
            return emptyList()
        }
        val listType = object : TypeToken<List<CodeBlock>>() {}.type
        return gson.fromJson(content, listType) ?: emptyList()
    }

    fun saveBlock(block: CodeBlock) {
        val current = loadBlocks().toMutableList()
        current.add(block)
        saveBlocks(current)
    }

    fun saveBlocks(blocks: List<CodeBlock>) {
        val json = gson.toJson(blocks)
        Files.writeString(storagePath, json, StandardCharsets.UTF_8)
    }
}
