package org.codefreak.cloud.companion

import com.pty4j.PtyProcessBuilder
import java.util.UUID
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.scheduler.Schedulers

@Service
class ProcessManager {
    private val processMap: MutableMap<UUID, Pair<Process, Flux<DataBuffer>>> = mutableMapOf()

    fun createProcess(cmd: List<String>): UUID {
        val process = generateProcess(cmd)
        val output = process.getInputStreamFlux()
            .subscribeOn(Schedulers.boundedElastic())
            .cache()
        val uid = UUID.randomUUID()
        processMap[uid] = Pair(process, output)
        println("Started new process with id=$uid")
        return uid
    }

    fun getProcess(uid: UUID): Process {
        return processMap[uid]?.first ?: throw IllegalArgumentException("There is no process $uid")
    }

    fun getOutput(uid: UUID): Flux<DataBuffer> {
        return processMap[uid]?.second ?: throw IllegalArgumentException("There is no process $uid")
    }

    private fun generateProcess(cmd: List<String>): Process {
        return PtyProcessBuilder(cmd.toTypedArray())
            .setEnvironment(
                mapOf(
                    "TERM" to "xterm"
                )
            )
            // redirect stderr to stdout so we only have to subscribe one
            .setRedirectErrorStream(true)
            .start()
    }
}