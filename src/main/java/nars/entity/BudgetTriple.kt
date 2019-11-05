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
    fun merge(that: BudgetTriple?)

}

operator fun   <C:BudgetTriple> C.component1() = priority
operator fun   <C:BudgetTriple> C.component2() = durability
operator fun   <C:BudgetTriple> C.component3() = quality
operator fun   <C:BudgetTriple> C.iterator() = let { (a, b, c) -> arrayOf(a, b, c).iterator() }
operator fun   <C:BudgetTriple> C.invoke(a: Float, b: Float, c: Float) = this.apply { priority = a;durability = b;quality = c }
