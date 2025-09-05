package apollo.kotlin.compiler.plugin.fir

import apollo.kotlin.compiler.plugin.ApolloCache
import apollo.kotlin.compiler.plugin.ClassIds
import apollo.kotlin.compiler.plugin.Identifiers
import apollo.kotlin.compiler.plugin.ap.capitalizeFirstLetter
import apollo.kotlin.compiler.plugin.apolloCompatPackageName
import apollo.kotlin.compiler.plugin.asIdentifier
import apollo.kotlin.compiler.plugin.fir.service.graphQLService
import apollo.kotlin.compiler.plugin.toConeClassLikeType
import apollo.kotlin.compiler.plugin.toConeKotlinType
import cast.safeAs
import com.apollographql.apollo.ast.GQLFragmentDefinition
import com.apollographql.apollo.ast.GQLOperationDefinition
import com.apollographql.apollo.ast.Schema
import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.backend.native.interop.parentsWithSelf
import org.jetbrains.kotlin.fir.caches.getValue
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.utils.isCompanion
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.MemberGenerationContext
import org.jetbrains.kotlin.fir.extensions.NestedClassGenerationContext
import org.jetbrains.kotlin.fir.plugin.createCompanionObject
import org.jetbrains.kotlin.fir.plugin.createConstructor
import org.jetbrains.kotlin.fir.plugin.createMemberFunction
import org.jetbrains.kotlin.fir.plugin.createMemberProperty
import org.jetbrains.kotlin.fir.plugin.createNestedClass
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames

