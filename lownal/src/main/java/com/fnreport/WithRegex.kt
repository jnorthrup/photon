package com.fnreport

interface WithRegex : SymbolWithInput {
    val regex: Regex get() = input.toRegex()

}