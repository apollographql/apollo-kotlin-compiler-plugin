// FIR_DUMP
// DUMP_IR
import apollo.kotlin.Query

@Query(
  """
{
  listOfInt
  nonNullInt
}
"""
)
class GetFooQuery

fun box(): String {
  GetFooQuery.Data(listOf(4), 4)
  GetFooQuery.Data(listOf(null), 4)

  return "OK"
}

