package nars.entity

interface TermLinkConstants {
    companion object {
        /**
         * At C, point to C; TaskLink only
         */
        const val SELF: Int  = 0
        /**
         * At (&&, A, C), point to C
         */
        const val COMPONENT: Int  = 1
        /**
         * At C, point to (&&, A, C)
         */
        const val COMPOUND: Int  = 2
        /**
         * At <C --> A>, point to C
        </C> */
        const val COMPONENT_STATEMENT: Int  = 3
        /**
         * At C, point to <C --> A>
        </C> */
        const val COMPOUND_STATEMENT: Int  = 4
        /**
         * At <(&&, C, B) ==> A>, point to C
         */
        const val COMPONENT_CONDITION: Int  = 5
        /**
         * At C, point to <(&&, C, B) ==> A>
         */
        const val COMPOUND_CONDITION: Int  = 6
        /**
         * At C, point to <(*, C, B) --> A>; TaskLink only
         */
        const val TRANSFORM: Int  = 8
    }
}