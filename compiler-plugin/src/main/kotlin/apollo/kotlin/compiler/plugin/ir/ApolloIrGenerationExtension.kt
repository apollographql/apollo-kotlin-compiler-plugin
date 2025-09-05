package apollo.kotlin.compiler.plugin.ir

import apollo.kotlin.compiler.plugin.ApolloCache
import com.apollographql.apollo.ast.Schema
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid

class ApolloIrGenerationExtension(private val schema: Schema, private val apolloCache: ApolloCache) : IrGenerationExtension {
  override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
    moduleFragment.acceptChildrenVoid(ApolloIrVisitor(schema, apolloCache, pluginContext, ))
  }
}
