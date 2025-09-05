package apollo.kotlin.compat

import apollo.kotlin.Executable
import com.apollographql.apollo.annotations.ApolloInternal
import com.apollographql.apollo.api.CompiledField
import com.apollographql.apollo.api.CustomScalarAdapters
import com.apollographql.apollo.api.json.JsonReader
import com.apollographql.apollo.api.json.JsonWriter
import com.apollographql.apollo.api.json.readAny
import com.apollographql.apollo.api.json.writeAny
import com.apollographql.apollo.api.Adapter as ApolloAdapter
import com.apollographql.apollo.api.Executable as ApolloExecutable

interface ExecutableCompat<D>: Executable<D>, ApolloExecutable<D> where D: ApolloExecutable.Data {
  @OptIn(ApolloInternal::class)
  override fun adapter(): ApolloAdapter<D> {
    val adapterDelegate = this@ExecutableCompat.dataAdapter()
    return object : ApolloAdapter<D> {
      override fun fromJson(
        reader: JsonReader,
        customScalarAdapters: CustomScalarAdapters
      ): D {
        return adapterDelegate.fromJson(reader.readAny())
      }

      override fun toJson(
        writer: JsonWriter,
        customScalarAdapters: CustomScalarAdapters,
        value: D
      ) {
        writer.writeAny(adapterDelegate.toJson(value))
      }
    }
  }

  override fun serializeVariables(
    writer: JsonWriter,
    customScalarAdapters: CustomScalarAdapters,
    withDefaultValues: Boolean
  ) {
    /**
     * TODO: is [withDefaultValues] so important?
     */
    this@ExecutableCompat.variables().forEach {
      writer.name(it.key)
      writer.writeAny(it.value)
    }
  }

  override fun rootField(): CompiledField {
    /**
     * TODO: This is doing the same work over and over again, should probably be fixed
     */
    return this@ExecutableCompat.compiledDocument().toCompiledField()
  }
}