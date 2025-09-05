package apollo.kotlin.compiler.plugin.ir

import apollo.kotlin.compiler.plugin.ClassIds
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.util.getAnnotation

internal fun IrClass.isExecutable(): Boolean {
  if (getAnnotation(ClassIds.Query.asSingleFqName()) != null) {
    return true
  }
  if (getAnnotation(ClassIds.Mutation.asSingleFqName()) != null) {
    return true
  }
  if (getAnnotation(ClassIds.Subscription.asSingleFqName()) != null) {
    return true
  }
  if (getAnnotation(ClassIds.Fragment.asSingleFqName()) != null) {
    return true
  }
  return false
}
