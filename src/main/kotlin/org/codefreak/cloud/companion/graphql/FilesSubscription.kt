package org.codefreak.cloud.companion.graphql

import com.expediagroup.graphql.server.operations.Subscription
import java.nio.file.Path
import org.codefreak.cloud.companion.FileService
import org.codefreak.cloud.companion.graphql.model.FileSystemEvent
import org.codefreak.cloud.companion.graphql.model.FileSystemEventType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux

@Component
class FilesSubscription : Subscription {
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
