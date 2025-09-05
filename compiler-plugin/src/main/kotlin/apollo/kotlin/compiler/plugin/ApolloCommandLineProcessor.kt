package apollo.kotlin.compiler.plugin

import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CliOptionProcessingException
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey

@Suppress("Unused") // Used from ServiceLocator
class ApolloCommandLineProcessor : CommandLineProcessor {
  override val pluginId: String
    get() = "com.apollographql.kotlin"
  override val pluginOptions: Collection<AbstractCliOption>
    get() {
      return ApolloOption.entries.map { it.raw.cli }
    }

  override fun processOption(option: AbstractCliOption, value: String, configuration: CompilerConfiguration) {
    super.processOption(option, value, configuration)
    when (val apolloOption = optionsByName.get(option.optionName)) {
      null -> throw CliOptionProcessingException("Unknown plugin option: ${option.optionName}")
      else -> configuration.put(apolloOption.raw.key, value)
    }
  }
}

internal enum class ApolloOption(
  val raw: RawApolloOption
) {
  SCHEMA(
    RawApolloOption(
      optionName = "schema",
      valueDescription = "The schema to use",
      description = "The schema to use",
      required = false,
      allowMultipleOccurrences = false
    )
  ),
  COMPAT(
    RawApolloOption(
      optionName = "compat",
      valueDescription = "Whether to use the compat layer with the Apollo Runtime",
      description = "Whether to use the compat layer with the Apollo Runtime",
      required = true,
      allowMultipleOccurrences = false
    )
  ),
  SCALARS(
    RawApolloOption(
      optionName = "scalars",
      valueDescription = "The scalars configuration",
      description = "The scalars configuration",
      required = true,
      allowMultipleOccurrences = false
    )
  )
}

internal class RawApolloOption(
  optionName: String,
  valueDescription: String,
  description: String,
  required: Boolean,
  allowMultipleOccurrences: Boolean
) {
  val cli = CliOption(
    optionName = optionName,
    valueDescription = valueDescription,
    description = description,
    required = required,
    allowMultipleOccurrences = allowMultipleOccurrences
  )
  val key: CompilerConfigurationKey<String> = CompilerConfigurationKey(optionName)
}

internal val optionsByName = ApolloOption.entries.associateBy { it.raw.cli.optionName }
