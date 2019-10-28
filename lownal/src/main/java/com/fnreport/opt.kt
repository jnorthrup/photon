package com.fnreport




class opt(
        var emitter:  TokenEmitter,
        override val name: String = "opt" + ("()" / emitter.name),
        override val regex: Regex = Regex(("()" / emitter.regex.pattern) + "?"),
        override val symbol: String = ("()" / emitter.symbol) + "?"
) : RegexEmitter {
}