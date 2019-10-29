package com.fnreport

import com.fnreport.nards.accounting
import io.kotlintest.specs.StringSpec
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class optTest : StringSpec() {
    init {

        "testOptOpaque" {

            var emitter = (lit("X1"))
            val elements = opt(emitter)
            println(listOf<SymbolWithInput>(elements, emitter).map {
                it.run { Pair(this.input, symbol) }
            })


            assertNull((emitter).test("1X1"))
            assertNull(elements.test("1X1"))
            (opt(lit(1))).also { emitter ->

                assertNotNull((emitter).test("1"))
                assertNotNull(opt(emitter).test(""))
                assertNull(opt(emitter).test("2"))
                assertNull(opt(emitter).test("12"))
            }
        }
        "testTruth" {

            val emitter = accounting.truth


            assertNull(opt(emitter).test("1X1"))
        }


    }

}
