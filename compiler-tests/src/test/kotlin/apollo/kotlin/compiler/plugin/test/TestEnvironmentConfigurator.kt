package apollo.kotlin.compiler.plugin.test

import apollo.kotlin.compiler.plugin.test.configuration.ApolloDirectives
import apollo.kotlin.compiler.plugin.ApolloCache
import apollo.kotlin.compiler.plugin.ScalarInfo
import apollo.kotlin.compiler.plugin.fir.ApolloFirExtensionRegistrar
import apollo.kotlin.compiler.plugin.ir.ApolloIrGenerationExtension
import com.apollographql.apollo.ast.parseAsGQLDocument
import com.apollographql.apollo.ast.toSchema
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.TestServices
import java.io.File

class TestEnvironmentConfigurator(testServices: TestServices) : EnvironmentConfigurator(testServices) {
  @OptIn(ExperimentalCompilerApi::class)
  override fun CompilerPluginRegistrar.ExtensionStorage.registerCompilerExtensions(
    module: TestModule,
    configuration: CompilerConfiguration,
  ) {
    val schema = File("compiler-tests/src/test/data/schema.graphqls").parseAsGQLDocument().getOrThrow().toSchema()

    val scalars = if (ApolloDirectives.POINT_SCALAR in module.directives) {
      mapOf("Point" to ScalarInfo("lib.Point", "lib.PointAdapter"))
    } else {
      emptyMap()
    }
    val apolloCache = ApolloCache(scalars)
    FirExtensionRegistrarAdapter.registerExtension(ApolloFirExtensionRegistrar(
      schema,
      ApolloDirectives.COMPAT in module.directives,
      apolloCache
    ))
    IrGenerationExtension.registerExtension(ApolloIrGenerationExtension(schema, apolloCache))
  }
}