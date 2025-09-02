package apollo.kotlin.compiler.plugin.fir

import apollo.kotlin.compiler.plugin.ClassIds
import apollo.kotlin.compiler.plugin.ap.capitalizeFirstLetter
import apollo.kotlin.compiler.plugin.apolloCompatPackageName
import apollo.kotlin.compiler.plugin.asIdentifier
import apollo.kotlin.compiler.plugin.fir.service.graphQLService
import apollo.kotlin.compiler.plugin.toConeClassLikeType
import com.apollographql.apollo.ast.GQLOperationDefinition
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.FirSupertypeGenerationExtension
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.name.ClassId

class OperationSupertypeGenerationExtension(session: FirSession, private val compat: Boolean) :
  FirSupertypeGenerationExtension(session) {
  override fun FirDeclarationPredicateRegistrar.registerPredicates() {
    register(FirPredicates.Query)
    register(FirPredicates.Mutation)
    register(FirPredicates.Subscription)
    register(FirPredicates.Fragment)
  }

  override fun needTransformSupertypes(declaration: FirClassLikeDeclaration): Boolean {
    if (declaration !is FirRegularClass) {
      return false
    }

    if (declaration.classKind != ClassKind.CLASS) {
      return false
    }

    if (session.predicateBasedProvider.matches(FirPredicates.Query, declaration)) {
      return true
    }
    if (session.predicateBasedProvider.matches(FirPredicates.Mutation, declaration)) {
      return true
    }
    if (session.predicateBasedProvider.matches(FirPredicates.Subscription, declaration)) {
      return true
    }
    if (session.predicateBasedProvider.matches(FirPredicates.Fragment, declaration)) {
      return true
    }
    return false
  }

  override fun computeAdditionalSupertypes(
    classLikeDeclaration: FirClassLikeDeclaration,
    resolvedSupertypes: List<FirResolvedTypeRef>,
    typeResolver: TypeResolveService
  ): List<ConeKotlinType> {
    val symbol = classLikeDeclaration.symbol
    val typeArgument = symbol.classId.createNestedClassId("Data".asIdentifier()).toConeClassLikeType()
    if (!compat) {
      return listOf(ClassIds.Executable.toConeClassLikeType(typeArgument))
    }

    val executable = session.graphQLService.getExecutableFor(symbol)
    if (executable == null) {
      return emptyList()
    }

    if (executable.definition is GQLOperationDefinition) {
      val compatOperationClassId =
        ClassId(apolloCompatPackageName, executable.definition.operationType.capitalizeFirstLetter().plus("Compat").asIdentifier())
      return listOf(compatOperationClassId.toConeClassLikeType(typeArgument))
    } else {
      return listOf(ClassIds.FragmentCompat.toConeClassLikeType(typeArgument))
    }
  }
}