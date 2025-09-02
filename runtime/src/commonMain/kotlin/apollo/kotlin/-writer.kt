package apollo.kotlin

import okio.BufferedSink

private fun List<CompiledSelection>.writeTo(sink: BufferedSink) {
  join(sink, prefix = "{", suffix = "}") {
    when (it) {
      is CompiledField -> it.writeTo(sink)
      is CompiledFragmentSpread -> it.writeTo(sink)
      is CompiledInlineFragment -> it.writeTo(sink)
    }
  }
}

internal fun CompiledField.writeTo(sink: BufferedSink) {
  if (alias != null) {
    sink.writeUtf8("$alias:")
  }
  sink.writeUtf8(name)
  if (arguments.isNotEmpty()) {
    sink.writeByte('('.code)
    arguments.join(sink) {
      sink.writeUtf8("${it.name}:")
      it.value.writeTo(sink)
    }
    sink.writeByte(')'.code)
  }
  if (selections.isNotEmpty()) {
    selections.writeTo(sink)
  }
}

private fun <T> List<T>.join(sink: BufferedSink, prefix: String = "", suffix: String = "", block: (T) -> Unit) {
  sink.writeUtf8(prefix)
  var first = true
  forEach {
    if (first) {
      first = false
    } else {
      sink.writeByte(','.code)
    }
    block(it)
  }
  sink.writeUtf8(suffix)
}

fun CompiledValue.writeTo(sink: BufferedSink) {
  when (this) {
    is CompiledBooleanValue -> sink.writeUtf8(value.toString())
    is CompiledEnumValue -> sink.writeUtf8(value)
    is CompiledStringValue -> sink.writeUtf8(value)
    is CompiledFloatValue -> sink.writeUtf8(value.toString())
    is CompiledIntValue -> sink.writeUtf8(value.toString())
    is CompiledListValue -> {
      items.join(sink) {
        it.writeTo(sink)
      }
    }
    CompiledNullValue -> sink.writeUtf8("null")
    is CompiledObjectValue -> {
      fields.join(sink, prefix = "{", suffix = "}") {
        sink.writeUtf8("${it.name}:${it.value}")
      }
    }
    is CompiledVariableValue -> sink.writeUtf8("$$name")
  }
}
internal fun CompiledInlineFragment.writeTo(sink: BufferedSink) {
  sink.writeUtf8("... on $typeCondition")
  selections.writeTo(sink)
}

internal fun CompiledFragmentSpread.writeTo(sink: BufferedSink) {
  sink.writeUtf8("...$name")
}

internal fun CompiledFragmentDefinition.writeTo(sink: BufferedSink) {
  sink.writeUtf8("fragment $name on $typeCondition")
  selections.writeTo(sink)
}

fun CompiledDocument.writeTo(sink: BufferedSink) {
  var first = true
  operations.forEach {
    if (first) {
      first = false
    } else {
      sink.writeByte('\n'.code)
    }
    it.writeTo(sink)
  }
  fragments.forEach {
    if (first) {
      first = false
    } else {
      sink.writeByte('\n'.code)
    }
    it.writeTo(sink)
  }
}

internal fun CompiledOperation.writeTo(sink: BufferedSink) {
  if (name.isNotEmpty() || operationType != CompiledOperationType.Query) {
    sink.writeUtf8(operationType.name.lowercase())
  }
  if (name.isNotEmpty()) {
    sink.writeByte(' '.code)
  }
  sink.writeUtf8(name)
  selections.writeTo(sink)
}
