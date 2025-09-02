import com.gradleup.librarian.gradle.Librarian

plugins {
  id("org.jetbrains.kotlin.jvm")
}

Librarian.module(project)


dependencies {
  compileOnly(libs.kotlin.compilerEmbeddable)
  compileOnly("org.jetbrains.kotlin:kotlin-stdlib")

  implementation(libs.apollo.ast)
  implementation(libs.cast)
  testImplementation(libs.kct)
}

kotlin {
  compilerOptions {
    freeCompilerArgs.add("-Xcontext-parameters")
    optIn.addAll(
      "org.jetbrains.kotlin.fir.symbols.SymbolInternals",
      "org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi",
      "org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI",
      "org.jetbrains.kotlin.backend.common.extensions.ExperimentalAPIForScriptingPlugin",
    )
  }
}

