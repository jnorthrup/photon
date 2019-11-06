package nars.entity

/**
 *
 */
interface BudgetTriple:Iterable<Float> {
    /**
     *
     */
    var priority: Float

    /**
     *
     */
    fun incPriority(v: Float)

    /**
     *
     */
    fun decPriority(v: Float)

    /**
     *
     */
    var durability: Float

    /**
     *
     */
    fun incDurability(v: Float)

    /**
     *
     */
    fun decDurability(v: Float)

    /**
     *
     */
    var quality: Float

    /**
     *
     */
    fun merge(that: BudgetTriple ) :BudgetTriple

}
