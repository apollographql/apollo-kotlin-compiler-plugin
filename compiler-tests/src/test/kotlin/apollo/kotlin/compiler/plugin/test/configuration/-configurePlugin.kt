package apollo.kotlin.compiler.plugin.test.configuration

import apollo.kotlin.compiler.plugin.test.TestEnvironmentConfigurator
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder

fun TestConfigurationBuilder.configurePlugin() {
  useConfigurators(
    ::TestEnvironmentConfigurator,
    ::RuntimeClasspathConfigurator,
    ::RuntimeCompatClasspathConfigurator,
  )
  useDirectives(ApolloDirectives)

  useCustomRuntimeClasspathProviders(
    ::RuntimeClassPathProvider,
    ::RuntimeCompatClassPathProvider
  )
}

