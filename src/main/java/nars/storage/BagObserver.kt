package nars.storage

import nars.entity.ImmutableItemIdentity
import nars.entity.ItemIdentity
import nars.entity.Task

/**
 * Bag Observer; similar to Observer design pattern, except that here we have a single observer
 */
interface BagObserver<BagType : ItemIdentity?> {
    /**
     * Set a name for this observer
     */
    fun setTitle()

    /**
     * Set the observed Bag
     */
    fun setBag(concepts: Bag<BagType>?)

    /**
     * Post given bag content
     *
     * @param str The text
     */
    fun post(str: String?)

    /**
     * Refresh display if in showing state
     */
    fun refresh(string: String?)

    /**
     * put in non-showing state
     */
    fun stop()
}