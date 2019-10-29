package com.fnreport

import com.fnreport.nards.capture
import com.fnreport.nards.sentence
import com.fnreport.nards.ws
import io.kotlintest.specs.StringSpec

class sentenceTest : StringSpec() {
    val in1 = "OUT: <<$1 --> (/,REPRESENT,`Ω`,$2)> <=> <$1 --> (/,(/,REPRESENT,`Ω`,<(*,$2,FISH) --> FOOD>),`Ω`,eat,fish)>>. %1.00;0.45% {3 : 1;2}"

    init {
        "capture"{

            val lit =  rel("OUT:") `&` ws `&` capture
            val test = lit.test(in1)
             System.err.println(in1)
            System.err.println(test)


        }
        "nards"{

            val lit = rel("OUT:") `&` ws `&`  oneOf(*sentence.values())
            val test = lit.test(in1)
             System.err.println(in1)
            System.err.println(test)
        }
    }
}
