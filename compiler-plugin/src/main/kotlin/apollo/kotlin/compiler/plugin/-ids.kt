package apollo.kotlin.compiler.plugin

import apollo.kotlin.compiler.plugin.ap.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

internal val apolloPackageName = FqName("apollo.kotlin")
internal val apolloCompatPackageName = FqName("apollo.kotlin.compat")
internal val kotlinPackageName = FqName("kotlin")
internal val kotlinCollectionsPackageName = FqName("kotlin.collections")

internal object CallableIds {
  val TODO = TopLevelCallableId(kotlinPackageName, "TODO")
  val error = TopLevelCallableId(kotlinPackageName, "error")
  val emptyList = TopLevelCallableId(kotlinCollectionsPackageName, "emptyList")
  val listOf = TopLevelCallableId(kotlinCollectionsPackageName, "listOf")
  val Map_get = ClassIds.Map.callableId("get")
  val List_get = ClassIds.List.callableId("get")
  val Map_containsKey = ClassIds.Map.callableId("containsKey")

  val mutableMapOf = TopLevelCallableId(kotlinCollectionsPackageName, "mutableMapOf")
  val emptyMap = TopLevelCallableId(kotlinCollectionsPackageName, "emptyMap")

  val Deserializer_fromJson = ClassIds.Deserializer.callableId("fromJson")
  val Serializer_toJson = ClassIds.Serializer.callableId("toJson")

  val checkExpectedType = TopLevelCallableId(apolloPackageName, "assertExpectedType")
  val mergeFragment = TopLevelCallableId(apolloPackageName, "mergeFragment")
  val mergeField = TopLevelCallableId(apolloPackageName, "mergeField")

  private fun TopLevelCallableId(packageName: FqName, callableName: String, ): CallableId {
    return CallableId(packageName, Name.identifier(callableName))
  }
}

private fun ClassId.callableId(name: String) = CallableId(packageFqName, relativeClassName, Name.identifier(name))

internal object ClassIds {
  val Schema = ClassId(apolloPackageName, "Schema")
  val Scalar = ClassId(apolloPackageName, "Scalar")

  val Executable = ClassId(apolloPackageName, "Executable")
  val Query = ClassId(apolloPackageName, "Query")
  val Mutation  = ClassId(apolloPackageName, "Mutation")
  val Subscription = ClassId(apolloPackageName, "Subscription")
  val Fragment = ClassId(apolloPackageName, "Fragment")
  val FragmentCompat = ClassId(apolloCompatPackageName, "FragmentCompat")
  val FragmentCompat_Data = ClassId(apolloCompatPackageName, "FragmentCompat", "Data")

  val CompiledDocument = ClassId(apolloPackageName, "CompiledDocument")
  val CompiledOperation = ClassId(apolloPackageName, "CompiledOperation")
  val CompiledFragmentDefinition = ClassId(apolloPackageName, "CompiledFragmentDefinition")
  val CompiledOperationType = ClassId(apolloPackageName, "CompiledOperationType")
  val CompiledSelection = ClassId(apolloPackageName, "CompiledSelection")
  val CompiledField = ClassId(apolloPackageName, "CompiledField")
  val CompiledInlineFragment = ClassId(apolloPackageName, "CompiledInlineFragment")
  val CompiledFragmentSpread = ClassId(apolloPackageName, "CompiledFragmentSpread")
  val CompiledArgument = ClassId(apolloPackageName, "CompiledArgument")

  val CompiledValue = ClassId(apolloPackageName, "CompiledValue")
  val CompiledStringValue = ClassId(apolloPackageName, "CompiledStringValue")
  val CompiledIntValue = ClassId(apolloPackageName, "CompiledIntValue")
  val CompiledBooleanValue = ClassId(apolloPackageName, "CompiledBooleanValue")
  val CompiledFloatValue = ClassId(apolloPackageName, "CompiledFloatValue")
  val CompiledVariableValue = ClassId(apolloPackageName, "CompiledVariableValue")
  val CompiledEnumValue = ClassId(apolloPackageName, "CompiledEnumValue")
  val CompiledNullValue = ClassId(apolloPackageName, "CompiledNullValue")
  val CompiledListValue = ClassId(apolloPackageName, "CompiledListValue")
  val CompiledObjectValue = ClassId(apolloPackageName, "CompiledObjectValue")
  val CompiledObjectValueField = ClassId(apolloPackageName, "CompiledObjectValueField")

