package org.codefreak.cloud.companion.graphql.model

import java.io.File

class File(
    override val path: String,
    private val file: File
) : FileSystemNode {
    fun size() = file.length()
}
