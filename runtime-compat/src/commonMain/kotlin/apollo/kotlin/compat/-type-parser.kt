package apollo.kotlin.compat

import com.apollographql.apollo.api.CompiledListType
import com.apollographql.apollo.api.CompiledNamedType
import com.apollographql.apollo.api.CompiledNotNullType
import com.apollographql.apollo.api.CompiledType
import com.apollographql.apollo.api.CustomScalarType
import com.apollographql.apollo.api.ObjectType


internal class Lexer(val src: String) {
  private var pos = 0
  private val len = src.length
  private var started = false

  private fun Char.isNameStart(): Boolean {
    return when (this) {
      '_',
      in 'A'..'Z',
      in 'a'..'z',
        -> true

      else -> false
    }
  }

  fun nextToken(): Token {
    if (!started) {
      started = true
      return Token.StartOfFile
    }

    while (pos < len) {
      val c = src[pos]

      // do not consume the byte just yet, names and numbers need the first byte
      if (c.isNameStart()) {
        return readName()
      }
      // everything else can consume the byte
      val start = pos
      pos++

      when (c) {
        '!' -> return Token.ExclamationPoint
        '[' -> return Token.LeftBracket
        ']' -> return Token.RightBracket
        '-' -> return Token.Hyphen

        else -> {
          error("Unexpected symbol '${c}' (0x${c.code.toString(16)})")
        }
      }
    }

    return Token.EndOfFile
  }

  private fun Char.isNameContinue(): Boolean {
    return when (this) {
      '_',
      in '0'..'9',
      in 'A'..'Z',
      in 'a'..'z',
        -> true

      else -> false
    }
  }

  private fun readName(): Token {
    val start = pos

    // we're guaranteed this is a name start
    pos++

    while (pos < len) {
      val c = src[pos]
      if (c.isNameContinue()) {
        pos++
      } else {
        break
      }
    }
    return Token.Name(
      value = src.substring(start, pos)
    )
  }
}

internal sealed interface Token {
  data object StartOfFile : Token {
    override fun toString() = "SOF"
  }

  data object EndOfFile : Token {
    override fun toString() = "EOF"
  }

  data object ExclamationPoint : Token {
    override fun toString() = "!"
  }

  data object LeftBracket : Token {
    override fun toString() = "["
  }

  data object RightBracket : Token {
    override fun toString() = "]"
  }

  data object Hyphen : Token {
    override fun toString() = "-"
  }

  class Name( val value: String) : Token {
    override fun toString() = "name: $value"
  }
}

internal class Parser(
  src: String,
) {
  private val lexer = Lexer(src)
  private var token = lexer.nextToken()
  private var lastToken = token
  private var lookaheadToken: Token? = null

  fun parseType(): CompiledType {
    return parseTopLevel(::parseTypeInternal)
  }

  private fun advance() {
    lastToken = token
    if (lookaheadToken != null) {
      token = lookaheadToken!!
      lookaheadToken = null
    } else {
      token = lexer.nextToken()
    }
  }

  private inline fun <reified T : Token> expectToken(): T {
    val start = token
    if (start is T) {
      advance()
      return start
    } else {
      error("Expected ${T::class.simpleName}, found '$token'.")
    }
  }

  private fun parseName(): String {
    return expectToken<Token.Name>().value
  }


  private fun parseNamedType(): CompiledNamedType {
    val scalar = expectOptionalToken<Token.Hyphen>()
    val name = parseName()
    /**
     * This isn't 100% correct but should be good enough for Normalizer purposes
     */
    return if (scalar != null) {
      CustomScalarType(name, "")
    } else {
      ObjectType.Builder(name = name).build()
    }
  }

  private fun <T> parseTopLevel(block: () -> T): T {
    expectToken<Token.StartOfFile>()
    return block().also {
      expectToken<Token.EndOfFile>()
    }
  }
  private inline fun <reified T : Token> expectOptionalToken(): T? {
    val start = token
    if (start is T) {
      advance()
      return start
    }
    return null
  }


  private fun parseTypeInternal(): CompiledType {
    val type = if (expectOptionalToken<Token.LeftBracket>() != null) {
      val ofType = parseTypeInternal()
      expectToken<Token.RightBracket>()

      CompiledListType(ofType)
    } else {
      parseNamedType()
    }

    return if (token is Token.ExclamationPoint) {
      advance()
      CompiledNotNullType(type)
    } else {
      type
    }
  }
}

internal fun String.parseType(): CompiledType {
  return Parser(this).parseType()
}