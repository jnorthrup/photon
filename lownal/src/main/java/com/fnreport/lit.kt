package com.fnreport

/** String Literal.  anything tostring, or anything escaped to be opaue*/
class lit(c: Any, s: String = c.toString()) : OpaqueRegex(s), TokenMatcher {
    override fun test(challenge: String) = this.input.takeIf(challenge::equals)?.let { listOf(it) }
}