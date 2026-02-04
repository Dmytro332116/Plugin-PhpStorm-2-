package com.localblocks.model

import java.util.UUID

data class DraftNote(
    var id: String = UUID.randomUUID().toString(),
    var title: String = "",
    var twigTemplate: String = "",
    var twigCode: String = "",
    var preprocessLocation: String = "",
    var preprocessCode: String = "",
    var jsLocation: String = "",
    var jsCode: String = "",
    var generalNotes: String = "",
    var updatedAt: Long = System.currentTimeMillis()
)
