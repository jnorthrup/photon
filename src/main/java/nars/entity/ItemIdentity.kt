package nars.entity

/**
 *
 */
abstract class ItemIdentity (
        /**
         *
         */
        val budget:BudgetValue=BudgetValue()): Item, BudgetTriple by budget  {

    /**
     *
     */
    abstract override val key: String

    /**
     * Merge with another ImmutableItemIdentity with identical key
     *
     * @param that The ImmutableItemIdentity to be merged
     */
    override fun merge(that: BudgetTriple?) {
        budget.merge(that)
    }

    /**
     * Return a String representation of the ImmutableItemIdentity
     *
     * @return The String representation of the full content
     */

    override fun toString(): String {
        return "$budget $key"
    }

    /**
     * Return a String representation of the ImmutableItemIdentity after simplification
     *
     * @return A simplified String representation of the content
     */

    override fun toStringBrief(): String? {
        return budget.toStringBrief() + " " + key
    }

    /**
     *
     */
    override fun toStringLong(): String? {
        return toString()
    }
}