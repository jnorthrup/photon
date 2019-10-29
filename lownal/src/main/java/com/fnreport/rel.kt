package com.fnreport

/**
 * regex literal -- any operator-enabled regex strings.
 */
class rel(override val input: String, override val regex: Regex = Regex (input)) : RegexEmitter