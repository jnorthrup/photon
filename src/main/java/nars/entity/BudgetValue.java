package nars.entity;

import nars.io.Symbols;

/**
 * Created by jim on 1/2/2016.
 */
public interface BudgetValue extends Cloneable {
    /** The character that marks the two ends of a budget value */
    char MARK = Symbols.BUDGET_VALUE_MARK;
    /** The character that separates the factors in a budget value */
    char SEPARATOR = Symbols.VALUE_SEPARATOR;
}
