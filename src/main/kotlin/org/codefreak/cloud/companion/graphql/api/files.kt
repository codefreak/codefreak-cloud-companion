package org.codefreak.cloud.companion.graphql.api

import graphql.schema.idl.RuntimeWiring
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import org.codefreak.cloud.companion.FileService
import org.codefreak.cloud.companion.graphql.model.FileSystemEvent
import org.codefreak.cloud.companion.graphql.model.FileSystemEventType
import org.codefreak.cloud.companion.graphql.model.FileSystemNode
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.graphql.boot.RuntimeWiringBuilderCustomizer
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Component
class FilesDataWiring(
    private val filesQuery: FilesQuery,
    private val filesSubscription: FilesSubscription
) : RuntimeWiringBuilderCustomizer {
    override fun customize(builder: RuntimeWiring.Builder) {
        builder.type("Query") { wiring ->
            wiring.dataFetcher("listFiles") { env ->
                filesQuery.listFiles(env.getArgument("path"))
            }
        }
        builder.type("Subscription") { wiring ->
            wiring.dataFetcher("watchFiles") { env ->
                filesSubscription.watchFiles(env.getArgument("path"))
            }
        }
    }
}

@Component
class FilesQuery {
    @Autowired
    lateinit var fileService: FileService

    fun listFiles(path: String): Mono<List<FileSystemNode>> {
        return Mono.just(fileService.resolve(path))
            .flatMapMany {
                when {
                    !it.exists() -> Flux.error(IllegalArgumentException("Directory $path does not exist"))
                    !it.isDirectory() -> Flux.error(IllegalArgumentException("$path is not a directory"))
                    else -> Flux.fromStream {
                        Files.list(it)
                    }
                }
            }
            .map {
                FileSystemNode(fileService.relativePath(it), it.toFile())
            }
            .collectList()
    }
}

@Component
class FilesSubscription {
    @Autowired
    lateinit var fileService: FileService

    /**
     * Watch given directory for changes (new, deleted, modified files).
     * For new files this will trigger "new" and "modified"
     */
    fun watchFiles(path: String): Flux<FileSystemEvent> {
        val dir = fileService.resolve(path)
        return fileService.watchDirectory(path).map {
            val eventPath = it.context() as Path
            FileSystemEvent(
                fileService.relativePath(dir.resolve(eventPath)),
                FileSystemEventType(it.kind())
            )
        }
    }
}
