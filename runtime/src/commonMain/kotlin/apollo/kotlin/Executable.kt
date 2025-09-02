package apollo.kotlin

interface Executable<D> {
  fun variables(): Map<String, JsonElement>

  fun compiledDocument(): CompiledDocument

  fun dataAdapter(): Adapter<D>
}
