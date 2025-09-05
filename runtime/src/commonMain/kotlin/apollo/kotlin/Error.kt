package apollo.kotlin

/**
 * Represents an error response returned from the GraphQL server
 * See https://spec.graphql.org/draft/#sec-Errors.Error-result-format
 */
class Error(
  /**
   * Server error message
   */
  val message: String,

  /**
   * Locations of the errors in the GraphQL operation.
   * `null` if the location cannot be determined.
   */
  val locations: List<Location>?,

  /**
   * If this error is a field error, the path of the field where the error happened.
   * Values in the list can be either Strings or Int.
   * Null for requests errors.
   */
  val path: List<Any>?,

  /**
   * Extensions if any.
   */
  val extensions: Map<String, JsonElement>?,
) {
  /**
   * Represents the location of the error in the GraphQL operation sent to the server. This location is represented in
   * terms of the line and column number.
   */
  class Location(
    /**
     * Line number of the error location
     */
    val line: Int,

    /**
     * Column number of the error location.
     */
    val column: Int,
  )
}