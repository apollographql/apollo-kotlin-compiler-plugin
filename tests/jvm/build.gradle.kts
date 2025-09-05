plugins {
  id("org.jetbrains.kotlin.jvm")
  id("com.apollographql.kotlin.compiler.plugin")
}


dependencies {
  implementation(apolloKotlinCompilerPlugin.deps.runtime)
  implementation(apolloKotlinCompilerPlugin.deps.runtimeCompat)
  testImplementation(libs.kotlin.test)
  testImplementation("org.jetbrains.kotlinx:kotlinx-datetime:0.7.1")
  testImplementation(libs.coroutines.test)
}

apolloKotlinCompilerPlugin {
  schemaFile.set(file("schema.graphqls"))
}