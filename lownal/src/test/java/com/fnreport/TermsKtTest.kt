package com.fnreport

import com.fnreport.TokenMatcher.Companion.nd_
import com.fnreport.nards.carrion

import io.kotlintest.specs.StringSpec
import kotlin.test.assertNotNull
import kotlin.test.assertNull


class TermsKtTest : StringSpec() {
    init {
        val l1 = lit(1)
        val l2 = lit(2)


        "plus" {
            val s = l1 + 2

            let {
                val regex = s.regex
                val pattern = regex.pattern
                System.err.println(pattern)
                val matchEntire = regex.matchEntire("12")
                val matches = regex.matches("12")
            }
            let {
                val regex1 = Regex("1+2")
                val regex = regex1
                val pattern = regex.pattern
                System.err.println(pattern)
                val matchEntire = regex.matchEntire("12")
                val matches = regex.matches("12")
                System.err.println(matchEntire)
            }


            val actual = s.test("12")
            System.err.println(assertNotNull(actual))
            System.err.println(assertNotNull((l1 + 2 + 1 + l1).test("1211")))
            System.err.println(assertNotNull((2 + l1 + 1 + l1).test("2111")))
            var regexEmitter = 2 + l1 + 1 + l1
            System.err.println(regexEmitter.regex)
            System.err.println(assertNotNull((regexEmitter).test("22222111")))
            System.err.println(assertNull((1 + 2 + l1 + 1 + l1).test("2111")))
            System.err.println(assertNotNull((1 + 2 + l1 + 1 + l1).test("3111")))
            System.err.println(assertNotNull((l1 + 2 + opt(l1)).test("12")))
            System.err.println(assertNotNull((l1 + 2 + opt(l1)).test("121")))

            regexEmitter = l1 + 2 + ((oneOf(lit(33), lit(4)) as SymbolWithInput) `&` nd_)

            System.err.println(regexEmitter.regex.pattern)
            System.err.println(assertNotNull(regexEmitter.test("124")))
            System.err.println(assertNull(('z' + l1 + l2 + l1 + l1).test("1211")))
            System.err.println(assertNull(('z' + l1 + l2 + opt(l1)).test("12")))
            System.err.println(assertNull(('z' + l1 + l2 + opt(l1)).test("121")))
            System.err.println(assertNull(('z' + l1 + l2 + oneOf(lit(33), lit(433))).test("124")))
        }


        "times"   {
            System.err.println(assertNotNull((l1 * 2).test("12")))
            System.err.println(assertNotNull((l1 * 2 * 1 * l1).test("1211")))
            System.err.println(assertNotNull((2 * l1 * 1 * l1).test("2111")))
            System.err.println(assertNotNull((l1 * 2 * opt(l1)).test("12")))
            System.err.println(assertNotNull((l1 * 2 * opt(l1)).test("121")))

            val rel = l1 * 2 * (oneOf(lit(3), lit(4)))
            System.err.println(rel.regex)
            System.err.println(assertNotNull(rel.test("4")))


        }

        "&" {
            val regexEmitter1 = l1 `&` 2
            System.err.println(assertNotNull((regexEmitter1).test("12")))
            System.err.println(assertNotNull((l1 `&` 2 `&` 1 `&` l1).test("1211")))
            val regexEmitter = 2 `&` l1 `&` 1 `&` l1 `&` carrion.ws


            System.err.println(assertNotNull(regexEmitter.test("2111")))
            System.err.println(assertNotNull((l1 `&` 2 `&` opt(l1)).test("12")))
            System.err.println(assertNotNull((l1 `&` 2 `&` opt(l1)).test("121")))

            val rel = l1 `&` 2 `&` oneOf(lit(3), lit(4))
            System.err.println(rel.regex)
            System.err.println(assertNotNull(rel.test("124")))
            System.err.println(assertNull(('z' `&` l1 `&` l2 `&` l1 + l1).test("1211")))
            System.err.println(assertNull(('z' `&` l1 `&` l2 `&` opt(l1)).test("12")))
            System.err.println(assertNull(('z' `&` l1 `&` l2 `&` opt(l1)).test("121")))
            System.err.println(assertNull(('z' `&` l1 `&` l2 `&` oneOf(lit(3), lit(4))).test("124")))

        }
        "|" {
            System.err.println(assertNotNull((l1 `|` 2).test("2")))
            System.err.println(assertNotNull((l1 `|` 2 `|` 1 `|` l1).test("2")))
            System.err.println(assertNotNull((l1 `|` 2 `|` 1 `|` l1).test("1")))
            System.err.println(assertNotNull((2 `|` l1 `|` 1 `|` l1).test("1")))
            System.err.println(assertNotNull((2 `|` l1 `|` 1 `|` l1).test("2")))


            val rel1 = l1 `|` 2 `|` opt(l1)
            System.err.println(rel1.regex)
            System.err.println(assertNull((rel1).test("21")))
            System.err.println(assertNotNull((rel1).test("2")))
            System.err.println(assertNotNull((rel1).test("1")))
            System.err.println(assertNotNull((rel1).test("")))
            System.err.println(assertNotNull((l1 `|` 2 `|` opt(l1)).test("1")))
            System.err.println(assertNotNull((l1 `|` 2 `|` opt(l1)).test("2")))
            System.err.println(assertNull((l1 `|` 2 `|` opt(l1)).test("11")))
            System.err.println(assertNull((l1 `|` 2 `|` opt(l1)).test("12")))

            val rel = l1 `|` 2 `|` oneOf(lit(3), lit(4))
            System.err.println(rel.regex)
            System.err.println(assertNull(rel.test("124")))
            System.err.println(assertNull(rel.test("0")))
            System.err.println(assertNotNull(rel.test("1")))
            System.err.println(assertNotNull(rel.test("2")))
            System.err.println(assertNotNull(rel.test("3")))
            System.err.println(assertNull(rel.test("5")))
            System.err.println(assertNull(('z' `|` l1 `|` l2 `|` l1 + l1).test("1211")))
            System.err.println(assertNull(('z' `|` l1 `|` l2 `|` opt(l1)).test("12")))
            System.err.println(assertNull(('z' `|` l1 `|` l2 `|` opt(l1)).test("121")))
            System.err.println(assertNull(('z' `|` l1 `|` l2 `|` oneOf(lit(3), lit(4))).test("124")))
        }

    }
}


