package com.fnreport

interface Emitter {
    val name: String
    fun test(input: String): List<String>?
}

interface TokenEmitter : Emitter {
    val symbol: String
    val rep: String
        get() = symbol
    val regex: Regex
        get() = symbol.replace("(\\W)".toRegex(), "\\\\$1").toRegex()

    override fun test(input: String): List<String>? = symbol.takeIf(input::equals)?.let { listOf(it) }
}

interface RegexEmitter : TokenEmitter {

    override val regex: Regex get() = symbol.toRegex()
    //tested in JVM: the list returned by groupValues is using idempotent strings so there's no benefits to returning lazy reification of matches
    override fun test(input: String): List<String>? = regex.matchEntire(input)?.groupValues

}

class rel(override val symbol: String, override val name: String = symbol) : RegexEmitter


class lit(c: Any, override val name: String = c.toString(), override val symbol: String = name) : TokenEmitter


infix operator fun <P : TokenEmitter> Char.plus(i: P) = "\\" + toString() plus i
infix operator fun <P : TokenEmitter> P.plus(c: Char) = this plus ("\\" + c)
infix operator fun <T : Number, P : TokenEmitter> T.plus(i: P) = toString() plus i
infix operator fun <T : Number, P : TokenEmitter> P.plus(c: T) = this plus c.toString()
infix operator fun <T : CharSequence, P : TokenEmitter> P.plus(c: T) = this plus lit(c)
infix operator fun <T : CharSequence, P : TokenEmitter> T.plus(i: P) = lit(this.toString() + "+" + i.symbol)
infix operator fun <T : TokenEmitter> T.plus(t: T) = rel("${symbol.takeUnless { it.length > 1 }
        ?: "(${symbol})"}+${t.symbol}") as RegexEmitter


infix operator fun <P : TokenEmitter> Char.times(i: P) = "\\" + toString() times i
infix operator fun <P : TokenEmitter> P.times(c: Char) = this times ("\\" + c)
infix operator fun <T : Number, P : TokenEmitter> T.times(i: P) = toString() times i
infix operator fun <T : Number, P : TokenEmitter> P.times(c: T) = this times c.toString()
infix operator fun <T : CharSequence, P : TokenEmitter> P.times(c: T) = this times lit(c)
infix operator fun <T : CharSequence, P : TokenEmitter> T.times(i: P) = lit(this.toString() + "*" + i.symbol)
infix operator fun <T : TokenEmitter> T.times(t: T) = rel("${symbol.takeUnless { it.length > 1 }
        ?: "(${symbol})"}*${t.symbol}", name + "*" + t.name)


infix fun <T, P : TokenEmitter> P.`|`(any: T) = this `|` any.toString()
infix fun <P : TokenEmitter> Char.`|`(i: P) = "\\" + toString() `|` i
infix fun <P : TokenEmitter> P.`|`(c: Char) = this `|` ("\\" + c)
infix fun <T : Number, P : TokenEmitter> T.`|`(i: P) = toString() `|` i
infix fun <T : Number, P : TokenEmitter> P.`|`(c: T) = this `|` c.toString()
infix fun <T : CharSequence, P : TokenEmitter> P.`|`(c: T) = this `|` lit(c)
infix fun <T : CharSequence, P : TokenEmitter> T.`|`(i: P) = lit(this.toString() + "|" + i.symbol)
infix fun <T : TokenEmitter> T.`|`(c: T) = rel(this.symbol + "|" + c.symbol, name + "|" + c.name) as RegexEmitter


infix fun <P : TokenEmitter> Char.`&`(i: P) = "\\" + toString() `&` i
/**
 * hackish bit to wrap all in parens
 */
fun <P : TokenEmitter> P.`&`() = this `&` ("")

infix fun <P : TokenEmitter> P.`&`(c: Char) = this `&` ("\\" + c)
infix fun <T : Number, P : TokenEmitter> T.`&`(i: P) = toString() `&` i
infix fun <T : Number, P : TokenEmitter> P.`&`(c: T) = this `&` c.toString()
infix fun <T : CharSequence, P : TokenEmitter> P.`&`(c: T) = this `&` lit(c)
infix fun <T : CharSequence, P : TokenEmitter> T.`&`(i: P) = lit("()" / this.toString() + i.symbol)
infix fun <T : TokenEmitter> T.`&`(t: T) = rel("${symbol.takeUnless { it.length > 1 }
        ?: "(${symbol})"}(${t.symbol})", name + ">>" + t.name) as RegexEmitter


infix fun <P : TokenEmitter> Char.`?`(i: P) = "\\" + toString() `?` i
fun <P : TokenEmitter> P.`?`() = lit(symbol) `?` ""
infix fun <P : TokenEmitter> P.`?`(c: Char) = this `?` ("\\" + c)
infix fun <T : Number, P : TokenEmitter> T.`?`(i: P) = toString() `?` i
infix fun <T : Number, P : TokenEmitter> P.`?`(c: T) = this `?` c.toString()
infix fun <T : CharSequence, P : TokenEmitter> P.`?`(c: T) = this `?` lit(c)
infix fun <T : CharSequence, P : TokenEmitter> T.`?`(i: P) = lit(this.toString() + "?" + i.symbol)
infix fun <T : TokenEmitter> T.`?`(t: T) = rel("${symbol.takeUnless { it.length > 1 }
        ?: "($symbol)"}?${t.symbol}", name + "?" + t.name) as RegexEmitter


fun <T : TokenEmitter> oneOf(vararg a: T) = a.map {
    (it.takeIf { it is TokenEmitter } ?: lit(it.toString())) as TokenEmitter
}.toTypedArray().reduce { acc: TokenEmitter, tokenEmitter: TokenEmitter -> (acc `|` tokenEmitter) }

fun <T> anyOf(vararg a: T): TokenEmitter = a.map {
    (it.takeIf { it is TokenEmitter } ?: lit(it.toString())) as TokenEmitter
}.toTypedArray().reduce { acc: TokenEmitter, tokenEmitter: TokenEmitter -> (acc `?` tokenEmitter) }


fun opt(l1: TokenEmitter) = l1.`?`()


