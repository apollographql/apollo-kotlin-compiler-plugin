package apollo.kotlin.compiler.plugin.ir

import apollo.kotlin.compiler.plugin.CallableIds
import apollo.kotlin.compiler.plugin.ClassIds
import apollo.kotlin.compiler.plugin.Identifiers
import cast.cast
import com.apollographql.apollo.ast.GQLArgument
import com.apollographql.apollo.ast.GQLBooleanValue
import com.apollographql.apollo.ast.GQLDocument
import com.apollographql.apollo.ast.GQLEnumTypeDefinition
import com.apollographql.apollo.ast.GQLEnumValue
import com.apollographql.apollo.ast.GQLExecutableDefinition
import com.apollographql.apollo.ast.GQLField
import com.apollographql.apollo.ast.GQLFloatValue
import com.apollographql.apollo.ast.GQLFragmentDefinition
import com.apollographql.apollo.ast.GQLFragmentSpread
import com.apollographql.apollo.ast.GQLInlineFragment
import com.apollographql.apollo.ast.GQLIntValue
import com.apollographql.apollo.ast.GQLListType
import com.apollographql.apollo.ast.GQLListValue
import com.apollographql.apollo.ast.GQLNamedType
import com.apollographql.apollo.ast.GQLNonNullType
import com.apollographql.apollo.ast.GQLNullValue
import com.apollographql.apollo.ast.GQLObjectValue
import com.apollographql.apollo.ast.GQLOperationDefinition
import com.apollographql.apollo.ast.GQLScalarTypeDefinition
import com.apollographql.apollo.ast.GQLSelection
import com.apollographql.apollo.ast.GQLStringValue
import com.apollographql.apollo.ast.GQLType
import com.apollographql.apollo.ast.GQLValue
import com.apollographql.apollo.ast.GQLVariableValue
import com.apollographql.apollo.ast.Schema
import com.apollographql.apollo.ast.definitionFromScope
import com.apollographql.apollo.ast.rawType
import com.apollographql.apollo.ast.toUtf8
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.declarations.IrEnumEntry
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrGetEnumValueImpl
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.util.SYNTHETIC_OFFSET
import org.jetbrains.kotlin.ir.util.companionObject
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.isVararg
import org.jetbrains.kotlin.ir.util.properties

/**
 * ```kotlin
 * class CompiledOperation(
 *   val name: String,
 *   val operationType: CompiledOperationType,
 *   val selections: List<CompiledSelection>,
 *   // extra
 *   val rootType: String
 * )
 * ```
 */
context(visitor: ApolloIrVisitor)
fun IrBuilderWithScope.generateCompiledOperationExpression(operation: GQLOperationDefinition): IrExpression {
  val typename = visitor.schema.rootTypeNameFor(operation.operationType)
  return apIrSingleConstructorCall(
    ClassIds.CompiledOperation,
    apIrConst(operation.name ?: ""),
    generateCompiledOperationType(operation.operationType),
    generateCompiledSelections(operation.selections, typename),
    apIrConst(typename)
  )
}

/**
 * ```kotlin
 * class CompiledFragmentDefinition(
 *   val name: String,
 *   val typeCondition: String,
 *   val selections: List<CompiledSelection>
 * )
 * ```
 */
context(visitor: ApolloIrVisitor)
fun IrBuilderWithScope.generateCompiledFragmentExpression(fragmentDefinition: GQLFragmentDefinition): IrExpression {
  val typeCondition = fragmentDefinition.typeCondition.name
  return apIrSingleConstructorCall(
    ClassIds.CompiledFragmentDefinition,
    apIrConst(fragmentDefinition.name),
    apIrConst(typeCondition),
    generateCompiledSelections(fragmentDefinition.selections, typeCondition),
  )
}

context(visitor: ApolloIrVisitor)
fun IrBuilderWithScope.generateCompiledSelections(selections: List<GQLSelection>, parentType: String): IrExpression {
  return apIrList(ClassIds.CompiledSelection, selections.map {
    when (it) {
      is GQLField -> generateCompiledField(it, parentType)
      is GQLInlineFragment -> generateCompiledInlineFragment(it, parentType)
      is GQLFragmentSpread -> generateCompiledFragmentSpread(it)
    }
  })
}

/**
 * ```kotlin
 * class CompiledField(
 *   val alias: String?,
 *   val name: String,
 *   val arguments: List<CompiledArgument>,
 *   val selections: List<CompiledSelection>,
 *   val type: String
 * ) : CompiledSelection()
 * ```
 */
