// FIR_DUMP
// DUMP_IR
// COMPAT
import apollo.kotlin.Adapter
import apollo.kotlin.CompiledDocument
import apollo.kotlin.JsonElement
import apollo.kotlin.compat.QueryCompat

abstract class BaseFoo {
  fun document(): String {
    return field() + "foo"
  }

  abstract fun field(): String
}

class Foo: BaseFoo() {
  override fun field(): String {
    return "Foo"
  }
}
fun box(): String {
  return "OK"
}

