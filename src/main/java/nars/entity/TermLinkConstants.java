package nars.entity;

public interface TermLinkConstants {
    /**
     * At C, point to C; TaskLink only
     */
    short SELF = 0;
    /**
     * At (&&, A, C), point to C
     */
    short COMPONENT = 1;
    /**
     * At C, point to (&&, A, C)
     */
    short COMPOUND = 2;
    /**
     * At <C --> A>, point to C
     */
    short COMPONENT_STATEMENT = 3;
    /**
     * At C, point to <C --> A>
     */
    short COMPOUND_STATEMENT = 4;
    /**
     * At <(&&, C, B) ==> A>, point to C
     */
    short COMPONENT_CONDITION = 5;
    /**
     * At C, point to <(&&, C, B) ==> A>
     */
    short COMPOUND_CONDITION = 6;
    /**
     * At C, point to <(*, C, B) --> A>; TaskLink only
     */
    short TRANSFORM = 8;
}
