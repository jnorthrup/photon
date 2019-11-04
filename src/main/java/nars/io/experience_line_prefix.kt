package nars.io

/**
 *
 */
  enum class experience_line_prefix (/*x:Any,*/val sym:Any/*=x.toString()*/) {
    INPUT_LINE("IN"),
    OUTPUT_LINE("OUT"),
    PREFIX_MARK(':'),
    RESET_MARK('*'),
    COMMENT_MARK('/'),
}