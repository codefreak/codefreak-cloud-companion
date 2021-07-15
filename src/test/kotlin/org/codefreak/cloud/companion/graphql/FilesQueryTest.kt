package org.codefreak.cloud.companion.graphql

import kotlin.io.path.writeText
import org.apache.commons.io.FileUtils
import org.codefreak.cloud.companion.FileService
import org.hamcrest.Matchers
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.reactive.server.WebTestClient

@SpringBootTest
@AutoConfigureWebTestClient
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("test")
@ExtendWith(SpringExtension::class)
internal class FilesQueryTest(
    @Autowired private val testClient: WebTestClient,
    @Autowired private val fileService: FileService
) {

    @AfterEach
    fun tearDown() {
        // ensure clean directory after each test
        FileUtils.cleanDirectory(fileService.resolve("/").toFile())
    }

    @Test
    fun `list files delivers files`() {
        fileService.resolve("/test").writeText("Hello World")
        fileService.resolve("/test2").writeText("Hello World")
        testClient.post()
            .uri("/graphql")
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType("application", "graphql"))
            .bodyValue("query { listFiles(path: \"/\"){ __typename, path } }")
            .exchange()
            .expectBody()
            .jsonPath("$.data.listFiles").value(Matchers.hasSize<Any>(2))
    }
}
