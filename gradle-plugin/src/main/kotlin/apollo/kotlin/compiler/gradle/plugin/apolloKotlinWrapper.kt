package apollo.kotlin.compiler.gradle.plugin

import gratatouille.wiring.GPlugin
import org.gradle.api.Project

/**
 * A wrapper that is used to display a nice error message if Kotlin is not loaded
 * in the classpath.
 */
@GPlugin(id = "com.apollographql.kotlin.compiler.plugin")
fun apolloKotlinWrapper(project: Project) {
  val supportedPlugins = listOf(
    "org.jetbrains.kotlin.jvm", "org.jetbrains.kotlin.multiplatform", "org.jetbrains.kotlin.android",
    "com.android.application", "com.android.application" // those now configure Kotlin
  )
  var hasKgp = false
  supportedPlugins.forEach {
    project.pluginManager.withPlugin(it) {
      if (!hasKgp) {
        hasKgp = true
        project.pluginManager.apply(ApolloKotlinGradlePlugin::class.java)
      }
    }
  }

  project.afterEvaluate {
    if (!hasKgp) {
      error("The `com.apollographql.kotlin.compiler.plugin` Gradle plugin requires the Kotlin Gradle Plugin")
    }
  }
}