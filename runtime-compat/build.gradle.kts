import com.gradleup.librarian.gradle.Librarian

plugins {
  id("org.jetbrains.kotlin.multiplatform")
}

Librarian.module(project)

kotlin {
  jvm()
  sourceSets {
    getByName("commonMain").dependencies {
      api(libs.okio)
      api(libs.apollo.api)
      api(project(":runtime"))

      implementation(libs.apollo.ast)
    }
  }
}