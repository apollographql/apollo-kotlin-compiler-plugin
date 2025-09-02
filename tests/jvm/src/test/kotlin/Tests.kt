import apollo.kotlin.Fragment
import apollo.kotlin.Query
import apollo.kotlin.writeTo
import kotlinx.coroutines.test.runTest
import okio.Buffer
import kotlin.test.Test

@Query("{...queryDetails}")
class GetFooQuery

@Fragment("fragment queryDetails on Query { int }")
class QueryDetails

fun box(): String {
  check(GetFooQuery.Data(QueryDetails.Data(4)).queryDetails!!.int == 4)

  println(Buffer().apply { GetFooQuery.compiledDocument.writeTo(this) }.readUtf8())
  return "OK"
}

class Tests {
  @Test
  fun test() = runTest {
    box()
  }
}

