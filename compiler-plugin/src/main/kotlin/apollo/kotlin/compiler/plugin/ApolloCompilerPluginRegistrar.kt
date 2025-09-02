package apollo.kotlin.compiler.plugin

import apollo.kotlin.compiler.plugin.fir.ApolloFirExtensionRegistrar
import com.apollographql.apollo.ast.toGQLDocument
import com.apollographql.apollo.ast.toSchema
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import apollo.kotlin.compiler.plugin.ir.ApolloIrGenerationExtension
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter

@Suppress("Unused") // Used from ServiceLocator
class ApolloCompilerPluginRegistrar : CompilerPluginRegistrar() {
  override val supportsK2: Boolean
    get() = true

  override fun ExtensionStorage.registerExtensions(
    configuration: CompilerConfiguration
  ) {
    val schema = configuration.get(ApolloOption.SCHEMA.raw.key)!!.unescape().toGQLDocument().toSchema()
    val compat = configuration.get(ApolloOption.COMPAT.raw.key)!!.toBooleanStrict()
    val scalars = configuration.get(ApolloOption.SCALARS.raw.key)!!.unescape().toScalars()
    val cache = ApolloCache(scalars)
    FirExtensionRegistrarAdapter.registerExtension(ApolloFirExtensionRegistrar(schema, compat, cache))
    IrGenerationExtension.registerExtension(ApolloIrGenerationExtension(schema, cache))
  }
}

private fun String.toScalars(): Map<String, ScalarInfo> {
  return split(",").associate {
    it.split(":").let {
      it.get(0) to ScalarInfo(it.get(1), it.get(2))
    }
  }
}


/**
 * Make sure the string is correctly parsed by the Kotlin parser. Especially, ',' is used to separate plugin arguments:
 * https://github.com/Jetbrains/kotlin/blob/2022ff715bb3daa0c080f6298beae56d1f7276a5/compiler/cli/cli-common/src/org/jetbrains/kotlin/cli/common/arguments/parseCommandLineArguments.kt#L295
 */
private fun String.unescape(): String {
  val length = length
  return buildString {
    var i = 0
    while (i < length) {
      when (val c = this@unescape.get(i)) {
        '\\' -> {
          check(i + 5 < length) {
            "Unterminated escape sequence"
          }
          check(this@unescape.get(i + 1) == 'u') {
            "Invalid escape sequence"
          }
          val code = this@unescape.substring(i + 2, i + 6).toIntOrNull()
          check(code != null) {
            "Invalid escape sequence"
          }
          appendCodePoint(code)
          i += 6
        }

        else -> {
          append(c)
          i++
        }
      }
    }
  }
}