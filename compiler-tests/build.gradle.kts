plugins {
  id("org.jetbrains.kotlin.jvm")
  id("com.gradleup.kctf")
}

val apolloRuntimeClasspath: Configuration by configurations.creating { isTransitive = false }
val apolloRuntimeCompatClasspath: Configuration by configurations.creating { isTransitive = false }

dependencies {
  testImplementation(project(":compiler-plugin"))

  testImplementation(libs.apollo.ast)
  testImplementation(libs.kotlin.test)
  testImplementation(libs.kctf.runtime)

  apolloRuntimeClasspath(project(":runtime"))
  apolloRuntimeCompatClasspath(project(":runtime-compat"))
  apolloRuntimeCompatClasspath(libs.okio) // TODO: can we make this transitive instead of listing all api dependencies manually?
  apolloRuntimeCompatClasspath(libs.apollo.api) // TODO: can we make this transitive instead of listing all api dependencies manually?
}

tasks.withType(Test::class.java) {
  dependsOn(apolloRuntimeClasspath)
  dependsOn(apolloRuntimeCompatClasspath)

  systemProperty("apolloKotlinRuntim.classpath", apolloRuntimeClasspath.asPath)
  systemProperty("apolloKotlinRuntimCompat.classpath", apolloRuntimeCompatClasspath.asPath)
}

tasks.register("deleteTxt") {
  val directory = file("src/test/data/")
  doLast {
    directory.walk().filter { it.extension == "txt" }.forEach {
      it.delete()
    }
  }
}