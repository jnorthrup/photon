package com.fnreport

interface TokenEmitter : Emitter {
    val symbol: String
    val rep: String
        get() = symbol
    val regex: Regex
        get() = symbol.replace("(\\W)".toRegex(), "\\\\$1").toRegex()
    open override fun test(input: String): List<String>? = symbol.takeIf(input::equals)?.let { listOf(it) }

}

class lit(c: Any, override val name: String = c.toString(), override val symbol: String = name) : TokenEmitter
