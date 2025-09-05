// FIR_DUMP
// DUMP_IR

import apollo.kotlin.Query
import apollo.kotlin.Fragment

@Query("""
{
  node {
    id
    ...productDetails
    ...reviewDetails
  }
}
""")
class GetFooQuery


@Fragment("""
fragment reviewDetails on Review {
  text 
}
""")
class ReviewDetails

@Fragment("""
fragment productDetails on Product {
  price
}
""")
class ProductDetails

fun box(): String {
  val map = GetFooQuery.Data.Adapter.toJson(
    GetFooQuery.Data(
      GetFooQuery.Node(
        id = "42",
        reviewDetails = ReviewDetails.Data("Great"),
        productDetails = null,
      )
    )
  )
  map as Map<String, Any?>
  val node = map.get("node")
  node as Map<String, Any?>
  check(node.get("id") == "42")
  check(node.get("text") == "Great")
  return "OK"
}

