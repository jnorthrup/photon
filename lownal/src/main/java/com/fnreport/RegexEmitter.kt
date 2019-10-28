package com.fnreport

interface RegexEmitter : TokenEmitter {

    override val regex: Regex get() = symbol.toRegex()
    //tested in JVM: the list returned by groupValues is using idempotent strings so there's no benefits to returning lazy reification of matches
    override fun test(input: String): List<String>? = regex.matchEntire(input)?.groupValues

}

class  rel(override val symbol: String, override val name: String =symbol) :   RegexEmitter


infix fun <T : Char> T.`|`(i: RegexEmitter):RegexEmitter = toString() `|` i
infix fun  <T:Number>  T.`|`(i: RegexEmitter):RegexEmitter = toString() `|` i
infix fun <T : CharSequence> T.`|`(i: RegexEmitter):RegexEmitter = oneOf(lit(this), i)
infix operator fun TokenEmitter.plus(dua: TokenEmitter): RegexEmitter = let {
    object : RegexEmitter {
        override val name: String
            get() = symbol
        override val symbol: String = "(${it.symbol})+(${dua.symbol})"
    }
}
