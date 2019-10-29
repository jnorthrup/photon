package com.fnreport

open class OpaqueRegex(override val input: String) : WithRegex {
    override val regex: Regex
        get() = input.replace("(\\W)".toRegex(), "\\\\$1").toRegex()
}