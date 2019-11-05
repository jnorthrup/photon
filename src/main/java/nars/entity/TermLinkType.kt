package nars.entity

enum class TermLinkType {
    /**
     * At C, point to C; TaskLink only
     */
   SELF,
    /**
     * At (&&, A, C), point to C
     */
    COMPONENT,
    /**
     * At C, point to (&&, A, C)
     */
    COMPOUND,
    /**
     * At <C --> A>, point to C
    </C> */
   COMPONENT_STATEMENT,
    /**
     * At C, point to <C --> A>
    </C> */
    COMPOUND_STATEMENT,
    /**
     * At <(&&, C, B) ==> A>, point to C
     */
    COMPONENT_CONDITION,
    /**
     * At C, point to <(&&, C, B) ==> A>
     */
    COMPOUND_CONDITION,
    /**
     * At C, point to <(*, C, B) --> A>; TaskLink only
     */
    TRANSFORM,
}
