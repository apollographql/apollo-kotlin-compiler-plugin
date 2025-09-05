package apollo.kotlin.compiler.gradle.plugin

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property

abstract class ApolloKotlinExtension {
  val deps = ApolloKotlinDeps()
  abstract val schemaFile: RegularFileProperty
  abstract val compat: Property<Boolean>

  internal val scalars = mutableListOf<Triple<String, String, String>>()
  fun mapScalar(name: String, target: String, adapter: String) {
    scalars.add(Triple(name, target, adapter))
  }
}

class ApolloKotlinDeps {
  val runtime = "com.apollographql.kotlin:runtime:${VERSION}"
  val runtimeCompat = "com.apollographql.kotlin:runtime-compat:${VERSION}"
}