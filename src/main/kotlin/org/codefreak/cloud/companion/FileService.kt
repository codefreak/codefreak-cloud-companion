package org.codefreak.cloud.companion

import org.apache.tika.Tika
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.http.codec.multipart.FilePart
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.nio.file.FileSystemException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists

@Service
class FileService(
    @Autowired private val tika: Tika,
    @Value("#{config.projectFilesPath}") basePathString: String
) {
    private val basePath = Paths.get(basePathString)

    fun resolve(path: String): Path {
        return basePath.resolve(sanitizePath(path))
    }

    fun saveUpload(file: FilePart): Mono<Void> {
        return Mono.just(file)
            .map {
                // make sure parent dir exists to allow uploading nested file structures
                val parentDir = resolve(file.filename()).parent
                if (parentDir != null && !parentDir.exists()) {
                    try {
                        Files.createDirectories(parentDir)
                    } catch(e: FileSystemException) {
                        throw FileServiceException("Could not create parent dirs of ${file.filename()}: ${e.message}")
                    }
                }
                it
            }.flatMap {
                it.transferTo(resolve(it.filename()))
            }
    }

    /**
     * Get the mime type for a file that should be used when delivering
     * a file to the browser
     */
    fun getDownloadMimeType(path: Path): MediaType {
        val mime = tika.detect(path)
        return when {
            // allow to display images natively in browsers
            mime.startsWith("image/") -> MediaType.parseMediaType(mime)
            // Do never expose real mime values for text or this will allow to deliver executable JS for example.
            // The utf-8 encoding might not be correct but most browsers will complain (in the console) if
            // they do not receive any text encoding.
            mime.startsWith("text/") -> MediaType.parseMediaType("text/plain;charset=utf-8")
            // force download for everything else
            else -> MediaType.APPLICATION_OCTET_STREAM
        }
    }

    /**
     * Remove leading dots and slashes from given path and normalizes patterns like `foo/../bar`.
     */
    private fun sanitizePath(vararg name: String) = Paths.get("/", *name).normalize().toString().trim().trim('/')
}