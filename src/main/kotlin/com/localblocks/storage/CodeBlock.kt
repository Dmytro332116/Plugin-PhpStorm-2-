package com.localblocks.storage

data class CodeBlock(
    var name: String = "",
    var content: String = "",
    var tags: List<String> = emptyList(),
    var pinned: Boolean = false
)
