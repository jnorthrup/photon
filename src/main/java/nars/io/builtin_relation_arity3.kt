package nars.io

/**
 *
 */
  enum class builtin_relation_arity3 (val sym:String) {
    INHERITANCE_RELATION("-->"),
    SIMILARITY_RELATION("<->"),
    INSTANCE_RELATION("{--"),
    PROPERTY_RELATION("--]"),
    INSTANCE_PROPERTY_RELATION("{-]"),
    IMPLICATION_RELATION("==>"),
    EQUIVALENCE_RELATION("<=>"),
}