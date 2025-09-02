rootProject.name = "apollo-kotlin-compiler-plugin-tests"

@Suppress("UnstableApiUsage")
pluginManagement {
  listOf(repositories, dependencyResolutionManagement.repositories).forEach {
    it.mavenCentral()
  }
  repositories {
    maven("https://storage.googleapis.com/gradleup/m2/")
  }
}

includeBuild("../")
include("jvm")