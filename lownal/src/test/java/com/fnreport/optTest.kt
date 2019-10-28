package com.fnreport

import io.kotlintest.specs.StringSpec
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class optTest : StringSpec() {
    init {

        "testOptOpaque" {

            var emitter =(lit("X1"))
            println(listOf(opt(emitter), emitter).map { it.run { Pair(name, rep) } })
            println(listOf(opt(emitter), emitter).map {
                val test = it.test("X1")
                println("${it.name} : ${it.symbol} : ${it.rep} : ${it.regex.toPattern().pattern()}")
                assertNotNull(test)

            })

            assertNull((emitter).test("1X1"))
            assertNull(opt(emitter).test("1X1"))
           (opt(  lit( 1))).also{emitter->

            assertNotNull((emitter).test("1"))
            assertNotNull(opt(emitter).test(""))
            assertNull(opt(emitter).test("2"))
            assertNull(opt(emitter).test("12"))
        }}
        "testTruth" {

            val emitter = accounting.truth

            println(listOf(opt(emitter), emitter).map { it.run { Pair(name, rep) } })
            println(listOf(opt(emitter), emitter).map {
                val test = it.test("X1")
                println("${it.name} : ${it.symbol} : ${it.rep} : ${it.regex.toPattern().pattern()}")
                assertNull(test)
            })

            assertNull(opt(emitter).test("1X1"))
        }


    }

}
