package org.codefreak.cloud.companion

import java.nio.file.FileSystem
import java.nio.file.FileSystemException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchEvent
import java.nio.file.WatchKey
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import org.apache.tika.Tika
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.http.codec.multipart.FilePart
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

private val DEFAULT_WATCH_EVENT_KINDS = arrayOf(
    StandardWatchEventKinds.ENTRY_CREATE,
    StandardWatchEventKinds.ENTRY_MODIFY,
    StandardWatchEventKinds.ENTRY_DELETE
)

@Service
class FileService(
    @Autowired private val tika: Tika,
    @Value("#{config.projectFilesPath}") basePathString: String
) {
    private val basePath = Paths.get(basePathString)
    val fileSystem: FileSystem = basePath.fileSystem

    fun resolve(path: String): Path {
        return basePath.resolve(sanitizePath(path))
    }

    fun relativePath(path: Path): String {
        return "/${basePath.relativize(path)}"
    }

    /**
     * Watch a directory for events using nio's WatchService.
     * This creates a Flux which emits new WatchEvents.
     * The WatchService will be closed when the subscription ends.
     */
    fun watchDirectory(path: String): Flux<WatchEvent<*>> {
        val directory = resolve(path)
        if (!directory.isDirectory()) {
            return Flux.error(IllegalArgumentException("$directory is not a directory"))
        }

        return Mono.fromCallable {
            fileSystem.newWatchService().also {
                directory.register(it, DEFAULT_WATCH_EVENT_KINDS)
            }
        }.flatMapMany { watchService ->
            Flux.generate<WatchKey> { it.next(watchService.take()) }
                .subscribeOn(Schedulers.boundedElastic())
                .doOnCancel { watchService.close() }
                .doOnDiscard(WatchKey::class.java) { it.reset() }
                .flatMap {
                    it.reset()
                    Flux.fromIterable(it.pollEvents())
                }
        }
    }

    fun saveUpload(file: FilePart): Mono<Void> {
        return Mono.just(file)
            .map {
                // make sure parent dir exists to allow uploading nested file structures
                val parentDir = resolve(file.filename()).parent
                if (parentDir != null && !parentDir.exists()) {
                    try {
                        Files.createDirectories(parentDir)
                    } catch (e: FileSystemException) {
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
