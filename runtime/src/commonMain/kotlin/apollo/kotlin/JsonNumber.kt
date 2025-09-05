package apollo.kotlin

/**
 * A simple wrapper class that can be put in [Map] to indicate an arbitrary precision JSON number
 */
class JsonNumber(val value: String) {
  override fun toString(): String = value
}