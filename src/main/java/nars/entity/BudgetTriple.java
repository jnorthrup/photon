package nars.entity;

public interface BudgetTriple {
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

    void merge(BudgetTriple that);

//    float summary();
//
//    boolean aboveThreshold();
}
