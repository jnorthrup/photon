package nars.entity;

/**
 * Created by jim on 1/2/2016.
 */
public interface Item {
    void setBudget(BudgetValueAtomic budget);

    String getKey();

    BudgetValueAtomic getBudget();

    float getPriority();

    void setPriority(float v);

    void incPriority(float v);

    void decPriority(float v);

    float getDurability();

    void setDurability(float v);

    void incDurability(float v);

    void decDurability(float v);

    float getQuality();

    void setQuality(float v);
}
