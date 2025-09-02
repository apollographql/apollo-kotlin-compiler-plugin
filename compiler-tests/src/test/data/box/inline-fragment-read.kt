// FIR_DUMP
// DUMP_IR
import apollo.kotlin.Query

@Query("""
{
  node {
    id
    ... on Product {
      price
    }
    ... on Review {
      text 
    }
  }
}
"""
)
class GetFooQuery

fun box(): String {
  val data = GetFooQuery.Data.Adapter.fromJson(
    mapOf(
      "node" to mapOf(
        "id" to "42",
        "__onProduct" to "Product",
        "price" to 42.0
      )
    )
  )
  check(data.node.onProduct?.price == 42.0)
  check(data.node.id == "42")
  check(data.node.onReview == null)
  return "OK"
}