context(visitor: ApolloIrVisitor)
fun IrBuilderWithScope.generateCompiledField(field: GQLField, parentType: String): IrExpression {
  val fieldDefinition = field.definitionFromScope(visitor.schema, parentType)!!
  return apIrSingleConstructorCall(
    ClassIds.CompiledField,
    field.alias?.let { apIrConst(it) } ?: apIrNull(),
    apIrConst(field.name),
    apIrList(ClassIds.CompiledArgument, field.arguments.map { generateCompiledArgument(it) }),
    generateCompiledSelections(field.selections, fieldDefinition.type.rawType().name),
    apIrConst(fieldDefinition.type.encode(visitor.schema))
  )
}

/**
 * Encodes whether a given type is a scalar or not for usage in the Normalizer
 *
 * `[String]!` will become `[-String]!`
 */
private fun GQLType.encode(schema: Schema): String {
  return StringBuilder().apply { encode(this, schema) }.toString()


}

private fun GQLType.encode(builder: StringBuilder, schema: Schema) {
  when (this) {
    is GQLListType -> {
      builder.append('[')
      type.encode(builder, schema)
      builder.append(']')
    }

    is GQLNamedType -> {
      val definition = schema.typeDefinition(name)
      when(definition) {
        is GQLScalarTypeDefinition,
          is GQLEnumTypeDefinition -> {
            builder.append('-')
          }
        else -> Unit
      }
      builder.append(name)
    }

    is GQLNonNullType -> {
      type.encode(builder, schema)
      builder.append('!')
    }
  }
}

/**
 * ```kotlin
 * class CompiledInlineFragment(
 *   val typeCondition: String,
 *   val selections: List<CompiledSelection>,
 * )
 * ```
 */
context(visitor: ApolloIrVisitor)
fun IrBuilderWithScope.generateCompiledInlineFragment(
  inlineFragment: GQLInlineFragment,
  parentType: String
): IrExpression {
  val typeCondition = inlineFragment.typeCondition?.name ?: parentType
  return apIrSingleConstructorCall(
    ClassIds.CompiledInlineFragment,
    apIrConst(typeCondition),
    generateCompiledSelections(inlineFragment.selections, typeCondition),
  )
}


context(visitor: ApolloIrVisitor)
fun IrBuilderWithScope.generateCompiledFragmentSpread(fragmentSpread: GQLFragmentSpread): IrExpression {
  return apIrSingleConstructorCall(
    ClassIds.CompiledFragmentSpread,
    apIrConst(fragmentSpread.name),
  )
}

/**
 * ```kotlin
 * class CompiledArgument(
 *   val name: String,
 *   val value: CompiledValue,
 * )
 * ```
 */
context(visitor: ApolloIrVisitor)
fun IrBuilderWithScope.generateCompiledArgument(argument: GQLArgument): IrExpression {
  return apIrSingleConstructorCall(
    ClassIds.CompiledArgument,
    apIrConst(argument.name),
    generateCompiledValue(argument.value)
  )
}

context(visitor: ApolloIrVisitor)
fun IrBuilderWithScope.generateCompiledValue(argument: GQLValue): IrExpression {
  return when (argument) {
    is GQLStringValue -> {
      apIrSingleConstructorCall(
        ClassIds.CompiledStringValue,
        apIrConst(argument.value),
      )
    }

    is GQLIntValue -> {
      apIrSingleConstructorCall(
        ClassIds.CompiledIntValue,
        apIrConst(argument.value),
      )
    }

    is GQLBooleanValue -> {
      apIrSingleConstructorCall(
        ClassIds.CompiledBooleanValue,
        apIrConst(argument.value),
      )
    }

    is GQLFloatValue -> {
      apIrSingleConstructorCall(
        ClassIds.CompiledFloatValue,
        apIrConst(argument.value),
      )
    }

    is GQLNullValue -> {
      irGetObject(visitor.pluginContext.referenceClass(ClassIds.CompiledNullValue)!!)
    }

    is GQLEnumValue -> {
      apIrSingleConstructorCall(
        ClassIds.CompiledEnumValue,
        apIrConst(argument.value),
      )
    }

    is GQLListValue -> {
      apIrSingleConstructorCall(
        ClassIds.CompiledListValue,
        apIrList(ClassIds.CompiledValue, argument.values.map { generateCompiledValue(it) }),
      )
    }

    is GQLObjectValue -> {
      apIrSingleConstructorCall(
        ClassIds.CompiledObjectValue,
        // TODO: the List here doesn't have the <Entry> type argument, not sure how bad that is
        apIrList(ClassIds.List, argument.fields.map {
          apIrSingleConstructorCall(
            ClassIds.CompiledObjectValueField,
            apIrConst(it.name),
            generateCompiledValue(it.value)
          )
        }),
      )
    }

    is GQLVariableValue -> {
      apIrSingleConstructorCall(
        ClassIds.CompiledVariableValue,
        apIrConst(argument.name),
      )
    }
  }
}

