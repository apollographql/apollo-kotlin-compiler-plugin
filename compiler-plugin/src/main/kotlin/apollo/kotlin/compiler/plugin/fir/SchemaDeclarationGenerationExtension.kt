package apollo.kotlin.compiler.plugin.fir

import apollo.kotlin.compiler.plugin.ApolloCache
import apollo.kotlin.compiler.plugin.ClassIds
import com.apollographql.apollo.ast.Schema
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirLiteralExpression
import org.jetbrains.kotlin.fir.expressions.arguments
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.MemberGenerationContext
import org.jetbrains.kotlin.fir.extensions.NestedClassGenerationContext
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name

class SchemaDeclarationGenerationExtension(
  session: FirSession,
  options: Options,
) : FirDeclarationGenerationExtension(session) {
  class Options(val schema: Schema, val cache: ApolloCache)

  override fun FirDeclarationPredicateRegistrar.registerPredicates() {
    register(FirPredicates.Schema)
    register(FirPredicates.Scalar)
  }

  override fun getNestedClassifiersNames(
    classSymbol: FirClassSymbol<*>,
    context: NestedClassGenerationContext
  ): Set<Name> {
    if (classSymbol.annotations.any {
        it.classId() == ClassIds.Schema
      }) {
    }
    return emptySet()
  }
}

