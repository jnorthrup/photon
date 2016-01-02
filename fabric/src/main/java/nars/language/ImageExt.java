/*
 * ImageExt.java
 *
 * Copyright (C) 2008  Pei Wang
 *
 * This file is part of Open-NARS.
 *
 * Open-NARS is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * Open-NARS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Open-NARS.  If not, see <http://www.gnu.org/licenses/>.
 */
package nars.language;

import nars.io.Symbols;
import nars.storage.Memory;

import java.util.ArrayList;
import java.util.List;

/**
 * An extension image.
 * <p>
 * B --> (/,P,A,_)) iff (*,A,B) --> P
 * <p>
 * Internally, it is actually (/,A,P)_1, with an index.
 */
public class ImageExt extends CompoundTerm {

    /**
     * The index of relation in the component list
     */
    private Integer relationIndex;

    /**
     * Constructor with partial values, called by make
     *
     * @param n     The name of the term
     * @param arg   The component list of the term
     * @param index The index of relation in the component list
     */
    private ImageExt(String n, List<Term> arg, Integer index) {
        super(n, arg);
        relationIndex = index;
    }

    /**
     * Constructor with full values, called by clone
     *
     * @param n          The name of the term
     * @param cs         Component list
     * @param complexity Syntactic complexity of the compound
     * @param index      The index of relation in the component list
     */
    private ImageExt(String n, List<Term> cs, boolean con, int complexity, Integer index) {
        super(n, cs, con, complexity);
        relationIndex = index;
    }

    /**
     * Try to make a new ImageExt. Called by StringParser.
     *
     * @param argList The list of components
     * @param memory  Reference to the memory
     * @return the Term generated from the arguments
     */
    public static Term make(List<Term> argList, Memory memory) {
        Term r = null;
        if (argList.size() >= 2) {
            Term relation = argList.get(0);
            List<Term> argument = new ArrayList<Term>();
            int index = 0;
            for (int j = 1; j < argList.size(); j++) {
                if (argList.get(j).getName().charAt(0) != Symbols.IMAGE_PLACE_HOLDER) {
                    argument.add(argList.get(j));
                } else {
                    index = j - 1;
                    argument.add(relation);
                }
            }
            r = make(argument, index, memory);
        }
        return r;
    }

    /**
     * Try to make an Image from a Product and a relation. Called by the inference rules.
     *
     * @param product  The product
     * @param relation The relation
     * @param index    The index of the place-holder
     * @return A compound generated or a term it reduced to
     */
    public static Term make(Product product, Term relation, Integer index, Memory memory) {
        Term r = null;

        e:
        {
            if (relation instanceof Product) {
                Product p2 = (Product) relation;
                if ((product.size() == 2) && (p2.size() == 2)) {
                    // (/,_,(*,a,b),b) is reduced to a
                    if ((index == 0) && product.componentAt(1).equals(p2.componentAt(1))) {
                        r = p2.componentAt(0);
                        break e;
                    } else // (/,(*,a,b),a,_) is reduced to b
                        if ((index == 1) && product.componentAt(0).equals(p2.componentAt(0))) {
                            r = p2.componentAt(1);
                            break e;
                        }
                }
            }
            List<Term> argument = product.cloneComponents();
            argument.set(index, relation);
            r = make(argument, index, memory);
        }
        return r;
    }

    /**
     * Try to make an Image from an existing Image and a component. Called by the inference rules.
     *
     * @param oldImage  The existing Image
     * @param component The component to be added into the component list
     * @param index     The index of the place-holder in the new Image
     * @return A compound generated or a term it reduced to
     */
    public static Term make(ImageExt oldImage, Term component, Integer index, Memory memory) {
        List<Term> argList = oldImage.cloneComponents();
        int oldIndex = oldImage.getRelationIndex();
        Term relation = argList.get(oldIndex);
        argList.set(oldIndex, component);
        argList.set(index, relation);
        return make(argList, index, memory);
    }

    /**
     * Try to make a new compound from a set of components. Called by the public make methods.
     *
     * @param argument The argument list
     * @param index    The index of the place-holder in the new Image
     * @return the Term generated from the arguments
     */
    public static Term make(List<Term> argument, Integer index, Memory memory) {
        String name = makeImageName(Symbols.IMAGE_EXT_OPERATOR, argument, index);
        Term t = Memory.nameToListedTerm(memory, name);
        return (t != null) ? t : new ImageExt(name, argument, index);
    }

    /**
     * Clone an object
     *
     * @return A new object, to be casted into an ImageExt
     */
    public Object clone() {
        return new ImageExt(name, (ArrayList<Term>) cloneList(getComponents()), isConstant(), getComplexity(), relationIndex);
    }

    /**
     * get the index of the relation in the component list
     *
     * @return the index of relation
     */
    public Integer getRelationIndex() {
        return relationIndex;
    }

    /**
     * Get the relation term in the Image
     *
     * @return The term representing a relation
     */
    public Term getRelation() {
        return getComponents().get(relationIndex);
    }

    /**
     * Get the other term in the Image
     *
     * @return The term related
     */
    public Term getTheOtherComponent() {
        return getComponents().size() != 2 ? null : (relationIndex == 0) ? getComponents().get(1) : getComponents().get(0);
    }

    /**
     * override the default in making the name of the current term from existing fields
     *
     * @return the name of the term
     * @param imageExt
     */
    public static String makeName(ImageExt imageExt) {
        return makeImageName(Symbols.IMAGE_EXT_OPERATOR, imageExt.getComponents(), imageExt.relationIndex);
    }

    /**
     * get the operator of the term.
     *
     * @return the operator of the term
     */
    public String operator() {
        return Symbols.IMAGE_EXT_OPERATOR;
    }
}
