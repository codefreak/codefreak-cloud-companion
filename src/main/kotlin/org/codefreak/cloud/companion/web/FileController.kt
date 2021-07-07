package org.codefreak.cloud.companion.web

import kotlin.io.path.absolutePathString
import kotlin.io.path.exists
import kotlin.io.path.inputStream
import kotlin.io.path.isReadable
import kotlin.io.path.isRegularFile
import org.codefreak.cloud.companion.FileService
import org.codefreak.cloud.companion.FileServiceException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.InputStreamResource
import org.springframework.core.io.Resource
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.multipart.FilePart
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.HandlerMapping
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * URL route prefix for files
 */
const val FILES_PATH_PREFIX = "/files"

@RestController
@RequestMapping(FILES_PATH_PREFIX)
class FileController {
    companion object {
        private val log = LoggerFactory.getLogger(FileController::class.java)
    }

    @Autowired
    private lateinit var fileService: FileService

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun uploadFiles(@RequestPart files: Flux<FilePart>): Mono<Void> {
        return files
            .flatMap {
                log.debug("Saving uploaded file ${it.filename()}")
                fileService.saveUpload(it)
            }
            .doOnError(FileServiceException::class.java) { e ->
                throw ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    e.message
                )
            }
            .then()
    }

    @GetMapping("/{path}/**")
    fun downloadFile(@PathVariable path: String, exchange: ServerWebExchange): ResponseEntity<Resource> {
        val filePath = extractFilePath(exchange)
        log.debug("Trying to serve file $filePath")
        val file = fileService.resolve(filePath)
        when {
            !file.exists() -> throw ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "File ${file.absolutePathString()} does not exist"
            )
            !file.isRegularFile() -> throw ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "File ${file.absolutePathString()} is no file"
            )
            !file.isReadable() -> throw ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "File ${file.absolutePathString()} is not readable"
            )
        }
        return ResponseEntity
            .ok()
            .contentType(fileService.getDownloadMimeType(file))
            // do not use filesystem based resources here as spring will be "smart"
            // and overrides our explicit content-type from above
            .body(InputStreamResource(file.inputStream()))
    }

    /**
     * Get the path of a file from the request
     * This expects the url to start with FILES_PREFIX and everything after the path is the name of the file.
     * Example:
     *   http://localhost:8080/files/my/file/path
     *   will return
     *   /my/file/path
     */
    private fun extractFilePath(exchange: ServerWebExchange): String {
        val fullPath = exchange.getAttribute<String>(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE)
        if (fullPath == null || !fullPath.startsWith(FILES_PATH_PREFIX)) {
            throw IllegalArgumentException("Current path does not start with $FILES_PATH_PREFIX")
        }
        return fullPath.substring(FILES_PATH_PREFIX.length)
    }
}
