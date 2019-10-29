package com.fnreport

/**
 * This is an attempted hack to use compile-time reference to Nothing without the possibility of instantiation
 */

interface TokenMatcher {
    fun test(input: String): List<String>?

    companion object {
        public val nd_ = object : Ω {}
        public val `Ω` = nd_

    }
}