package com.fnreport

import com.fnreport.nards.*
import com.fnreport.nards.carrion.*
import io.kotlintest.specs.StringSpec

class sentenceTest : StringSpec() {
    val in1 = "OUT: <(&&,<$1 --> (/,REPRESENT,_,$2)>,<(*,$3,$4) --> REPRESENT>) ==> <$1 --> (/,(/,REPRESENT,_,<(*,$4,$2) --> FOOD>),$3,eat,_)>>. %1.00;0.45%"

    init {
        "capture"{
            val lit = rel("OUT:") `&` ws `&` capture
            val test = lit.test(in1)
            System.err.println(in1)
            System.err.println(test)
        }
        "truth"{
            val t = "%1.00;0.45% {3 : 1;2}"
            val rel = capture `&` accounting.truth `&` capture
            System.err.println(rel.regex)
            val test = rel.test(t)
            System.err.println(test)
            System.err.println( ".*%([01]([.]\\d*)?|[.]\\d*)(;([01]([.]\\d*)?|[.]\\d*))?%(.*)"
                    .toRegex().matchEntire(t)?.groupValues)
        }
        "nards"{
            val lit = (capture  `&` ':' `&` ws  ) `?` (oneOf(*sentence.values()))
            val test = lit.test(in1)
            System.err.println(in1)
            System.err.println(test)
        }
    }
}
