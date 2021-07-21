package org.codefreak.cloud.companion.graphql.api

import com.expediagroup.graphql.server.operations.Mutation
import com.expediagroup.graphql.server.operations.Subscription
import com.pty4j.PtyProcess
import com.pty4j.WinSize
import java.util.UUID
import org.codefreak.cloud.companion.ProcessManager
import org.codefreak.cloud.companion.waitForMono
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import org.codefreak.cloud.companion.graphql.model.Process as ProcessModel
import reactor.core.publisher.Flux

@Component
class ProcessMutation : Mutation {
    @Autowired
    private lateinit var processManager: ProcessManager

    fun startProcess(cmd: List<String>): Mono<ProcessModel> {
        return Mono.fromCallable {
            ProcessModel(processManager.createProcess(cmd))
        }.subscribeOn(Schedulers.boundedElastic())
    }

    fun killProcess(id: UUID): Mono<Int> {
        return processManager.getProcess(id)
            .destroyForcibly()
            .waitForMono()
            .subscribeOn(Schedulers.boundedElastic())
    }

    fun resizeProcess(id: UUID, cols: Int, rows: Int): Mono<Boolean> {
        return Mono.just(processManager.getProcess(id)).flatMap {
            if (it is PtyProcess) {
                it.winSize = WinSize(cols, rows)
                Mono.just(true)
            } else {
                Mono.error(IllegalArgumentException("Process $it is not resizable"))
            }
        }
    }
}

@Component
class ProcessSubscription : Subscription {
    @Autowired
    private lateinit var processManager: ProcessManager

    fun waitForProcess(id: UUID): Flux<Int> {
        return processManager.getProcess(id)
            .waitForMono()
            .flatMapMany { Flux.just(it) }
            .subscribeOn(Schedulers.boundedElastic())
    }
}