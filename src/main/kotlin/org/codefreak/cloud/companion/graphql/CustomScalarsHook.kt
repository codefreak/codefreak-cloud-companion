package org.codefreak.cloud.companion.graphql

import com.expediagroup.graphql.generator.hooks.SchemaGeneratorHooks
import graphql.schema.GraphQLType
import java.util.UUID
import kotlin.reflect.KClass
import kotlin.reflect.KType
import org.springframework.stereotype.Component

@Component
class CustomScalarsHook : SchemaGeneratorHooks {
    override fun willGenerateGraphQLType(type: KType): GraphQLType? = when (type.classifier as? KClass<*>) {
        UUID::class -> UUID_SCALAR
        else -> null
    }
}
