package apollo.kotlin.compiler.plugin.ir

import apollo.kotlin.compiler.plugin.ApolloCache
import apollo.kotlin.compiler.plugin.CallableIds
import apollo.kotlin.compiler.plugin.ClassIds
import apollo.kotlin.compiler.plugin.Identifiers
import apollo.kotlin.compiler.plugin.ap.ApModel
import apollo.kotlin.compiler.plugin.asIdentifier
import apollo.kotlin.compiler.plugin.fir.Keys
import cast.cast
import cast.safeAs
import com.apollographql.apollo.ast.GQLDefinition
import com.apollographql.apollo.ast.GQLDocument
import com.apollographql.apollo.ast.GQLExecutableDefinition
import com.apollographql.apollo.ast.Schema
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrVariableImpl
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.expressions.impl.IrDelegatingConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrVariableSymbolImpl
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.impl.IrStarProjectionImpl
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.SYNTHETIC_OFFSET
import org.jetbrains.kotlin.ir.util.classId
import org.jetbrains.kotlin.ir.util.companionObject
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.util.properties
import org.jetbrains.kotlin.ir.util.superClass
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import kotlin.collections.map
import kotlin.collections.toTypedArray

class ApolloIrVisitor(val schema: Schema, val apolloCache: ApolloCache, val pluginContext: IrPluginContext) : IrVisitorVoid() {
  override fun visitElement(element: IrElement) {
    when (element) {
      is IrDeclaration,
      is IrFile,
      is IrModuleFragment -> element.acceptChildrenVoid(this)

      else -> {}
    }
  }

  override fun visitClass(declaration: IrClass) {
    if (declaration.isExecutable()) {
      // Operation
      declaration.functions.single { it.name == Identifiers.compiledDocumentName }.let { function ->
        function.body = generateCompiledDocumentFunction(function, declaration)
      }
      declaration.functions.single { it.name == Identifiers.variablesName }.let { function ->
        function.body = generateVariablesFunction(function)
      }
      declaration.functions.single { it.name == Identifiers.dataAdapterName }.let { function ->
        function.body = generateDataAdapterFunction(function, declaration)
      }

      val companion = declaration.companionObject()!!
      val key = companion.origin.cast<IrDeclarationOrigin.GeneratedByPlugin>().pluginKey.cast<Keys.OperationCompanion>()
      val field = companion
        .properties
        .single { it.name == Identifiers.compiledDocumentName }
        .backingField!!
      field.initializer = generateCompiledDocumentInitializer(key.definition, field)

      // keep going
      super.visitClass(declaration)
      return
    }

    val key = declaration.origin.safeAs<IrDeclarationOrigin.GeneratedByPlugin>()?.pluginKey
    if (key is Keys.OperationCompanion) {
      val constructor = declaration.constructors.single()
      constructor.body = apIrConstructor(pluginContext, constructor) {}
      return
    }

    if (key is Keys.Model) {
      val constructor = declaration.constructors.single()
      constructor.body = apIrConstructor(pluginContext, constructor) {
        val properties = declaration.properties.toList()
        constructor.parameters.forEachIndexed { index, parameter ->
          +irSetField(irGet(declaration.thisReceiver!!), properties.get(index).backingField!!, irGet(parameter))
        }
      }
      // keep going
      super.visitClass(declaration)
      return
    }

    if (key is Keys.ModelAdapter) {
      // Model adapter
      declaration.functions.single { it.name == Identifiers.fromJson }.let { function ->
        function.body = generateFromJson(function, declaration, key.model)
      }
      declaration.functions.single { it.name == Identifiers.toJson }.let { function ->
        function.body = generateToJson(function, declaration, key.model)
      }

      val constructor = declaration.constructors.single()
      constructor.body = apIrConstructor(pluginContext, constructor) {}
      return
    }
  }

  private fun generateCompiledDocumentFunction(function: IrSimpleFunction, containingClass: IrClass): IrBody {
    val irBuilder = DeclarationIrBuilder(pluginContext, function.symbol)
    return irBuilder.irBlockBody {
      val companion = containingClass.companionObject()!!
      val property = companion.properties.first { it.name == Identifiers.compiledDocumentName }
      +irReturn(apIrCall(property.getter!!.symbol, irGetObject(companion.symbol)))
    }
  }

