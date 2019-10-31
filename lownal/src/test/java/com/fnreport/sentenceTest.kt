package com.fnreport

import com.fnreport.nards.*
import com.fnreport.nards.carrion.*
import com.github.h0tk3y.betterParse.combinators.*
import com.github.h0tk3y.betterParse.lexer.CharToken
import com.github.h0tk3y.betterParse.parser.Parser
import io.kotlintest.specs.StringSpec

class sentenceTest : StringSpec() {
    val in1 = "OUT: <(&&,<$1 --> (/,REPRESENT,_,$2)>,<(*,$3,$4) --> REPRESENT>) ==> <$1 --> (/,(/,REPRESENT,_,<(*,$4,$2) --> FOOD>),$3,eat,_)>>. %1.00;0.45%"

    init {
        "capture"{
        }
    }
}