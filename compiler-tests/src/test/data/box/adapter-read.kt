// FIR_DUMP
// DUMP_IR
import apollo.kotlin.Query

@Query("""
{
  object {
    nested
  }
}
"""
)
class GetFooQuery

fun box(): String {
  val data = GetFooQuery.Data.Adapter.fromJson(mapOf("object" to mapOf("nested" to 4)))
  check(data.`object`?.nested == 4)
  return "OK"
}

