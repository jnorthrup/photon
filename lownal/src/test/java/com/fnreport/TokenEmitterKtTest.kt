package com.fnreport

import io.kotlintest.specs.StringSpec

class TokenEmitterKtTest : StringSpec() {

    init {
        "anyOf" {
            System.err.println( anyOf(1,2,3,4,65,7,4,78,9,45,3,7,9,4 ).regex)
        }
    }

}