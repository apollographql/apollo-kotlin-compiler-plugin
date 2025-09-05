// FIR_DUMP
// DUMP_IR
// COMPAT

// FILE: a.kt
import apollo.kotlin.Query

@Query("{...queryDetails}")
class GetFooQuery

// FILE: b.kt
import apollo.kotlin.Fragment

@Fragment("fragment queryDetails on Query { int }")
class QueryDetails

fun box(): String {
  check(GetFooQuery.Data(QueryDetails.Data(4)).queryDetails!!.int == 4)
  check(GetFooQuery().document() == "{...queryDetails}\n" +
    "fragment queryDetails on Query{__queryDetails:__typename,int}")
  return "OK"
}

