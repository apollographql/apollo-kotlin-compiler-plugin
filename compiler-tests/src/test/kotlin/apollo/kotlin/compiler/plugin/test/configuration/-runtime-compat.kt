package apollo.kotlin.compiler.plugin.test.configuration

import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoot
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.RuntimeClasspathProvider
import org.jetbrains.kotlin.test.services.TestServices
import java.io.File

internal val apolloKotlinRuntimCompatClasspath =
  System.getProperty("apolloKotlinRuntimCompat.classpath")?.split(File.pathSeparator)?.map(::File)
    ?: error("Unable to get a valid classpath from 'apolloKotlinRuntimCompat.classpath' property")

class RuntimeCompatClasspathConfigurator(testServices: TestServices) :
  EnvironmentConfigurator(testServices) {
  override fun configureCompilerConfiguration(
    configuration: CompilerConfiguration,
    module: TestModule,
  ) {
    if (ApolloDirectives.COMPAT in module.directives) {
      for (file in apolloKotlinRuntimCompatClasspath) {
        configuration.addJvmClasspathRoot(file)
      }
    }
  }
}

class RuntimeCompatClassPathProvider(testServices: TestServices) : RuntimeClasspathProvider(testServices) {
  override fun runtimeClassPaths(module: TestModule): List<File> {
    return if (ApolloDirectives.COMPAT in module.directives) {
      apolloKotlinRuntimCompatClasspath
    } else {
      emptyList()
    }
  }
}
