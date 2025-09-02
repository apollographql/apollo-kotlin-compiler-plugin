package apollo.kotlin.compiler.gradle.plugin

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption
import kotlin.text.iterator

abstract class ApolloKotlinGradlePlugin : KotlinCompilerPluginSupportPlugin {
  override fun apply(target: Project) {
    super.apply(target)
    target.extensions.create("apolloKotlinCompilerPlugin", ApolloKotlinExtension::class.java)
  }

  override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> {
    val project = kotlinCompilation.target.project
    val extension = project.extensions.getByType(ApolloKotlinExtension::class.java)
    return project.provider {
      listOf(
        SubpluginOption(
          "schema",
          lazy(LazyThreadSafetyMode.NONE) {
            extension.schemaFile.get().asFile.readText().escape()
          }),
        SubpluginOption(
          "compat",
          lazy(LazyThreadSafetyMode.NONE) {
            extension.compat.orElse(false).get().toString()
          }),
        SubpluginOption(
          "scalars",
          lazy(LazyThreadSafetyMode.NONE) {
            extension.scalars.joinToString(",") { "${it.first}:${it.second}:${it.third}" }.escape()
          })
      )
    }
  }

  override fun getCompilerPluginId(): String {
    return "com.apollographql.kotlin"
  }

  override fun getPluginArtifact(): SubpluginArtifact {
    return SubpluginArtifact("com.apollographql.kotlin", "compiler-plugin", VERSION)
  }

  override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean {
    return true
  }
}

/**
 * Make sure the string is correctly parsed by the Kotlin parser. Especially, ',' is used to separate plugin arguments:
 * https://github.com/Jetbrains/kotlin/blob/2022ff715bb3daa0c080f6298beae56d1f7276a5/compiler/cli/cli-common/src/org/jetbrains/kotlin/cli/common/arguments/parseCommandLineArguments.kt#L295
 */
private fun String.escape(): String {
  return buildString {
    for (c in this@escape) {
      when {
        c in "\\/,\n =" -> {
          append(String.format("\\u%04d", c.code))
        }
        else -> append(c)
      }
    }
  }
}