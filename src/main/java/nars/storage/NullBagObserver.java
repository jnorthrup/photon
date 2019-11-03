package nars.storage;

import nars.entity.AbstractItem;

/**
 * a {@link BagObserver} that does nothing (null design pattern)
 */
public class NullBagObserver<BagType extends AbstractItem> implements BagObserver<BagType> {

    @Override
    public void setTitle() {
    }


    @Override
    public void setBag(Bag<BagType> concepts) {
    }


    @Override
    public void post(String str) {
    }


    @Override
    public void refresh(String string) {
    }


    @Override
    public void stop() {
    }
}