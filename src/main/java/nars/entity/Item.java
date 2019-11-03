package nars.entity;

public interface Item {
    String getKey();

    BudgetValue getBudget();

    String toStringBrief();

    String toStringLong();
}
