package org.codefreak.cloud.companion.graphql

import graphql.language.StringValue
import graphql.schema.Coercing
import graphql.schema.CoercingParseLiteralException
import graphql.schema.CoercingParseValueException
import graphql.schema.GraphQLScalarType
import java.util.UUID

val UUID_SCALAR: GraphQLScalarType =
    GraphQLScalarType.newScalar()
        .name("UUID")
        .description("A type representing a formatted java.util.UUID")
        .coercing(UuidConverter())
        .build()

class UuidConverter : Coercing<UUID, String> {
    override fun parseValue(input: Any?): UUID {
        return UUID.fromString(input.toString()) ?: throw CoercingParseValueException(
            "Expected type 'UUID' but was '${input?.javaClass?.simpleName}'."
        )
    }

    override fun parseLiteral(input: Any?): UUID {
        if (input is StringValue) {
            return UUID.fromString(input.value)
                ?: throw CoercingParseLiteralException("Unable to turn AST input into a 'UUID' : '$input'")
        }
        throw CoercingParseLiteralException(
            "Expected AST type 'StringValue' but was '${input?.javaClass?.simpleName}'."
        )
    }

    override fun serialize(dataFetcherResult: Any?): String {
        return dataFetcherResult.toString()
    }
}
