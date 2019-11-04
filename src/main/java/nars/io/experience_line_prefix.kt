package nars.io

/**
 *
 */
  enum class experience_line_prefix (val sym:Any) {
    INPUT_LINE("IN"),
    OUTPUT_LINE("OUT"),
    PREFIX_MARK(':'),
    RESET_MARK('*'),
    COMMENT_MARK('/'),
}