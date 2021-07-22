package org.codefreak.cloud.companion

import com.pty4j.PtyProcessBuilder
import java.io.OutputStream
import java.util.UUID
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.scheduler.Schedulers

@Service
class ProcessManager {
    private val processMap: MutableMap<UUID, Process> = mutableMapOf()
    private val outputStreamCache: MutableMap<UUID, Flux<DataBuffer>> = mutableMapOf()

    fun createProcess(cmd: List<String>): UUID {
        val uid = UUID.randomUUID()
        processMap[uid] = generateProcess(cmd)
        return uid
    }

    fun getProcess(uid: UUID): Process {
        return processMap[uid] ?: throw IllegalArgumentException("There is no process $uid")
    }

    fun getStdout(uid: UUID): Flux<DataBuffer> {
        return outputStreamCache.computeIfAbsent(uid) {
            getProcess(uid)
                .getInputStreamFlux()
                .subscribeOn(Schedulers.boundedElastic())
                .cache()
        }
    }

    fun getStdin(uid: UUID): OutputStream {
        return getProcess(uid).outputStream
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
