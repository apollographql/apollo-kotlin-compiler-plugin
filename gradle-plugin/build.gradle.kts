import com.gradleup.librarian.gradle.Librarian

plugins {
  id("org.jetbrains.kotlin.jvm")
  id("com.google.devtools.ksp")
  id("com.gradleup.compat.patrouille")
  id("com.gradleup.gratatouille")
}

Librarian.module(project)

dependencies {
  compileOnly(libs.kgp)
  compileOnly(libs.gradle.api)
  implementation(libs.gratatouille.tasks.runtime)
  implementation(libs.gratatouille.wiring.runtime)
}

gratatouille {
  codeGeneration {
    addDependencies.set(false)
  }

  pluginMarker("com.apollographql.kotlin.compiler.plugin")
}

