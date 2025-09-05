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

  val map = GetFooQuery.Data.Adapter.toJson(
    GetFooQuery.Data(
      GetFooQuery.Node(
        id = "42",
        onReview = GetFooQuery.OnReview("Great"),
        onProduct = null,
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

