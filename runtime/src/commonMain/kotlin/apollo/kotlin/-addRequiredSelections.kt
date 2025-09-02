package apollo.kotlin

fun CompiledDocument.addRequiredSelections(): CompiledDocument {
  return CompiledDocument(
    operations = operations.map {
      CompiledOperation(
        name = it.name,
        operationType = it.operationType,
        selections = it.selections.map { it.addRequiredSelections() },
        rawType = it.rawType
      )
    },
    fragments = fragments.map {
      CompiledFragmentDefinition(
        name = it.name,
        typeCondition = it.typeCondition,
        selections = listOf(syntheticField("__${it.name}")) + it.selections.map { it.addRequiredSelections() },
      )
    }
  )
}

fun CompiledSelection.addRequiredSelections(): CompiledSelection {
  return when (this) {
    is CompiledField -> CompiledField(
      alias = alias,
      name = name,
      arguments = arguments,
      selections = selections.map { it.addRequiredSelections() },
      type = type
    )
    is CompiledFragmentSpread -> {
      this
    }
    is CompiledInlineFragment -> {
      CompiledInlineFragment(
        typeCondition = typeCondition,
        selections = listOf(syntheticField("__on$typeCondition")) + selections.map { it.addRequiredSelections() }
      )
    }
  }
}

private fun syntheticField(name: String) : CompiledField = CompiledField(
  alias = name,
  name = "__typename",
  arguments = emptyList(),
  selections = emptyList(),
  type = "String!"
)