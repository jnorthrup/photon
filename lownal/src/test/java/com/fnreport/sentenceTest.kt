package com.fnreport

import com.fnreport.nards.*
import com.github.h0tk3y.betterParse.combinators.OrCombinator
import com.github.h0tk3y.betterParse.grammar.Grammar
import com.github.h0tk3y.betterParse.grammar.parseToEnd
import com.github.h0tk3y.betterParse.lexer.TokenMatch
import com.github.h0tk3y.betterParse.lexer.regexToken
import io.kotlintest.specs.StringSpec

class sentenceTest : StringSpec() {
    val _a = fragment.word.ordinal

    init {
        "capture"{
            val in1: String = "OUT: <(&&,<$1 --> (/,REPRESENT,_,$2)>,<(*,$3,$4) --> REPRESENT>) ==> <$1 --> (/,(/,REPRESENT,_,<(*,$4,$2) --> FOOD>),$3,eat,_)>>. %1.00;0.45%"
            val x = lit("IN") `|` "OUT" `&` ':' `&` regexToken("\\s+".toRegex(), true) `&` OrCombinator(sentence.values().toList())

            val ItemsParser = object : Grammar<TokenMatch>() {
                override val rootParser by x
            }
            val result = ItemsParser.parseToEnd(in1)
        }
    }
}