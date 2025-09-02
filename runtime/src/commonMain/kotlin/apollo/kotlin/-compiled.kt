package apollo.kotlin

sealed class CompiledSelection

class CompiledField(
  val alias: String?,
  val name: String,
  val arguments: List<CompiledArgument>,
  val selections: List<CompiledSelection>,

  // Below is extra information coming from the schema
  /**
   * The type as a GraphQL type
   */
  val type: String
) : CompiledSelection()

class CompiledInlineFragment(
  val typeCondition: String,
  val selections: List<CompiledSelection>,
) : CompiledSelection()

class CompiledFragmentSpread(
  val name: String
) : CompiledSelection()

sealed interface CompiledValue

@JvmInline
value class CompiledVariableValue(val name: String): CompiledValue
@JvmInline
value class CompiledEnumValue(val value: String): CompiledValue
@JvmInline
value class CompiledStringValue(val value: String): CompiledValue
@JvmInline
value class CompiledIntValue(val value: Int): CompiledValue
@JvmInline
value class CompiledFloatValue(val value: Double): CompiledValue
@JvmInline
value class CompiledBooleanValue(val value: Boolean): CompiledValue
class CompiledListValue(val items:List<CompiledValue>): CompiledValue
class CompiledObjectValue(val fields: List<Field>): CompiledValue {
  class Field(val name: String, val value: CompiledValue)
}
data object CompiledNullValue: CompiledValue

class CompiledArgument(
  val name: String,
  /**
   * The compile-time value of that argument.
   *
   * May contain variables.
   */
  val value: CompiledValue,
)

enum class CompiledOperationType {
  Query,
  Mutation,
  Subscription
}

class CompiledOperation(
  val name: String,
  val operationType: CompiledOperationType,
  val selections: List<CompiledSelection>,

  // Below is extra information coming from the schema

  val rawType: String
)

class CompiledDocument(
  val operations: List<CompiledOperation>,
  val fragments: List<CompiledFragmentDefinition>
)

class CompiledFragmentDefinition(
  val name: String,
  val typeCondition: String,
  val selections: List<CompiledSelection>
)