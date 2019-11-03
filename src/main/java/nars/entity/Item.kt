package nars.entity

interface Item {
    val key: String?
    val budget: BudgetValue?
    fun toStringBrief(): String?
    fun toStringLong(): String?
}