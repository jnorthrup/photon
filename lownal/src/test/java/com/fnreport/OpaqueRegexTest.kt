package com.fnreport

import com.fnreport.nards.*
import com.fnreport.nards.nd_
import io.kotlintest.specs.StringSpec

class OpaqueRegexTest : StringSpec() {

    init {
        "regex" {
            System.err.println(lit("<==>") `&`  "<==>"  )
            System.err.println(lit("<==>") *   "<==>"  )
            System.err.println(lit("<==>")  `&`  "<==>"   )
            System.err.println(lit("<==>")  `|`  "<==>"   )
            System.err.println(lit("<==>")  `?`  "<==>"   )
        }
    }

}