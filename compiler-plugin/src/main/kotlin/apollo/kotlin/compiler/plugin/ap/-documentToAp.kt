package apollo.kotlin.compiler.plugin.ap

import com.apollographql.apollo.ast.GQLDefinition
import com.apollographql.apollo.ast.GQLDocument
import com.apollographql.apollo.ast.GQLEnumTypeDefinition
import com.apollographql.apollo.ast.GQLField
import com.apollographql.apollo.ast.GQLFragmentDefinition
import com.apollographql.apollo.ast.GQLFragmentSpread
import com.apollographql.apollo.ast.GQLInlineFragment
import com.apollographql.apollo.ast.GQLInputObjectTypeDefinition
import com.apollographql.apollo.ast.GQLInterfaceTypeDefinition
import com.apollographql.apollo.ast.GQLListType
import com.apollographql.apollo.ast.GQLNamedType
import com.apollographql.apollo.ast.GQLNonNullType
import com.apollographql.apollo.ast.GQLObjectTypeDefinition
import com.apollographql.apollo.ast.GQLOperationDefinition
import com.apollographql.apollo.ast.GQLScalarTypeDefinition
import com.apollographql.apollo.ast.GQLSelection
import com.apollographql.apollo.ast.GQLType
import com.apollographql.apollo.ast.GQLUnionTypeDefinition
import com.apollographql.apollo.ast.Schema
import com.apollographql.apollo.ast.definitionFromScope
import com.apollographql.apollo.ast.findDeprecationReason
import com.apollographql.apollo.ast.rawType
import com.apollographql.apollo.ast.responseName

private class Context(val schema: Schema) {
  fun convert(definition: GQLDefinition): ApProperty {
    val type: String
    val selections: List<GQLSelection>

    if (definition is GQLOperationDefinition) {
      type = schema.rootTypeNameFor(definition.operationType)
      selections = definition.selections
    } else {
      definition as GQLFragmentDefinition
      type = definition.typeCondition.name
      selections = definition.selections
    }

    return walkField(
      name = "data",
      type = GQLNamedType(null, type),
      deprecationReason = null,
      selections = selections,
      false
    )
  }

  private fun walkField(
    name: String, // name of the field
    type: GQLType,
    deprecationReason: String?,
    selections: List<GQLSelection>,
    isFragment: Boolean
  ): ApProperty {
    val contextType = type.rawType().name
    val mergedFields = selections.groupBy {
      when (it) {
        is GQLField -> it.responseName()
        is GQLFragmentSpread -> it.name
        is GQLInlineFragment -> "on${it.typeCondition?.name ?: contextType}"
      }
    }
    val properties = mergedFields.entries.map { mergedFieldEntry ->
      val list = mergedFieldEntry.value
      when (val first = list.first()) {
        is GQLField -> {
          val subSelections = list.flatMap { (it as GQLField).selections }
          val definition = first.definitionFromScope(schema, contextType)!!
          walkField(
            name = mergedFieldEntry.key,
            type = definition.type,
            deprecationReason = definition.directives.findDeprecationReason(),
            selections = subSelections,
            isFragment = false,
          )
        }

        is GQLInlineFragment -> {
          val subSelections = list.flatMap { (it as GQLInlineFragment).selections }
          walkField(
            name = mergedFieldEntry.key,
            // TODO make this non-nullable if the condition is always satisfied
            type = GQLNamedType(name = (first.typeCondition?.name ?: contextType)),
            deprecationReason = null,
            selections = subSelections,
            isFragment = true
          )
        }

        is GQLFragmentSpread -> {
          ApProperty(
            name = mergedFieldEntry.key,
            // TODO make this non-nullable if the condition is always satisfied
            type = ApNullableType(ApFragmentType(name = first.name)),
            model = null,
            deprecationReason = null,
            isFragment = true,
          )
        }
      }
    }
    val apType = type.toApType {
      name.capitalizeFirstLetter()
    }
    val model = if (properties.isNotEmpty()) {
      ApModel(
        name = apType.rawType().name,
        properties = properties
      )
    } else {
      null
    }

    return ApProperty(
      name = name,
      type = apType,
      deprecationReason = deprecationReason,
      model = model,
      isFragment = isFragment
    )
  }

  fun GQLType.toApType2(modelName: () -> String): ApType {
    return when (this) {
      is GQLListType -> ApListType(type.toApType(modelName))
      is GQLNamedType -> {
        val typeDefinition = schema.typeDefinition(name)
        when (typeDefinition) {
          is GQLEnumTypeDefinition -> ApEnumType(name)
          is GQLInputObjectTypeDefinition -> ApInputObjectType(name)
          is GQLInterfaceTypeDefinition,
          is GQLObjectTypeDefinition,
          is GQLUnionTypeDefinition -> ApLocalModelType(modelName())
          is GQLScalarTypeDefinition -> ApScalarType(name)
        }
      }
      is GQLNonNullType -> error("")
    }
  }

  fun GQLType.toApType(name: () -> String): ApType {
    return if (this is GQLNonNullType) {
      this.type.toApType2(name)
    } else {
      return ApNullableType(toApType2(name))
    }
  }
}

internal fun documentToAp(schema: Schema, definition: GQLDefinition): ApProperty {
  return Context(schema).convert(definition)
}

