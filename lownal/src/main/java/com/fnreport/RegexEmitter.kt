package com.fnreport

interface RegexEmitter : TokenEmitter {

    override val regex: Regex get() = symbol.toRegex()
    //tested in JVM: the list returned by groupValues is using idempotent strings so there's no benefits to returning lazy reification of matches
    override fun test(input: String): List<String>? = regex.matchEntire(input)?.groupValues

}