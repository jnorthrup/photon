package nars.entity

/**
 *
 */
interface Item {
    /**
     *
     */
    val key: String?
    /**
     *
     */
    fun toStringBrief(): String?
    /**
     *
     */
    fun toStringLong(): String?
}