// FIR_DUMP
// DUMP_IR
import apollo.kotlin.Query

@Query(
  """
{
  object {
    nested
  }
}
"""
)
class GetFooQuery

fun box(): String {
  val map = GetFooQuery.Data.Adapter.toJson(
    GetFooQuery.Data(
      GetFooQuery.Object(4)
    )
  )
  check(((map as Map<String, Any?>).get("object") as Map<String, Any?>).get("nested") == 4)
  return "OK"
}

