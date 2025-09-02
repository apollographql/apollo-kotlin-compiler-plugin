package apollo.kotlin.compiler.plugin.fir

import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirGetClassCall
import org.jetbrains.kotlin.fir.expressions.UnresolvedExpressionTypeAccess
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.classLikeLookupTagIfAny
import org.jetbrains.kotlin.fir.types.coneTypeSafe
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.fir.types.type
import org.jetbrains.kotlin.name.ClassId

fun FirAnnotation.classId(): ClassId? {
  return annotationTypeRef.coneTypeSafe<ConeKotlinType>()?.classLikeLookupTagIfAny?.classId
}

@OptIn(UnresolvedExpressionTypeAccess::class)
internal fun FirExpression.getKClass(): ClassId {
  this as FirGetClassCall

  val resolvedType = resolvedType

  return resolvedType.typeArguments.single().type!!.classId!!
}
