package com.fnreport

class oneOf(vararg testFor: TokenEmitter) : RegexEmitter {
    val testFor = testFor
    override val symbol = testFor.map { it.symbol }.joinToString(separator = "|")
    override val regex =  testFor.map { val pattern = it.regex.pattern
        pattern
    }.joinToString<String>(  "|",   "(",   ")").toRegex()
    override val name: String = testFor.map(TokenEmitter::name).joinToString("|")
}