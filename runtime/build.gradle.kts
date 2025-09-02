import com.gradleup.librarian.gradle.Librarian

plugins {
  id("org.jetbrains.kotlin.multiplatform")
}

Librarian.module(project)

kotlin {
  jvm()
  sourceSets {
    getByName("commonMain").dependencies {
      api(libs.coroutines)
      api(libs.okio)
    }
  }
}