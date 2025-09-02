package apollo.kotlin.compiler.plugin.ap

class ApModel(
  val name: String,
  val properties: List<ApProperty>
)

class ApField(
  val name: String,
  val model: List<ApModel>
)

class ApProperty(
  val name: String,
  val type: ApType,
  val model: ApModel?,
  val deprecationReason: String?,
  val isFragment: Boolean
)

sealed interface ApType
class ApNullableType(
  val ofType: ApType
) : ApType

class ApListType(
  val ofType: ApType
) : ApType

sealed interface ApNamedType : ApType {
  val name: String
}

class ApScalarType(
  override val name: String,
) : ApNamedType

class ApEnumType(
  override val name: String,
) : ApNamedType

class ApInputObjectType(
  override val name: String,
) : ApNamedType

class ApLocalModelType(
  override val name: String,
) : ApNamedType

class ApFragmentType(
  override val name: String,
) : ApNamedType

fun ApType.rawType(): ApNamedType {
  return when (this) {
    is ApListType -> ofType.rawType()
    is ApNullableType -> ofType.rawType()
    is ApFragmentType,
    is ApEnumType,
    is ApInputObjectType,
    is ApLocalModelType,
    is ApScalarType -> this
  }
}

fun ApProperty.allModels(): List<ApModel> {
  val models = mutableListOf<ApModel>()
  val queue = mutableListOf<ApProperty>()
  queue.add(this)
  while (queue.isNotEmpty()) {
    val p = queue.removeFirst()
    if (p.model != null) {
      models.add(p.model)
      queue.addAll(p.model.properties)
    }
  }
  return models
}