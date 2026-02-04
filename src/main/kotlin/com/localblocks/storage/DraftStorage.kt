package com.localblocks.storage

import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.intellij.openapi.application.PathManager
import com.localblocks.model.DraftNote
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

object DraftStorage {
    private val gson = GsonBuilder().setPrettyPrinting().create()

    private val storagePath: Path by lazy {
        val dir = Path.of(PathManager.getConfigPath(), "code-drafts")
        if (!Files.exists(dir)) {
            Files.createDirectories(dir)
        }
        dir.resolve("notes.json")
    }

    fun load(): MutableList<DraftNote> {
        val path = storagePath
        if (!Files.exists(path)) return mutableListOf()
        return try {
            val raw = Files.readString(path, StandardCharsets.UTF_8).trim()
            if (raw.isBlank()) {
                mutableListOf()
            } else {
                val listType = object : TypeToken<List<DraftNote>>() {}.type
                val parsed: List<DraftNote>? = gson.fromJson(raw, listType)
                parsed?.toMutableList() ?: mutableListOf()
            }
        } catch (_: Exception) {
            mutableListOf()
        }
    }

    fun save(notes: List<DraftNote>) {
        val path = storagePath
        val json = gson.toJson(notes)
        val tmp = path.resolveSibling("notes.json.tmp")
        Files.writeString(tmp, json, StandardCharsets.UTF_8)
        try {
            Files.move(
                tmp,
                path,
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE
            )
        } catch (_: Exception) {
            Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING)
        }
    }
}
