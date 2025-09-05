// FIR_DUMP
// DUMP_IR
import apollo.kotlin.Query

@Query("{int}")
class GetFooQuery

fun box(): String {
  check(GetFooQuery.Data(4).int == 4)
  return "OK"
}

