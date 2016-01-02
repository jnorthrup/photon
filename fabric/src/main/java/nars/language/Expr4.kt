package nars.language

/**
 * kotlin hates .parralellStream() so these live here.
 */

object Expr4 {
    /**
     * Recursively check if a compound contains a term

     * @param immutable
     * *            this pointer
     * *
     * @param ephemeral
     * *            The term to be searched for
     * *
     * @return Whether the two have the same content
     */
    /*tailrec*/ fun hasOrIsTerm(immutable: Term, ephemeral: Term): Boolean {
        return immutable == ephemeral || immutable is CompoundTerm && immutable.components.any { immutable1 -> hasOrIsTerm(immutable1, ephemeral) }
    }
}
