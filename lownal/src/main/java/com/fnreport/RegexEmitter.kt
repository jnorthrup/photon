package com.fnreport

open interface nd___

typealias  `Ω` = nd___
typealias  o = OpaqueRegex

interface RegexEmitter : WithRegex, TokenMatcher {
    //tested in JVM: the list returned by groupValues is using idempotent strings so there's no benefits to returning lazy reification of matches
    override fun test(input: String): List<String>? = regex.matchEntire(input)?.groupValues
}


