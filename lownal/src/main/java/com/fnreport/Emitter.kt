package com.fnreport

interface Emitter {
    val name:String
    fun test(input:String): List<String>?
}
