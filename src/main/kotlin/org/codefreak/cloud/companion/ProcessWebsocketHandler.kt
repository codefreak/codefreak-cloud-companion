package org.codefreak.cloud.companion

import java.util.UUID
import org.apache.commons.io.IOUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketMessage
import org.springframework.web.reactive.socket.WebSocketSession
import org.springframework.web.util.UriTemplate
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Component
class ProcessWebsocketHandler : WebSocketHandler {

    @Autowired
    private lateinit var processManager: ProcessManager

    private val uriTemplate = UriTemplate("/process/{id}")

    override fun handle(session: WebSocketSession): Mono<Void> {
        val processId = getProcessIdFromSession(session)
        val processInput = Mono.fromCallable {
            processManager.getProcess(processId)
        }.flatMapMany { process ->
            session.receive().map {
                IOUtils.copy(it.payload.asInputStream(), process.outputStream)
            }
        }.then()

        val processOutput: Flux<WebSocketMessage> = processManager.getOutput(processId)
            .map {
                WebSocketMessage(WebSocketMessage.Type.BINARY, it)
            }

        return Mono.zip(
            processInput,
            session.send(processOutput)
        ).then()
    }

    private fun getProcessIdFromSession(session: WebSocketSession): UUID {
        val path = session.handshakeInfo.uri.path
        return UUID.fromString(
            uriTemplate.match(path)["id"] ?: throw RuntimeException("Invalid process specified in URI")
        )

    }
}