package com.fnreport.nards

import com.fnreport.OpaqueRegex
import com.fnreport.`&`
import com.fnreport.lit

interface SetOp   {
    val opn: OpaqueRegex
    val cls: OpaqueRegex

    infix operator fun div(e: String) =
                    lit(this.opn.input) `&` e `&` this.cls.input

}
