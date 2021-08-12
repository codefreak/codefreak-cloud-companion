package org.codefreak.cloud.companion.graphql.api

import com.pty4j.PtyProcess
import com.pty4j.WinSize
import java.util.UUID
import org.codefreak.cloud.companion.ProcessManager
import org.codefreak.cloud.companion.graphql.model.Process as ProcessModel
import org.codefreak.cloud.companion.waitForMono
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.GraphQlController
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.SubscriptionMapping
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

@GraphQlController
class ProcessController {
    @Autowired
    private lateinit var processManager: ProcessManager

    @MutationMapping
    fun startProcess(@Argument cmd: List<String>): Mono<ProcessModel> {
        return Mono.fromCallable {
            ProcessModel(processManager.createProcess(cmd))
        }.subscribeOn(Schedulers.boundedElastic())
    }

    @MutationMapping
    fun killProcess(@Argument id: UUID): Mono<Int> {
        return processManager.getProcess(id)
            .destroyForcibly()
            .waitForMono()
            .subscribeOn(Schedulers.boundedElastic())
    }

    @MutationMapping
    fun resizeProcess(@Argument id: UUID, @Argument cols: Int, @Argument rows: Int): Mono<Boolean> {
        return Mono.just(processManager.getProcess(id)).flatMap {
            if (it is PtyProcess) {
                it.winSize = WinSize(cols, rows)
                Mono.just(true)
            } else {
                Mono.error(IllegalArgumentException("Process $it is not resizable"))
            }
        }
    }

    @SubscriptionMapping
    fun waitForProcess(@Argument id: UUID): Flux<Int> {
        return processManager.getProcess(id)
            .waitForMono()
            .flatMapMany { Flux.just(it) }
            .subscribeOn(Schedulers.boundedElastic())
    }
}
