package apollo.kotlin.compiler.plugin.fir

import apollo.kotlin.compiler.plugin.ApolloCache
import apollo.kotlin.compiler.plugin.fir.service.GraphQLService
import com.apollographql.apollo.ast.Schema
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar

class ApolloFirExtensionRegistrar(private val schema: Schema, private val compat: Boolean, private val cache: ApolloCache) : FirExtensionRegistrar() {
  override fun ExtensionRegistrarContext.configurePlugin() {

    +::ExecutableDeclarationGenerationExtension.bind(ExecutableDeclarationGenerationExtension.Options(schema, compat, cache))
    +::SchemaDeclarationGenerationExtension.bind(SchemaDeclarationGenerationExtension.Options(schema, cache))
    +::OperationSupertypeGenerationExtension.bind(compat)
    +::GraphQLService.bind(schema)
  }
}
