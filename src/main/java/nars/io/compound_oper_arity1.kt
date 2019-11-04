package nars.io

/**
 *
 */
  enum class compound_oper_arity1 (val sym:String) {
    INTERSECTION_EXT_OPERATOR("&"),
    INTERSECTION_INT_OPERATOR("|"),
    DIFFERENCE_EXT_OPERATOR("-"),
    DIFFERENCE_INT_OPERATOR("~"),
    PRODUCT_OPERATOR("*"),
    IMAGE_EXT_OPERATOR("/"),
    IMAGE_INT_OPERATOR("\\"),

}