package apollo.kotlin.compat

import apollo.kotlin.addRequiredSelections
import apollo.kotlin.writeTo
import okio.Buffer
import com.apollographql.apollo.api.Operation as ApolloOperation

interface OperationCompat<D>: ExecutableCompat<D>, ApolloOperation<D> where D: ApolloOperation.Data {
  override fun document(): String {
    // TODO: cache this
    return Buffer().apply { this@OperationCompat.compiledDocument().addRequiredSelections().writeTo(this) }.readUtf8()
  }

  override fun name(): String {
    return this@OperationCompat.compiledDocument().operations.single().name
  }

  override fun id(): String {
    return "FIXME"
  }
}