plugins {
  id("base")
}

buildscript {
  dependencies {
    classpath("com.apollographql.kotlin:gradle-plugin")
    classpath(libs.kgp)
  }
}
