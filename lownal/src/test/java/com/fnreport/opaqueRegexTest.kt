package com.fnreport

import io.kotlintest.specs.StringSpec

class opaqueRegexTest : StringSpec() {

    init {
        "aaa"{
            val x=OpaqueRegex("<==>")

            System.err.println(x.symbol)
        }
    }

}