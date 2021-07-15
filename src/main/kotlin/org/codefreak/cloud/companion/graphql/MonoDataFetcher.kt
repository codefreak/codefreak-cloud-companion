package org.codefreak.cloud.companion.graphql

import com.expediagroup.graphql.generator.execution.FunctionDataFetcher
import com.expediagroup.graphql.generator.execution.SimpleKotlinDataFetcherFactoryProvider
import com.fasterxml.jackson.databind.ObjectMapper
import graphql.schema.DataFetcherFactory
import graphql.schema.DataFetchingEnvironment
import kotlin.reflect.KFunction
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

/**
 * Data Fetcher that will convert Mono instances into CompletableFuture.
 * See here why this is needed:
 *
 * https://opensource.expediagroup.com/graphql-kotlin/docs/3.x.x/schema-generator/execution/async-models/#rxjavareactor
 */
class MonoDataFetcher(target: Any?, fn: KFunction<*>, objectMapper: ObjectMapper) :
    FunctionDataFetcher(target, fn, objectMapper) {
    override fun get(environment: DataFetchingEnvironment): Any? = when (val result = super.get(environment)) {
        is Mono<*> -> result.toFuture()
        else -> result
    }
}

/**
 * Overrides the default fetcher factory
 */
@Component
class MonoDataFetcherFactoryProvider(
    private val objectMapper: ObjectMapper
) : SimpleKotlinDataFetcherFactoryProvider(objectMapper) {

    override fun functionDataFetcherFactory(target: Any?, kFunction: KFunction<*>) = DataFetcherFactory {
        MonoDataFetcher(target, kFunction, objectMapper)
    }
}
