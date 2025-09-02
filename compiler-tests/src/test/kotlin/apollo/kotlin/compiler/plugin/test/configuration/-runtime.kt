package apollo.kotlin.compiler.plugin.test.configuration

import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoot
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.RuntimeClasspathProvider
import org.jetbrains.kotlin.test.services.TestServices
import java.io.File

internal val apolloKotlinRuntimClasspath =
  System.getProperty("apolloKotlinRuntim.classpath")?.split(File.pathSeparator)?.map(::File)
    ?: error("Unable to get a valid classpath from 'apolloKotlinRuntim.classpath' property")

class RuntimeClasspathConfigurator(testServices: TestServices) :
  EnvironmentConfigurator(testServices) {
  override fun configureCompilerConfiguration(
    configuration: CompilerConfiguration,
    module: TestModule,
  ) {
    for (file in apolloKotlinRuntimClasspath) {
      configuration.addJvmClasspathRoot(file)
    }
  }
}

class RuntimeClassPathProvider(testServices: TestServices) : RuntimeClasspathProvider(testServices) {
  override fun runtimeClassPaths(module: TestModule): List<File> {
    return apolloKotlinRuntimClasspath
  }
}
