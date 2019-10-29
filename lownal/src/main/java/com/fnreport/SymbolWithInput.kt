package com.fnreport

import com.fnreport.TokenMatcher.Companion.nd_
import com.fnreport.nards.div


fun <T : SymbolWithInput> oneOf(vararg a: T) = a.map {
    (it.takeIf { it is SymbolWithInput } ?: lit(it.toString())) 
}.toTypedArray().reduce { acc: SymbolWithInput, tokenTokenEmitter: SymbolWithInput -> (acc `|` tokenTokenEmitter) }



fun <T:SymbolWithInput>opt(l1:T) = l1 `?` nd_


interface SymbolWithInput {
    val input: String
    val symbol: String
        get() = input

}