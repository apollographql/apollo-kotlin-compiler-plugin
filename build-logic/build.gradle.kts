plugins {
  alias(libs.plugins.kgp.jvm)
}

group = "com.apollographql.kotlin.build"

dependencies {
  implementation(libs.kgp)
  implementation(libs.ksp)
  implementation(libs.librarian)
  implementation(libs.compat.patrouille)
  implementation(libs.gratatouille)
}