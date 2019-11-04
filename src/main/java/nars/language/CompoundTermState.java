package nars.language;

import nars.io.Symbols;
import nars.storage.BackingStore;

import java.util.List;

public class CompoundTermState extends Term {
    private List<Term> components;
    private short complexity;
    private boolean isConstant = true;

    public CompoundTermState(String name) {
        super(name);
    }

    public CompoundTermState() {
    }

    /**
     * Check CompoundTerm operator symbol
     *
     * @param s The String to be checked
     * @return if the given String is an operator symbol
     */
    public static boolean isOperator(String s) {
        if (s.length() == 1) {
            return (List.of(Symbols.INTERSECTION_EXT_OPERATOR, Symbols.INTERSECTION_INT_OPERATOR, Symbols.DIFFERENCE_EXT_OPERATOR, Symbols.DIFFERENCE_INT_OPERATOR, Symbols.PRODUCT_OPERATOR, Symbols.IMAGE_EXT_OPERATOR, Symbols.IMAGE_INT_OPERATOR).contains(s));
        }
        if (s.length() == 2) {
            return (List.of(Symbols.NEGATION_OPERATOR, Symbols.DISJUNCTION_OPERATOR, Symbols.CONJUNCTION_OPERATOR).contains(s));
        }
        return false;
    }

    /**
     * Try to replace a component in a compound at a given index by another one
     *
     * @param compound The compound
     * @param index    The location of replacement
     * @param t        The new component
     * @param memory   Reference to the memory
     * @return The new compound
     */
    public static Term setComponent(CompoundTerm compound, int index, Term t, BackingStore memory) {
        var list = compound.cloneComponents();
        list.remove(index);
        if (t != null) {
            if (compound.getClass() != t.getClass()) {
                list.add(index, t);
            } else {
                var list2 = ((CompoundTerm) t).cloneComponents();
                int bound = list2.size();
                for (int i = 0; i < bound; i++) {
                    list.add(index + i, list2.get(i));
                }
            }
        }
        return Util11.make(compound, list, memory);
    }

    /**
     * Change the oldName of a CompoundTerm, called after variable substitution
     *
     * @param s The new oldName
     */
    protected void setName(String s) {
        name = s;
    }

    /**
     * syntactic complexity of the compound, the sum of those of its components
     * plus 1
     */ /**
     * report the term's syntactic complexity
     *
     * @return the complexity value
     */

    @Override
    public short getComplexity() {
        return complexity;
    }

    /**
     * Whether the term names a concept
     */ /**
     * check if the term contains free variable
     *
     * @return if the term is a constant
     */

    @Override
    public boolean isConstant() {
        return isConstant;
    }

    /**
     * Set the constant status
     *
     * @param isConstant
     */
    public void setConstant(boolean isConstant) {
        this.isConstant = isConstant;
    }

    /**
     * list of (direct) components
     */ /**
     * Get the component list
     *
     * @return The component list
     */
    public List<Term> getComponents() {
        return components;
    }

    public void setComponents(List<Term> components) {
        this.components = components;
    }

    public void setComplexity(short complexity) {
        this.complexity = complexity;
    }
}