  val Adapter = ClassId(apolloPackageName, "Adapter")
  val Deserializer = ClassId(apolloPackageName, "Deserializer")
  val Serializer = ClassId(apolloPackageName, "Serializer")

  val JsonElement = ClassId(apolloPackageName, "JsonElement")

  val IntAdapter = ClassId(apolloPackageName, "IntAdapter")
  val StringAdapter = ClassId(apolloPackageName, "StringAdapter")
  val FloatAdapter = ClassId(apolloPackageName, "FloatAdapter")
  val BooleanAdapter = ClassId(apolloPackageName, "BooleanAdapter")
  val ListAdapter = ClassId(apolloPackageName, "ListAdapter")
  val NullableAdapter = ClassId(apolloPackageName, "NullableAdapter")

  val Int = ClassId(kotlinPackageName, "Int")
  val String = ClassId(kotlinPackageName, "String")
  val Boolean = ClassId(kotlinPackageName, "Boolean")
  val Double = ClassId(kotlinPackageName, "Double")
  val Any = ClassId(kotlinPackageName, "Any")
  val Nothing = ClassId(kotlinPackageName, "Nothing")
  val Unit = ClassId(kotlinPackageName, "Unit")

  val List = ClassId(kotlinCollectionsPackageName, "List")
  val Map = ClassId(kotlinCollectionsPackageName, "Map")

  private fun ClassId(packageName: FqName, name: String, vararg nested: String): ClassId {
    var classId = ClassId(packageName, Name.identifier(name))

    for (name in nested) {
      classId = classId.createNestedClassId(Name.identifier(name))
    }

    return classId
  }
}

fun ClassId.toConeClassLikeType(
  vararg typeArguments: ConeTypeProjection,
  isMarkedNullable: Boolean = false,
  attributes: ConeAttributes = ConeAttributes.Empty,
): ConeClassLikeType {
  return ConeClassLikeTypeImpl(this.toLookupTag(), typeArguments, isMarkedNullable, attributes)
}

internal fun ApType.toConeKotlinType(containingClass: ClassId, apolloCache: ApolloCache): ConeKotlinType {
  return when (this) {
    is ApListType -> ClassIds.List.toConeClassLikeType(
      ofType.toConeKotlinType(containingClass, apolloCache),
    )
    is ApEnumType -> TODO("enums are not implemented")
    is ApInputObjectType -> TODO("input objects are not implemented")
    is ApLocalModelType -> containingClass.createNestedClassId(Name.identifier(name)).toConeClassLikeType()
    is ApFragmentType -> {
      val fragmentDefinition = apolloCache.getFragmentDefinition(name)
      val fragment = apolloCache.getFragment(name)
      if (fragmentDefinition == null || fragment == null) {
        ClassIds.Nothing.toConeClassLikeType()
      } else {
        fragment.createNestedClassId(Identifiers.Data).toConeClassLikeType().let {
          if (fragmentDefinition.typeCondition.name == this.parentType) {
            // TODO refine this, we can make this assumption in more cases
            it
          } else {
            it.copy(nullable = true)
          }
        }
      }
    }
    is ApScalarType -> apolloCache.getScalar(name).toConeClassLikeType()
    is ApNullableType -> ofType.toConeKotlinType(containingClass, apolloCache).copy(nullable = true)
  }
}

internal fun ConeKotlinType.copy(nullable: Boolean): ConeKotlinType {
  check(this is ConeClassLikeTypeImpl)
  return ConeClassLikeTypeImpl(
    lookupTag = this.lookupTag,
    typeArguments = this.typeArguments,
    isMarkedNullable = nullable,
    attributes = attributes
  )
}

internal fun String.asIdentifier() = Name.identifier(this)
internal fun List<String>.asFqName() = FqName.fromSegments(this)
internal fun String.classId(): ClassId {
  val components = this.split(".")
  check(components.isNotEmpty())
  var index = components.indexOfFirst { it.get(0).isUpperCase() }
  if (index < 0) {
    index = components.size - 1
  }
  return ClassId(components.subList(0, index).asFqName(), components.subList(index, components.size).asFqName(), false)
}