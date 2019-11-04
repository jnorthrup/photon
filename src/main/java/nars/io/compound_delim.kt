package nars.io

/**
 *
 */
 enum class compound_delim(val sym:Char) {

    COMPOUND_TERM_OPENER('('),
    COMPOUND_TERM_CLOSER(')'),
    STATEMENT_OPENER('<'),
    STATEMENT_CLOSER('>'),
    SET_EXT_OPENER('{'),
    SET_EXT_CLOSER('}'),
    SET_INT_OPENER('['),
    SET_INT_CLOSER(']'),
}