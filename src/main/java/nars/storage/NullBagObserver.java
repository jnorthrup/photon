package nars.storage;

import nars.entity.AbstractItem;

/**
 * a {@link BagObserver} that does nothing (null design pattern)
 */
public class NullBagObserver<BagType extends AbstractItem> implements BagObserver<BagType> {

    public void setTitle() {
    }


    public void setBag(Bag<BagType> concepts) {
    }


    public void post(String str) {
    }


    public void refresh(String string) {
    }


    public void stop() {
    }
}