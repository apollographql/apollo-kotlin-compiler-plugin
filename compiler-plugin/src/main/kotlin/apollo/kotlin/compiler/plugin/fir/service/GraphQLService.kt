/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package apollo.kotlin.compiler.plugin.fir.service

import apollo.kotlin.compiler.plugin.ClassIds
import apollo.kotlin.compiler.plugin.ap.ApModel
import apollo.kotlin.compiler.plugin.ap.allModels
import apollo.kotlin.compiler.plugin.ap.documentToAp
import apollo.kotlin.compiler.plugin.fir.classId
import cast.cast
import com.apollographql.apollo.ast.GQLExecutableDefinition
import com.apollographql.apollo.ast.Schema
import com.apollographql.apollo.ast.parseAsGQLDocument
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.caches.FirCache
import org.jetbrains.kotlin.fir.caches.createCache
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.caches.getValue
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirLiteralExpression
import org.jetbrains.kotlin.fir.expressions.arguments
import org.jetbrains.kotlin.fir.extensions.FirExtensionSessionComponent
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.coneType

class GraphQLExecutable(
  val definition: GQLExecutableDefinition,
  val models: List<ApModel>
)
class GraphQLService(session: FirSession, private val schema: Schema) : FirExtensionSessionComponent(session) {
  private val cache: FirCache<FirClassLikeSymbol<*>, GraphQLExecutable?, Nothing?> =
    session.firCachesFactory.createCache(::createExecutable)

  private fun createExecutable(symbol: FirClassLikeSymbol<*>): GraphQLExecutable? {
    val definition = symbol.definitionFromAnnotation()?.parseAsGQLDocument()?.value?.definitions?.single()?.cast<GQLExecutableDefinition>()
    if (definition == null) {
      return null
    }
    return GraphQLExecutable(
      definition = definition,
      models = documentToAp(schema, definition).allModels()
    )
  }

  fun getExecutableFor(symbol: FirClassLikeSymbol<*>) = cache.getValue(symbol)
}

private fun FirBasedSymbol<*>.definitionFromAnnotation(): String? {
  val annotation = annotations.firstOrNull {
    val classId = it.classId()
    when (classId) {
      ClassIds.Query,
      ClassIds.Mutation,
      ClassIds.Fragment,
      ClassIds.Subscription -> true

      else -> false
    }
  }

  if (annotation == null) {
    return null
  }

  annotation as FirAnnotationCall
  val argument = annotation.arguments.first()

  check(argument is FirLiteralExpression) {
    "@${annotation.annotationTypeRef.coneType.classId!!.relativeClassName} argument must be a String literal"
  }
  return argument.value as String
}

val FirSession.graphQLService: GraphQLService by FirSession.sessionComponentAccessor()
