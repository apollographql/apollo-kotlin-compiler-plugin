// FIR_DUMP
// DUMP_IR
import apollo.kotlin.Adapter
import apollo.kotlin.Query

@Query(
  """
{
  foo: object {
    nested
  }
}
"""
)
class GetFooQuery

fun box(): String {
  val foo = GetFooQuery.Foo(4)
  GetFooQuery.Data(foo)

  return "OK"
}

