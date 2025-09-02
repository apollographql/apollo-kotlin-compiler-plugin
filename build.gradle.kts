import com.gradleup.librarian.gradle.Librarian

plugins {
  id("base")
}

buildscript {
  dependencies {
    classpath("com.apollographql.kotlin.build:build-logic")
    classpath(libs.kctf.gradle.plugin)
  }
}

Librarian.root(project)