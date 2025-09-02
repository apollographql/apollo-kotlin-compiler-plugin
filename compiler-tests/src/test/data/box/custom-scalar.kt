// FIR_DUMP
// DUMP_IR
// POINT_SCALAR

// FILE: Point.kt
package lib

import apollo.kotlin.assertExpectedType
import apollo.kotlin.JsonElement
import apollo.kotlin.Adapter

class Point(val x: Double, val y: Double)

object PointAdapter : Adapter<Point> {
  override fun fromJson(element: JsonElement): Point {
    assertExpectedType<Map<*, *>>(element)
    return Point(
      element.get("x") as Double,
      element.get("y") as Double,
    )
  }

  override fun toJson(value: Point): JsonElement {
    return mapOf(
      "x" to value.x,
      "y" to value.y,
    )
  }
}

// FILE: box.kt
import apollo.kotlin.Query
import lib.Point
import lib.PointAdapter

@Query("{point}")
class PointQuery

fun box(): String {
  val adapter = PointQuery.Data.Adapter
  val data = adapter.fromJson(
    mapOf(
      "point" to mapOf(
        "x" to 1.0,
        "y" to 2.0,
      )
    )
  )

  check(data.point.x == 1.0)
  check(data.point.y == 2.0)

  val map = adapter.toJson(data)
  map as Map<String, Any?>
  val point = map.get("point")
  point as Map<String, Any?>
  check(point.get("x") == 1.0)
  check(point.get("y") == 2.0)

  return "OK"
}

