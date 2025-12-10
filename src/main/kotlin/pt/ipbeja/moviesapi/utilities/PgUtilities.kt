@file:Suppress("SpellCheckingInspection")

package pt.ipbeja.moviesapi.utilities

import org.jetbrains.exposed.v1.core.*

class ILikeEscapeOp(expr1: Expression<*>, expr2: Expression<*>, like: Boolean, val escapeChar: Char?) :
    ComparisonOp(expr1, expr2, if (like) "ILIKE" else "NOT ILIKE") {
    override fun toQueryBuilder(queryBuilder: QueryBuilder) {
        super.toQueryBuilder(queryBuilder)
        if (escapeChar != null) {
            with(queryBuilder) {
                +" ESCAPE "
                +stringParam(escapeChar.toString())
            }
        }
    }
}
infix fun <T : String?> Expression<T>.ilike(pattern: LikePattern): ILikeEscapeOp =
    ILikeEscapeOp(this, stringParam(pattern.pattern), true, pattern.escapeChar)

infix fun <T : String?> Expression<T>.ilike(pattern: String): ILikeEscapeOp = ilike(LikePattern(pattern))


/*
data class ILikePattern(
    */
/** The string representation of a pattern to match. *//*

    val pattern: String,
    */
/** The special character to use as the escape character. *//*

    val escapeChar: Char? = null
) {

    infix operator fun plus(rhs: ILikePattern): ILikePattern {
        require(escapeChar == rhs.escapeChar) { "Mixing escape chars '$escapeChar' vs. '${rhs.escapeChar} is not allowed" }
        return ILikePattern(pattern + rhs.pattern, rhs.escapeChar)
    }

    infix operator fun plus(rhs: String): ILikePattern {
        return ILikePattern(pattern + rhs, escapeChar)
    }

    companion object {
        */
/** Creates a [ILikePattern] from the provided [text], with any special characters escaped using [escapeChar]. *//*

        fun ofLiteral(text: String, escapeChar: Char = '\\'): ILikePattern {
            val likePatternSpecialChars = currentDialect.likePatternSpecialChars
            val nextExpectedPatternQueue = arrayListOf<Char>()
            var nextCharToEscape: Char? = null
            val escapedPattern = buildString {
                text.forEach {
                    val shouldEscape = when (it) {
                        escapeChar -> true
                        in likePatternSpecialChars -> {
                            likePatternSpecialChars[it]?.let { nextChar ->
                                nextExpectedPatternQueue.add(nextChar)
                                nextCharToEscape = nextChar
                            }
                            true
                        }
                        nextCharToEscape -> {
                            nextExpectedPatternQueue.removeLast()
                            nextCharToEscape = nextExpectedPatternQueue.lastOrNull()
                            true
                        }
                        else -> false
                    }
                    if (shouldEscape) {
                        append(escapeChar)
                    }
                    append(it)
                }
            }
            return ILikePattern(escapedPattern, escapeChar)
        }
    }
}
*/

