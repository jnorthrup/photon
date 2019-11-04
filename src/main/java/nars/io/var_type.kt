package nars.io

/**
 *
 */
  enum class var_type (val sym:Any) {
    VAR_INDEPENDENT('$'),
    VAR_DEPENDENT('#'),
    VAR_QUERY('?'),
}