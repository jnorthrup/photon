package com.fnreport

import com.fnreport.nards.div

private fun <T : SymbolWithInput> T.compend(oper: String, t: T): rel {
    return rel("${if (input.startsWith("(") and input.endsWith(")")) input else if (input.isNotBlank()) "()" / input else ""}" +
            oper +
            "${if (t.input.isNotBlank()) "()" / t.input else ""}")
}

/**
 * exceptional case over the others overriding existing  String+
 */
infix operator fun <T : String , P : SymbolWithInput> T.plus(i: P)=  this as CharSequence plus i


infix operator fun <P : SymbolWithInput> P.plus(nd___: `Ω`) = lit(input) plus ""
infix operator fun <P : SymbolWithInput> Char.plus(i: P) = "\\" + toString() plus i
infix operator fun <P : SymbolWithInput> P.plus(c: Char) = this plus ("\\" + c)
infix operator fun <T : Number, P : SymbolWithInput> T.plus(i: P) = toString() plus i
infix operator fun <T : Number, P : SymbolWithInput> P.plus(c: T) = this plus c.toString()
infix operator fun <T : CharSequence, P : SymbolWithInput> P.plus(c: T) = this plus lit(c)
infix operator fun <T : CharSequence, P : SymbolWithInput> T.plus(i: P)=rel("()" / lit(this.toString()).regex.pattern) plus i.input
infix operator fun <T : SymbolWithInput> T.plus(t: T)  = compend("+", t)



infix operator fun <P : SymbolWithInput> P.times(nd___: `Ω`) = lit(input) times ""
infix operator fun <P : SymbolWithInput> Char.times(i: P) = "\\" + toString() times i
infix operator fun <P : SymbolWithInput> P.times(c: Char) = this times ("\\" + c)
infix operator fun <T : Number, P : SymbolWithInput> T.times(i: P) = toString() times i
infix operator fun <T : Number, P : SymbolWithInput> P.times(c: T) = this times c.toString()
infix operator fun <T : CharSequence, P : SymbolWithInput> P.times(c: T) = this times lit(c)
infix operator fun <T : CharSequence, P : SymbolWithInput> T.times(i: P)=rel("()" / lit(this.toString()).regex.pattern) times i.input
infix operator fun <T : SymbolWithInput> T.times(t: T)  = compend("*", t)

infix fun <P : SymbolWithInput> P.`|`(nd___: `Ω`) = lit(input) `|` ""
infix fun <P : SymbolWithInput> Char.`|`(i: P) = "\\" + toString() `|` i
infix fun <P : SymbolWithInput> P.`|`(c: Char) = this `|` ("\\" + c)
infix fun <T : Number, P : SymbolWithInput> T.`|`(i: P) = toString() `|` i
infix fun <T : Number, P : SymbolWithInput> P.`|`(c: T) = this `|` c.toString()
infix fun <T : CharSequence, P : SymbolWithInput> P.`|`(c: T) = this `|` lit(c)
infix fun <T : CharSequence, P : SymbolWithInput> T.`|`(i: P)=rel("()" / lit(this.toString()).regex.pattern) `|` i.input
infix fun <T : SymbolWithInput> T.`|`(t: T)  = compend("|", t)



infix fun <P : SymbolWithInput> P.`&`(nd___: `Ω`) = lit(input) `&` ""
infix fun <P : SymbolWithInput> Char.`&`(i: P) = "\\" + toString() `&` i
infix fun <P : SymbolWithInput> P.`&`(c: Char) = this `&` ("\\" + c)
infix fun <T : Number, P : SymbolWithInput> T.`&`(i: P) = toString() `&` i
infix fun <T : Number, P : SymbolWithInput> P.`&`(c: T) = this `&` c.toString()
infix fun <T : CharSequence, P : SymbolWithInput> P.`&`(c: T) = this `&` lit(c)
infix fun <T : CharSequence, P : SymbolWithInput> T.`&`(i: P)=rel("()" / lit(this.toString()).regex.pattern) `&` i.input
infix fun <T : SymbolWithInput> T.`&`(t: T)  = compend("", t)

infix fun <P : SymbolWithInput> P.`?`(nd___: `Ω`) = lit(input) `?` ""
infix fun <P : SymbolWithInput> Char.`?`(i: P) = "\\" + toString() `?` i
infix fun <P : SymbolWithInput> P.`?`(c: Char) = this `?` ("\\" + c)
infix fun <T : Number, P : SymbolWithInput> T.`?`(i: P) = toString() `?` i
infix fun <T : Number, P : SymbolWithInput> P.`?`(c: T) = this `?` c.toString()
infix fun <T : CharSequence, P : SymbolWithInput> P.`?`(c: T) = this `?` lit(c)
infix fun <T : CharSequence, P : SymbolWithInput> T.`?`(i: P)=rel("()" / lit(this.toString()).regex.pattern) `?` i.input
infix fun <T : SymbolWithInput> T.`?`(t: T)  = compend("?", t)
