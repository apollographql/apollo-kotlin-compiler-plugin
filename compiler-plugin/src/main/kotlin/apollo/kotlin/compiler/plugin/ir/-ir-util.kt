package apollo.kotlin.compiler.plugin.ir

import apollo.kotlin.compiler.plugin.CallableIds
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetObjectValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.SYNTHETIC_OFFSET
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId

fun apIrConstructor(
  pluginContext: IrPluginContext,
  declaration: IrConstructor,
  superClassConstructor: IrConstructor? = null,
  block: IrBlockBodyBuilder.() -> Unit = {}
): IrBody? {
  val delegatingConstructor = superClassConstructor ?: pluginContext.irBuiltIns.anyClass.owner.primaryConstructor ?: return null

  return apIrBlockBody(pluginContext, declaration.symbol) {
    +irDelegatingConstructorCall(delegatingConstructor)

    +IrInstanceInitializerCallImpl(
      startOffset = SYNTHETIC_OFFSET,
      endOffset = SYNTHETIC_OFFSET,
      classSymbol = (declaration.parent as? IrClass)?.symbol ?: return null,
      type = context.irBuiltIns.unitType,
    )

    block()
  }
}

inline fun apIrBlockBody(
  pluginContext: IrPluginContext,
  symbol: IrSymbol,
  block: IrBlockBodyBuilder.() -> Unit
): IrBody? {
  return DeclarationIrBuilder(pluginContext, symbol).irBlockBody(
    startOffset = SYNTHETIC_OFFSET,
    endOffset = SYNTHETIC_OFFSET,
    block
  )
}

context(visitor: ApolloIrVisitor)
fun IrBuilder.apIrList(classId: ClassId, expressions: List<IrExpression>): IrExpression {
  if (expressions.isEmpty()) {
    return apIrCall(emptyListSymbol)
  } else if (expressions.size == 1) {
    return apIrCall(listOfSingleSymbol, expressions.single())
  } else {
    val type = visitor.pluginContext.referenceClass(classId)!!.owner.defaultType
    return apIrCall(listOfManySymbol, irVararg(type, expressions))
  }
}

fun IrBuilder.apIrConst(value: String): IrConst {
  return IrConstImpl.string(SYNTHETIC_OFFSET, SYNTHETIC_OFFSET, context.irBuiltIns.stringType, value)
}

fun IrBuilder.apIrConst(value: Boolean): IrConst {
  return IrConstImpl.boolean(SYNTHETIC_OFFSET, SYNTHETIC_OFFSET, context.irBuiltIns.booleanType, value)
}

fun IrBuilder.apIrConst(value: Double): IrConst {
  return IrConstImpl.double(SYNTHETIC_OFFSET, SYNTHETIC_OFFSET, context.irBuiltIns.doubleType, value)
}

fun IrBuilder.apIrConst(value: Int): IrConst {
  return IrConstImpl.int(SYNTHETIC_OFFSET, SYNTHETIC_OFFSET, context.irBuiltIns.intType, value)
}

fun IrBuilder.apIrNull(): IrConst {
  return IrConstImpl.constNull(SYNTHETIC_OFFSET, SYNTHETIC_OFFSET, context.irBuiltIns.nothingType)
}

fun IrBuilder.apIrCall(callee: IrSimpleFunctionSymbol, vararg arguments: IrExpression): IrCall {
  return irCall(callee).apply {
    this.arguments.clear()
    this.arguments.addAll(arguments)
  }
}

context(visitor: ApolloIrVisitor)
fun IrBuilder.apIrCall(callableId: CallableId, vararg arguments: IrExpression): IrCall {
  return irCall(visitor.pluginContext.referenceFunctions(callableId).single()).apply {
    this.arguments.clear()
    this.arguments.addAll(arguments)
  }
}

context(visitor: ApolloIrVisitor)
fun apIrGetObject(classId: ClassId): IrGetObjectValueImpl {
  val classSymbol = visitor.pluginContext.referenceClass(classId)!!
  return IrGetObjectValueImpl(SYNTHETIC_OFFSET, SYNTHETIC_OFFSET, classSymbol.typeWith(), classSymbol)
}


fun IrBuilder.apIrConstructorCall(callee: IrConstructorSymbol, vararg arguments: IrExpression): IrConstructorCall {
  return irCallConstructor(callee, emptyList()).apply {
    this.arguments.clear()
    this.arguments.addAll(arguments)
  }
}

context(visitor: ApolloIrVisitor)
fun IrBuilder.apIrSingleConstructorCall(forClass: ClassId, vararg arguments: IrExpression): IrConstructorCall {
  val callee = visitor.pluginContext.referenceConstructors(forClass).single()
  return apIrConstructorCall(callee, *arguments)
}

fun IrBuilder.apIrTodo(pluginContext: IrPluginContext, reason: String = "not implemented"): IrCall {
  val function = pluginContext.referenceFunctions(CallableIds.TODO)
    .single { it.owner.parameters.size == 1 }
  val call = apIrCall(function, apIrConst(reason))

  return call
}

fun IrBuilder.apIrError(pluginContext: IrPluginContext, reason: String): IrCall {
  val function = pluginContext.referenceFunctions(CallableIds.error)
    .single { it.owner.parameters.size == 1 }
  val call = apIrCall(function, apIrConst(reason))

  return call
}
