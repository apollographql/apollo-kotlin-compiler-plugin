package apollo.kotlin.compiler.plugin.ap

fun String.capitalizeFirstLetter(): String {
  return replaceFirstChar { it.uppercase() }
}