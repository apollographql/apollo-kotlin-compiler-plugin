package apollo.kotlin.compiler.plugin.test.configuration

import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer

object ApolloDirectives: SimpleDirectivesContainer() {
  val COMPAT by directive("Generate compat fir/ir")
  val POINT_SCALAR by directive("Configures the Point scalar")
}