class ExecutableDeclarationGenerationExtension(
  session: FirSession,
  options: Options,
) : FirDeclarationGenerationExtension(session) {
  class Options(val schema: Schema, val compat: Boolean, val cache: ApolloCache)

  val compat = options.compat
  private val schema = options.schema
  private val apolloCache = options.cache

  override fun FirDeclarationPredicateRegistrar.registerPredicates() {
    register(FirPredicates.Query)
    register(FirPredicates.Mutation)
    register(FirPredicates.Subscription)
    register(FirPredicates.Fragment)
  }

  override fun getNestedClassifiersNames(
    classSymbol: FirClassSymbol<*>,
    context: NestedClassGenerationContext
  ): Set<Name> {
    val executable = session.graphQLService.getExecutableFor(context.owner)
    if (executable != null) {
      if (executable.definition is GQLFragmentDefinition) {
        apolloCache.rememberFragment(executable.definition.name, classSymbol.classId, executable.definition)
      }
      return buildSet {
        executable.models.forEach { add(it.name.asIdentifier()) }
        add(SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT)
      }
    }

    if (classSymbol.origin.safeAs<FirDeclarationOrigin.Plugin>()?.key is Keys.Model) {
      return setOf(Identifiers.Adapter)
    }
    return emptySet()
  }

  override fun generateNestedClassLikeDeclaration(
    owner: FirClassSymbol<*>,
    name: Name,
    context: NestedClassGenerationContext
  ): FirClassLikeSymbol<*>? {
    val executable = session.graphQLService.getExecutableFor(owner)
    return if (executable != null && name == SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT) {
      // Operation companion
      createCompanionObject(owner, Keys.OperationCompanion(executable.definition)).symbol
    } else if (executable != null) {
      // Model
      val model = executable.models.first { it.name == name.asString() }
      createNestedClass(
        owner = owner,
        name = name,
        key = Keys.Model(model),
      ) {
        if (name == Identifiers.Data) {
          if (compat) {
            if (executable.definition is GQLOperationDefinition) {
              val classId =
                ClassId(apolloCompatPackageName, executable.definition.operationType.capitalizeFirstLetter().plus("Compat").asIdentifier())
                  .createNestedClassId("Data".asIdentifier())
              superType(classId.toConeClassLikeType())
            } else {
              superType(ClassIds.FragmentCompat_Data.toConeClassLikeType())
            }
          }
        }
      }.symbol
    } else {
      val key = owner.origin.safeAs<FirDeclarationOrigin.Plugin>()?.key
      if (key is Keys.Model) {
        createNestedClass(
          owner = owner,
          name = Identifiers.Adapter,
          key = Keys.ModelAdapter(key.model),
          classKind = ClassKind.OBJECT
        ) {
          this.superType(ClassIds.Adapter.toConeClassLikeType(owner.classId.toConeClassLikeType()))
        }.symbol
      } else {
        null
      }
    }
  }

  override fun getCallableNamesForClass(classSymbol: FirClassSymbol<*>, context: MemberGenerationContext): Set<Name> {
    val executable = session.graphQLService.getExecutableFor(classSymbol)
    if (executable != null) {
      // Operation class
      return setOf(Identifiers.compiledDocumentName, Identifiers.variablesName, Identifiers.dataAdapterName)
    }

    val key = context.key
    if (key is Keys.Model) {
      val model = key.model
      // Nested model
      return buildSet {
        add(SpecialNames.INIT)
        model.properties.forEach {
          add(Name.identifier(it.name))
        }
      }
    }

    if (key is Keys.ModelAdapter) {
      // Model adapter
      return setOf(SpecialNames.INIT, Identifiers.fromJson, Identifiers.toJson)
    }

    if (classSymbol.isCompanion) {
      // Operation companion
      return setOf(SpecialNames.INIT, Identifiers.compiledDocumentName)
    }
    return emptySet()
  }

  override fun generateConstructors(context: MemberGenerationContext): List<FirConstructorSymbol> {
    return when (val key = context.key) {
      is Keys.Model -> {
        val model = key.model
        createConstructor(
          owner = context.owner,
          key = Keys.Apollo,
          isPrimary = true,
        ) {
          model.properties.forEach {
            valueParameter(
              name = Name.identifier(it.name),
              type = it.type.toConeKotlinType(context.owner.enclosingClassId(), apolloCache)
            )
          }
        }.symbol.let(::listOf)
      }

      is Keys.OperationCompanion -> {
        createConstructor(
          owner = context.owner,
          key = Keys.Apollo,
          isPrimary = true,
        ).symbol.let(::listOf)
      }

      is Keys.ModelAdapter -> {
        createConstructor(
          owner = context.owner,
          key = Keys.Apollo,
          isPrimary = true,
        ).symbol.let(::listOf)
      }

      else -> emptyList()
    }
  }

  override fun generateFunctions(
    callableId: CallableId,
    context: MemberGenerationContext?
  ): List<FirNamedFunctionSymbol> {
    val owner = context?.owner
    if (owner == null) {
      return emptyList()
    }

    val executable = session.graphQLService.getExecutableFor(context.owner)
    if (executable != null) {
      return when (callableId.callableName) {
        Identifiers.compiledDocumentName -> {
          createMemberFunction(
            owner = owner,
            key = Keys.Apollo,
            name = callableId.callableName,
            returnType = ClassIds.CompiledDocument.toConeClassLikeType(),
          ).symbol.let(::listOf)
        }

        Identifiers.variablesName -> {
          createMemberFunction(
            owner = owner,
            key = Keys.Apollo,
            name = callableId.callableName,
            returnType = ClassIds.Map.toConeClassLikeType(
              ClassIds.String.toConeClassLikeType(),
              ClassIds.Any.toConeClassLikeType(isMarkedNullable = true)
            ),
          ).symbol.let(::listOf)
        }

        Identifiers.dataAdapterName -> {
          createMemberFunction(
            owner = owner,
            key = Keys.Apollo,
            name = callableId.callableName,
            returnType = ClassIds.Adapter.toConeClassLikeType(
              typeArguments = arrayOf(owner.classId.createNestedClassId(Identifiers.Data).toConeClassLikeType())
            ),
          ).symbol.let(::listOf)
        }

        else -> emptyList()
      }
    }

    val key = context.key
    if (key is Keys.ModelAdapter) {
      return when (callableId.callableName) {
        Identifiers.toJson -> {
          createMemberFunction(
            owner = owner,
            key = Keys.Apollo,
            name = callableId.callableName,
            returnType = ClassIds.JsonElement.toConeClassLikeType(),
          ) {
            valueParameter(Identifiers.value, context.owner.classId.parentClassId!!.toConeClassLikeType())
          }.symbol.let(::listOf)
        }

        Identifiers.fromJson -> {
          createMemberFunction(
            owner = owner,
            key = Keys.Apollo,
            name = callableId.callableName,
            returnType = context.owner.classId.parentClassId!!.toConeClassLikeType(),
          ) {
            valueParameter(Identifiers.element, ClassIds.JsonElement.toConeClassLikeType())
          }.symbol.let(::listOf)
        }

        else -> emptyList()
      }
    }

    return emptyList()
  }

  override fun generateProperties(
    callableId: CallableId,
    context: MemberGenerationContext?,
  ): List<FirPropertySymbol> {
    when (val key = context?.key) {
      is Keys.Model -> {
        val model = key.model
        // look up the property
        val it = model.properties.single { it.name == callableId.callableName.identifier }
        return createMemberProperty(
          owner = context.owner,
          key = Keys.Apollo,
          name = Name.identifier(it.name),
          returnType = it.type.toConeKotlinType(context.owner.enclosingClassId(), apolloCache),
          isVal = true,
          hasBackingField = true,
        ).symbol.let(::listOf)
      }

      is Keys.OperationCompanion -> {
        return createMemberProperty(
          owner = context.owner,
          key = Keys.Apollo,
          name = Identifiers.compiledDocumentName,
          returnType = ClassIds.CompiledDocument.toConeClassLikeType(),
          isVal = true,
          hasBackingField = true,
        ).symbol.let(::listOf)
      }

      else -> {
        return emptyList()
      }
    }
  }

  private val MemberGenerationContext.key: GeneratedDeclarationKey?
    get() = (owner.origin as? FirDeclarationOrigin.Plugin)?.key

  private fun FirClassSymbol<*>.enclosingClassId(): ClassId {
    // TODO: fix the case where the field has the same name as the Query
    return parentsWithSelf(session).single { it.name != name }.classId
  }
}