/**
 * ```kotlin
 * enum class CompiledOperationType {
 *   Query,
 *   Mutation,
 *   Subscription
 * }
 * ```
 */
context(visitor: ApolloIrVisitor)
fun generateCompiledOperationType(operationType: String): IrExpression {
  val irClass = visitor.pluginContext.referenceClass(ClassIds.CompiledOperationType)!!

  return IrGetEnumValueImpl(
    SYNTHETIC_OFFSET,
    SYNTHETIC_OFFSET,
    irClass.owner.defaultType,
    irClass.owner.declarations.filterIsInstance<IrEnumEntry>()
      .first { it.name.asString().lowercase() == operationType }.symbol
  )
}

/**
 * ```kotlin
 * class CompiledDocument(
 *   val operations: List<CompiledOperation>,
 *   val fragments: List<CompiledFragmentDefinition>
 * )
 * ```
 */
context(visitor: ApolloIrVisitor)
fun IrBuilderWithScope.generateCompiledDocumentExpression(definition: GQLExecutableDefinition): IrExpression {
  val operations: IrExpression
  val fragments: IrExpression
  when (definition) {
    is GQLOperationDefinition -> {
      operations = apIrCall(listOfSingleSymbol, generateCompiledOperationExpression(definition))
      // TODO maybe break the operation/fragment symmetry and have the fragments store only a single CompiledFragment in their companion
      fragments = apIrList(ClassIds.CompiledFragmentDefinition, definition.collectFragments())
    }

    is GQLFragmentDefinition -> {
      operations = apIrCall(emptyListSymbol)
      fragments = apIrCall(listOfSingleSymbol, generateCompiledFragmentExpression(definition))
    }
  }

  return apIrSingleConstructorCall(
    ClassIds.CompiledDocument,
    operations,
    fragments
  )
}

context(visitor: ApolloIrVisitor, scope: IrBuilderWithScope)
fun GQLOperationDefinition.collectFragments(): List<IrExpression> {
  val selections = mutableListOf<GQLSelection>()
  val visitedFragments = mutableSetOf<String>()
  val result = mutableListOf<IrExpression>()
  selections.addAll(this@collectFragments.selections)

  while (selections.isNotEmpty()) {
    when (val selection = selections.removeFirst()) {
      is GQLField -> {
        selections.addAll(selection.selections)
      }

      is GQLFragmentSpread -> {
        if (selection.name !in visitedFragments) {
          visitedFragments.add(selection.name)

          val definition = visitor.apolloCache.getFragmentDefinition(selection.name)
          selections.addAll(definition.selections)

          val fragment = visitor.apolloCache.getFragment(selection.name)
          val companion = visitor.pluginContext.referenceClass(fragment)!!
            .owner
            .companionObject()!!


          val expression = with(scope) {
            // Fragment.Companion
            val var1 = irGetObject(companion.symbol)

            // ...Companion.compiledDocument
            val property = companion.properties.first { it.name == Identifiers.compiledDocumentName }
            val var2 = apIrCall(property.getter!!.symbol, var1)

            // ...compiledDocument.getFragments()
            val fragmentsProperty =
              visitor.pluginContext.referenceClass(ClassIds.CompiledDocument)!!.owner.properties.single { it.name.asString() == "fragments" }
            val var3 = apIrCall(fragmentsProperty.getter!!.symbol, var2)

            // ...fragments.get(0)
            val get =
              visitor.pluginContext.referenceFunctions(CallableIds.List_get).single { it.owner.parameters.size == 2 }
            apIrCall(get, var3, apIrConst(0))
          }

          result.add(expression)
        }
      }

      is GQLInlineFragment -> {
        selections.addAll(selection.selections)
      }
    }
  }
  return result
}

context(visitor: ApolloIrVisitor)
val listOfSingleSymbol: IrSimpleFunctionSymbol
  get() {
    return visitor.pluginContext.referenceFunctions(CallableIds.listOf)
      .single { it.owner.parameters.size == 1 && !it.owner.parameters.get(0).isVararg }
  }

context(visitor: ApolloIrVisitor)
val listOfManySymbol: IrSimpleFunctionSymbol
  get() {
    return visitor.pluginContext.referenceFunctions(CallableIds.listOf)
      .single { it.owner.parameters.size == 1 && it.owner.parameters.get(0).isVararg }
  }

context(visitor: ApolloIrVisitor)
val emptyListSymbol: IrSimpleFunctionSymbol
  get() = visitor.pluginContext.referenceFunctions(CallableIds.emptyList).single()

context(visitor: ApolloIrVisitor)
val emptyMapSymbol: IrSimpleFunctionSymbol
  get() = visitor.pluginContext.referenceFunctions(CallableIds.emptyMap).single()
