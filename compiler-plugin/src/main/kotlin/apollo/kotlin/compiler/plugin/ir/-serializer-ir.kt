package apollo.kotlin.compiler.plugin.ir

import apollo.kotlin.compiler.plugin.ApolloCache
import apollo.kotlin.compiler.plugin.CallableIds
import apollo.kotlin.compiler.plugin.ClassIds
import apollo.kotlin.compiler.plugin.Identifiers
import apollo.kotlin.compiler.plugin.ap.*
import apollo.kotlin.compiler.plugin.asIdentifier
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.classId
import org.jetbrains.kotlin.ir.util.properties
import org.jetbrains.kotlin.name.ClassId

context(visitor: ApolloIrVisitor)
fun IrBuilder.generateReadProperty(
  property: ApProperty,
  elementParameter: IrValueParameter,
  adapterClass: IrClass
): IrExpression {
  val adapter = generateDeserializer(property.type, adapterClass)
  val fromJson = visitor.pluginContext.referenceFunctions(CallableIds.Deserializer_fromJson).single()
  return if (property.isFragment) {
    /**
     * if (map.containsKey("__onNode")) {
     *   Fragment.Adapter.fromJson(element)
     * } else {
     *   null
     * }
     */
    val containsKey =
      visitor.pluginContext.referenceFunctions(CallableIds.Map_containsKey).single { it.owner.parameters.size == 2 }
    val prop = apIrCall(
      containsKey,
      irGet(elementParameter),
      apIrConst("__${property.name}")
    )
    val returnType = property.type.toIrType(adapterClass.classId!!, visitor.apolloCache).makeNullable()

    irIfThenElse(
      type = returnType,
      condition = prop,
      thenPart = apIrCall(fromJson, adapter, irGet(elementParameter)),
      elsePart = apIrNull(),
      origin = null
    )
  } else {
    // IntDeserializer.fromJson(element.get("int"))
    val get = visitor.pluginContext.referenceFunctions(CallableIds.Map_get).single { it.owner.parameters.size == 2 }
    val prop = apIrCall(
      get,
      irGet(elementParameter),
      apIrConst(property.name)
    )
    apIrCall(
      fromJson,
      adapter,
      prop
    )
  }
}

context(visitor: ApolloIrVisitor)
fun IrBuilder.generateWriteProperty(
  property: ApProperty,
  mapVariable: IrVariable,
  value: IrValueParameter,
  adapterClass: IrClass
): IrExpression {
  val serializer = generateSerializer(property.type, irGet(value), adapterClass)
  val toJson = visitor.pluginContext.referenceFunctions(CallableIds.Serializer_toJson).single()
  val modelClass = visitor.pluginContext.referenceClass(adapterClass.classId!!.parentClassId!!)
  val modelProperty = modelClass!!.owner.properties.single { it.name == property.name.asIdentifier() }
  val jsonElement = apIrCall(toJson, serializer, apIrCall(modelProperty.getter!!.symbol, irGet(value)))
  return if (property.isFragment) {
    /**
     * if (value.fragment != null) {
     *   mergeFragment(map, Fragment.Adapter.toJson(value.fragment))
     * }
     */
    irIfThen(
      type = context.irBuiltIns.unitType,
      condition = irNotEquals(apIrCall(modelProperty.getter!!.symbol, irGet(value)), apIrNull()),
      thenPart = apIrCall(CallableIds.mergeFragment, irGet(mapVariable), jsonElement),
      origin = null
    )

  } else {
    // mergeField(map, "int", IntSerializer.toJson(value.int))
    apIrCall(CallableIds.mergeField, irGet(mapVariable), apIrConst(property.name), jsonElement)
  }
}

context(visitor: ApolloIrVisitor)
fun IrBuilder.generateDeserializer(type: ApType, adapterClass: IrClass): IrExpression {
  return when (type) {
    is ApNullableType -> {
      apIrSingleConstructorCall(ClassIds.NullableAdapter, generateDeserializer(type.ofType, adapterClass))
    }

    is ApListType -> {
      apIrSingleConstructorCall(ClassIds.ListAdapter, generateDeserializer(type.ofType, adapterClass))
    }

    is ApEnumType -> TODO()
    is ApInputObjectType -> apIrError(visitor.pluginContext, "input object used in output position")
    is ApLocalModelType -> {
      apIrGetObject(
        adapterClass.classId!! // adapter
          .parentClassId!! // model
          .parentClassId!! // operation
          .createNestedClassId(type.name.asIdentifier())
          .createNestedClassId(Identifiers.Adapter)
      )
    }

    is ApFragmentType -> {
      apIrGetObject(
        visitor.apolloCache
          .getFragment(type.name)
          .createNestedClassId("Data".asIdentifier())
          .createNestedClassId(Identifiers.Adapter)
      )
    }

    is ApScalarType -> {
      return apIrGetObject(visitor.apolloCache.getScalarAdapter(type.name))
    }
  }
}


context(visitor: ApolloIrVisitor)
fun IrBuilder.generateSerializer(type: ApType, element: IrExpression, adapterClass: IrClass): IrExpression {
  return when (type) {
    is ApNullableType -> {
      apIrSingleConstructorCall(ClassIds.NullableAdapter, generateSerializer(type.ofType, element, adapterClass))
    }

    is ApListType -> {
      apIrSingleConstructorCall(ClassIds.ListAdapter, generateSerializer(type.ofType, element, adapterClass))
    }

    is ApEnumType -> TODO()
    is ApInputObjectType -> apIrError(visitor.pluginContext, "input object used in output position")
    is ApLocalModelType -> {
      apIrGetObject(
        adapterClass.classId!! // adapter
          .parentClassId!! // model
          .parentClassId!! // operation
          .createNestedClassId(type.name.asIdentifier())
          .createNestedClassId(Identifiers.Adapter)
      )
    }
    is ApFragmentType -> {
      apIrGetObject(
        visitor.apolloCache.getFragment(type.name)
          .createNestedClassId(Identifiers.Data)
          .createNestedClassId(Identifiers.Adapter)
      )
    }

    is ApScalarType -> {
      return apIrGetObject(visitor.apolloCache.getScalarAdapter(type.name))
    }
  }
}

context(visitor: ApolloIrVisitor)
internal fun ApType.toIrType(adapterClassId: ClassId, apolloCache: ApolloCache): IrType {
  val context = visitor.pluginContext
  fun ClassId.toIrClassSymbol() = context.referenceClass(this)!!

  return when (this) {
    is ApListType -> ClassIds.List.toIrClassSymbol().typeWith(
      ofType.toIrType(adapterClassId, apolloCache)
    )
    is ApEnumType -> TODO("enums are not implemented")
    is ApInputObjectType -> TODO("input objects are not implemented")
    is ApLocalModelType -> {
      adapterClassId.parentClassId!! // model
        .parentClassId!! // operation
        .createNestedClassId(name.asIdentifier()) // model
        .toIrClassSymbol().defaultType
    }
    is ApFragmentType -> {
      apolloCache.getFragment(name).createNestedClassId(Identifiers.Data).toIrClassSymbol().defaultType
    }
    is ApScalarType -> apolloCache.getScalar(name).toIrClassSymbol().defaultType
    is ApNullableType -> ofType.toIrType(adapterClassId, apolloCache).makeNullable()
  }
}