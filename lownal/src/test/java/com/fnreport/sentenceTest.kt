package com.fnreport

import com.fnreport.Emitter.Companion.nd_
import io.kotlintest.specs.StringSpec

class sentenceTest : StringSpec() {

    init {
        "capture"{

            val in1 = "OUT: <<$1 --> (/,REPRESENT,nd__,$2)> <=> <$1 --> (/,(/,REPRESENT,nd__,<(*,$2,FISH) --> FOOD>),nd__,eat,fish)>>. %1.00;0.45% {3 : 1;2}"
            val lit =  rel("OUT:") `&` ws  `&`  capture
            val test = lit.test(in1)
            System.err.println(lit.symbol to lit.name to lit.regex )
            System.err.println(in1)
            System.err.println(test)


        }
        "nards"{

            val in1 = "OUT: <<$1 --> (/,REPRESENT,nd__,$2)> <=> <$1 --> (/,(/,REPRESENT,nd__,<(*,$2,FISH) --> FOOD>),nd__,eat,fish)>>. %1.00;0.45% {3 : 1;2}"
            val lit = rel("OUT:") `&` ws `&`  oneOf(*sentence.values())
            val test = lit.test(in1)
            System.err.println(lit.symbol to lit.name to lit.rep )
            System.err.println(in1)
            System.err.println(test)


        }
    }
}
