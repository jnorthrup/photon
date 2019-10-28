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

infix operator fun <P :  TokenEmitter> P.plus(c: Char) = this + ("\\" + c)
infix operator fun <T : Number,P :  TokenEmitter> P.plus(c: T) = this + (c).toString()
infix operator fun <T : CharSequence,P :  TokenEmitter> P.plus(c: T) = this + lit(c)
infix operator fun <T : TokenEmitter> T.plus(t: T) = "${symbol.takeUnless { it.length > 1 }
        ?: "(${symbol})"}+${t.symbol}"

infix operator fun <T : Any> T.plus(t: TokenEmitter) = lit(this) + t

infix operator fun <P :  TokenEmitter> P.times(c: Char) = this * ("\\" + c)
infix operator fun <T : Number,P :  TokenEmitter> P.times(c: T) = this * (c).toString()
infix operator fun <T : CharSequence,P :  TokenEmitter> P.times(c: T) = this * lit(c)
infix operator fun <T : Any,P :  TokenEmitter> T.times(t: P) = lit(this) * t
infix operator fun <T : TokenEmitter> T.times(t: T) = rel("${symbol.takeUnless { it.length > 1 }
        ?: "(${symbol})"}*${t.symbol}", name + "*" + t.name)

infix fun <P : TokenEmitter> P.`|`(c: Char) = this `|` ("\\" + c)
infix fun <T : Number, P : TokenEmitter> P.`|`(c: T) = this `|` c.toString()
infix fun <T : CharSequence, P : TokenEmitter> P.`|`(c: T) = this `|` lit(c)
infix fun <P : TokenEmitter> Char.`|`(i: P) = "\\" + toString() `|` i
infix fun <T : Number, P : TokenEmitter> T.`|`(i: P) = toString() `|` i
infix fun <T : CharSequence, P : TokenEmitter> T.`|`(i: P) = lit(this.toString() + "|" + i.symbol)
infix fun <T : TokenEmitter> T.`|`(c: T) = rel(this.symbol + "|" + c.symbol, name + "|" + c.name)

infix fun <P : TokenEmitter> P.`&`(c: Char) = this `&` "(\\$c)"
infix fun <T : Number, P : TokenEmitter> P.`&`(c: T) = this `&` c.toString()
infix fun <T : CharSequence, P : TokenEmitter> P.`&`(c: T) = this `&` lit(c)
infix fun <T : Any, P : TokenEmitter> T.`&`(t: P) = lit(this) `&` t
infix fun <T : TokenEmitter> T.`&`(t: T) = rel("${symbol.takeUnless { it.length > 1 }
        ?: "(${symbol})"}(${t.symbol})", name + ">>" + t.name)
