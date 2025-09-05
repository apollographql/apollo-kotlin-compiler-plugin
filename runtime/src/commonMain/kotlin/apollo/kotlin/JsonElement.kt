package apollo.kotlin

/**
 * A typealias for a type-unsafe Kotlin representation of JSON. This typealias is
 * mainly for internal documentation purposes and low-level manipulations and should
 * generally be avoided in application code.
 *
 * Number handling:
 * The JSON parser decodes numbers on the fly if they fit in a [Long]. Else they are
 * represented as [JsonElement]
 *
 * [JsonElement] can be any of:
 * - null
 * - String
 * - Boolean
 * - Long
 * - [JsonNumber]
 * - Map<String, [JsonElement]> where values are any of these values recursively
 * - List<[JsonElement]> where values are any of these values recursively
 */
typealias JsonElement = Any?