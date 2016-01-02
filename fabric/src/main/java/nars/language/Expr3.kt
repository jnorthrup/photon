package nars.language

import nars.data.TermStruct
import java.util.stream.IntStream

/**
 * Created by jim on 1/11/16.
 */
object Expr3 {


    private val paralel: Boolean = true

    /**
     * To recursively find a substitution that can unify two Terms without changing them

     * @param type  The type of Variable to be substituted
     * *
     * @param term1 The first Term to be unified
     * *
     * @param term2 The second Term to be unified
     * *z
     * @param map1
     * *
     * @param map2  @return The substitution that unifies the two Terms
     */
    public fun findSubstitute(type: Char, term1: TermStruct, term2: TermStruct, map1: MutableMap<Term?, Term?>, map2: MutableMap<Term?, Term?>): Boolean = term1 is Variable
                && term1.type == type
                && (map1[term1]) == term2
                && null != map1.put(term1, term2) //  ---- OR -----
                || !(term1 is Variable && term1.type == type) && (term2 is Variable
                && term2.type == type
                && map2[term2] == term1
                && null != map2.put(term2, term1) //  ---- OR -----
                || !(term2 is Variable && term2.type == type) && (term1 !is CompoundTerm || term1.javaClass != term2.javaClass
                && term1 !is Variable
                && term2 !is Variable
                && term1 == term2 || !(term1 !is CompoundTerm || term1.javaClass != term2.javaClass)
                && term1.size() == (term2 as CompoundTerm).size()
                && IntStream.range(0, term1.size()).parallel().allMatch({ findSubstitute(type, term1.componentAt(it), term2.componentAt(it), map1, map2) }))); };