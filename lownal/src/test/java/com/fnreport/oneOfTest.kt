package com.fnreport

import io.kotlintest.specs.StringSpec
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class oneOfTest : StringSpec() {

    init {
        "test" {

                val one = lit(1)
                val dua = lit(2)

                val oneOf2 = one `|` dua  //   (1)?|2


                assertNotNull(oneOf2.test("2").also(::println))
                assertNotNull(oneOf2.test("1").also(::println))
                assertNull(oneOf2.test("12").also(::println))
                assertNull(oneOf2.test("3").also(::println))
                val both = one + dua


            }
    }

}