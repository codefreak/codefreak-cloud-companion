package org.codefreak.cloud.companion.graphql.model

/**
 * Factory for new instances of Directory/File depending on the type of file
 */
fun FileSystemNode(relativePath: String, file: java.io.File): FileSystemNode {
    return if (file.isDirectory) {
        Directory(relativePath)
    } else {
        File(relativePath, file)
    }
}

interface FileSystemNode {
    val path: String
}
