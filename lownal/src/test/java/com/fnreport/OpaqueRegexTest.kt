package com.fnreport

import io.kotlintest.specs.StringSpec

class OpaqueRegexTest : StringSpec() {

    init {
        "regex" {

            System.err.println(( "<==>" as CharSequence + rel("<==>")).regex)
            System.err.println(("<==>" *  rel("<==>")).regex)
            System.err.println(("<==>" `&`  rel("<==>")).regex)
            System.err.println(("<==>" `|`  rel("<==>")).regex)
            System.err.println(("<==>" `?`  rel("<==>")).regex)


        }
    }

}