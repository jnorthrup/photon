package nars.storage

import nars.entity.ItemIdentity

/**
 * a [BagObserver] that does nothing (null design pattern)
 */
class NullBagObserver<BagType : ItemIdentity?> : BagObserver<BagType> {
    override fun setTitle() {}
    override fun setBag(concepts: Bag<BagType>?) {}
    override fun post(str: String?) {}
    override fun refresh(string: String?) {}
    override fun stop() {}
}