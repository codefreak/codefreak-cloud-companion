package org.codefreak.cloud.companion.graphql.api

import java.time.Duration
import org.codefreak.cloud.companion.ProcessManager
import org.codefreak.cloud.companion.graphql.BasicGraphqlTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import reactor.core.publisher.Flux
import reactor.test.StepVerifier

internal class ProcessControllerTest(
    @Autowired private val processManager: ProcessManager
) : BasicGraphqlTest() {

    @AfterEach
    fun beforeEach() {
        // kill all processes after each test
        Flux.fromIterable(processManager.getProcesses()).flatMap { (id) -> processManager.purgeProcess(id) }.blockLast()
    }

    @Test
    fun `startProcess starts a new process`() {
        graphQlTester.query("mutation { startProcess(cmd: [\"/bin/bash\"]){ id } }")
            .execute()
            .path("startProcess.id")
            .pathExists()
            .valueIsNotEmpty()
    }

    @Test
    fun killProcess() {
        val id = processManager.createProcess(listOf("/bin/bash"))
        graphQlTester.query("mutation { killProcess(id: \"${id}\") }")
            .execute()
            .path("killProcess")
            .pathExists()
            .valueIsNotEmpty()
            .matchesJson("137")
    }

    @Test
    fun resizeProcess() {
        val id = processManager.createProcess(listOf("/bin/bash"))
        graphQlTester.query("mutation { resizeProcess(id: \"${id}\", cols: 111, rows: 111) }")
            .execute()
            .path("resizeProcess")
            .pathExists()
            .valueIsNotEmpty()
        // write bash's cols and rows to stdout and wait 3 seconds for it to arrive
        processManager.getStdin(id).writer().let {
            it.write("echo $(tput cols) $(tput lines)\r\n")
            it.flush()
        }
        StepVerifier.create(processManager.getStdout(id)
            .map { it.asInputStream().readAllBytes().decodeToString() }
            .filter { it.contains("111 111") }
        ).expectNextCount(1)
            .thenCancel()
            .verify(Duration.ofSeconds(3))
    }

    @Test
    fun waitForProcess() {
        val id = processManager.createProcess(listOf("/bin/bash"))
        val subFlux = graphQlTester.query("subscription { waitForProcess(id: \"${id}\") }")
            .executeSubscription()
            .toFlux()
        StepVerifier.create(subFlux)
            .then {
                processManager.getStdin(id).writer().let {
                    it.write("exit 111\r\n")
                    it.flush()
                }
            }
            .consumeNextWith { it.path("waitForProcess").matchesJson("111") }
            .expectComplete()
            .verify(Duration.ofSeconds(3))
    }
}
