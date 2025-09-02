package apollo.kotlin.compiler.plugin.fir

import apollo.kotlin.compiler.plugin.ap.ApModel
import com.apollographql.apollo.ast.GQLDefinition
import com.apollographql.apollo.ast.GQLDocument
import com.apollographql.apollo.ast.GQLExecutableDefinition
import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol

object Keys {
  /**
   * Generic key when we don't need to convey more information
   */
  data object Apollo: GeneratedDeclarationKey()

  class OperationCompanion(val definition: GQLExecutableDefinition): GeneratedDeclarationKey()
  class Model(val model: ApModel, ) : GeneratedDeclarationKey()
  class ModelAdapter(val model: ApModel): GeneratedDeclarationKey()
}
