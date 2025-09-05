package apollo.kotlin.compat

import apollo.kotlin.*
import com.apollographql.apollo.api.CompiledArgumentDefinition
import com.apollographql.apollo.api.CompiledArgument as ApolloCompiledArgument
import com.apollographql.apollo.api.CompiledField as ApolloCompiledField
import com.apollographql.apollo.api.CompiledFragment as ApolloCompiledFragment
import com.apollographql.apollo.api.CompiledSelection as ApolloCompiledSelection
import com.apollographql.apollo.api.CompiledVariable as ApolloCompiledVariable
import com.apollographql.apollo.api.ObjectType as ApolloObjectType

private class Context(private val fragments: Map<String, CompiledFragmentDefinition>) {
  fun List<CompiledSelection>.toApollo(): List<ApolloCompiledSelection> {
    return map {
      when (it) {
        is CompiledField ->  {
          ApolloCompiledField.Builder(
            it.name,
            /**
             * TODO
             * - this loses key fields and more
             */
            it.type.parseType(),
            ).selections(
              it.selections.toApollo()
            ).arguments(it.arguments.toApollo())
            .build()
        }
        is CompiledFragmentSpread -> {
          val definition = fragments.get(it.name)!!

          /**
           * TODO: this breaks fake resolver, potentially more?
           */
          val possibleTypes = emptyList<String>()
          ApolloCompiledFragment.Builder(definition.typeCondition, possibleTypes).selections(definition.selections.toApollo()).build()
        }
        is CompiledInlineFragment -> {
          /**
           * TODO: this breaks fake resolver, potentially more?
           */
          val possibleTypes = emptyList<String>()
          ApolloCompiledFragment.Builder(it.typeCondition, possibleTypes).selections(it.selections.toApollo()).build()
        }
      }
    }
  }
}

internal fun List<CompiledArgument>.toApollo(): List<ApolloCompiledArgument> {
  return map {
    // TODO: this loses key args and more
    ApolloCompiledArgument.Builder(CompiledArgumentDefinition.Builder(it.name).build())
      .value(it.value.toApollo())
      .build()
  }
}

private fun CompiledValue.toApollo() : Any? {
  return when(this) {
    is CompiledBooleanValue -> value
    is CompiledEnumValue -> value
    is CompiledFloatValue -> value
    is CompiledIntValue -> value
    is CompiledListValue -> items.map { it.toApollo() }
    CompiledNullValue -> null
    is CompiledObjectValue -> fields.associate { it.name to it.value.toApollo() }
    is CompiledStringValue -> value
    is CompiledVariableValue -> ApolloCompiledVariable(name)
  }
}

internal fun CompiledDocument.toCompiledField(): ApolloCompiledField {
  val fragments = fragments.associateBy { it.name }
  val operation = operations.single()
  return ApolloCompiledField.Builder(
    name = "data",
    type = ApolloObjectType.Builder(operation.rawType).build(),
  ).selections(
    with(Context(fragments)) {
      operation.selections.toApollo()
    }
  ).build()
}