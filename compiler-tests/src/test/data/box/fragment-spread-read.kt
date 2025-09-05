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
  val data = GetFooQuery.Data.Adapter.fromJson(
    mapOf(
      "node" to mapOf(
        "id" to "42",
        "__productDetails" to "Product",
        "price" to 42.0
      )
    )
  )
  check(data.node.productDetails?.price == 42.0)
  check(data.node.id == "42")
  check(data.node.reviewDetails == null)
  return "OK"
}

