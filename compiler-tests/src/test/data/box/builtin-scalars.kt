// FIR_DUMP
// DUMP_IR
import apollo.kotlin.Query

@Query(
  """
{
  int
  boolean
  string
  float
}
"""
)
class GetFooQuery

fun box(): String {
  GetFooQuery.Data(4, true, "foo", 2.0)
  GetFooQuery.Data(null, null, null, null)

  return "OK"
}