  private fun generateVariablesFunction(function: IrSimpleFunction): IrBody {
    val irBuilder = DeclarationIrBuilder(pluginContext, function.symbol)
    return irBuilder.irBlockBody {
      // TODO fix me
      +irReturn(apIrCall(CallableIds.emptyMap))
    }
  }

  private fun generateDataAdapterFunction(function: IrSimpleFunction, containingClass: IrClass): IrBody {
    val irBuilder = DeclarationIrBuilder(pluginContext, function.symbol)
    return irBuilder.irBlockBody {
      +irReturn(
        irGetObject(
          pluginContext.referenceClass(
            containingClass.classId!!
              .createNestedClassId(Identifiers.Data)
              .createNestedClassId(Identifiers.Adapter)
          )!!
        )
      )
    }
  }

  private fun generateCompiledDocumentInitializer(definition: GQLExecutableDefinition, field: IrField): IrExpressionBody {
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
    return DeclarationIrBuilder(pluginContext, field.symbol).run {
      irExprBody(
        generateCompiledDocumentExpression(definition)
      )
    }
  }

  private val mutableMapType = pluginContext.irBuiltIns.mutableMapClass

  private val mapType = pluginContext.irBuiltIns.mapClass

  private fun generateFromJson(function: IrSimpleFunction, adapterClass: IrClass, model: ApModel): IrBody {
    val irBuilder = DeclarationIrBuilder(pluginContext, function.symbol)
    return irBuilder.irBlockBody {
      val check = pluginContext.referenceFunctions(CallableIds.checkExpectedType).single()
      val mapIrType = mapType.owner.defaultType
      val mapWithStarsType = IrSimpleTypeImpl(
        mapIrType.classifier,
        mapIrType.nullability,
        listOf(IrStarProjectionImpl, IrStarProjectionImpl),
        mapIrType.annotations,
        mapIrType.abbreviation
      )
      val elementParameter = function.parameters.first { it.name == Identifiers.element }
      +irCall(check, check.owner.returnType, listOf(mapWithStarsType)).apply {
        arguments.clear()
        arguments.add(irGet(elementParameter))
      }
      val arguments = model.properties.map {
        generateReadProperty(it, elementParameter, adapterClass)
      }.toTypedArray()
      +irReturn(
        apIrSingleConstructorCall(adapterClass.classId!!.parentClassId!!, *arguments)
      )
    }
  }

  private fun generateToJson(function: IrSimpleFunction, adapterClass: IrClass, model: ApModel): IrBody {
    val irBuilder = DeclarationIrBuilder(pluginContext, function.symbol)
    return irBuilder.irBlockBody {
      val valueParameter = function.parameters.first { it.name == Identifiers.value }

      val mutableMapOfFunction = pluginContext.referenceFunctions(CallableIds.mutableMapOf).single { it.owner.parameters.isEmpty() }

      val mapVariable = IrVariableImpl(
        startOffset = SYNTHETIC_OFFSET,
        endOffset = SYNTHETIC_OFFSET,
        origin = function.origin,
        symbol = IrVariableSymbolImpl(),
        name = "map".asIdentifier(),
        type = mutableMapType.typeWith(pluginContext.irBuiltIns.stringType, pluginContext.irBuiltIns.anyNType),
        isVar = false,
        isConst = false,
        isLateinit = false,
      ).apply {
        parent = scope.getLocalDeclarationParent()
        initializer = irCall(mutableMapOfFunction, mutableMapOfFunction.owner.returnType, listOf(pluginContext.irBuiltIns.stringType, pluginContext.irBuiltIns.anyType.makeNullable())).apply {
          arguments.clear()
        }
      }

      +mapVariable

      model.properties.map {
        +generateWriteProperty(it, mapVariable, valueParameter, adapterClass)
      }

      +irReturn(irGet(mapVariable))
    }
  }

}
