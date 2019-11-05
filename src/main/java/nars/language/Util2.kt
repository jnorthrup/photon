package nars.language

import nars.io.compound_delim.COMPOUND_TERM_CLOSER
import nars.io.compound_delim.COMPOUND_TERM_OPENER
import nars.io.compound_oper_arity1.*
import nars.io.compound_oper_arity2.*
import nars.io.special_operator.ARGUMENT_SEPARATOR
import nars.io.special_operator.IMAGE_PLACE_HOLDER
import nars.language.Util11.make
import nars.storage.BackingStore
import java.util.*

object Util2 {
    private val operators = arrayOf(INTERSECTION_EXT_OPERATOR.sym, INTERSECTION_INT_OPERATOR.sym, DIFFERENCE_EXT_OPERATOR.sym,
            DIFFERENCE_INT_OPERATOR.sym, PRODUCT_OPERATOR.sym, IMAGE_EXT_OPERATOR.sym, IMAGE_INT_OPERATOR.sym,
            NEGATION_OPERATOR.sym, DISJUNCTION_OPERATOR.sym, CONJUNCTION_OPERATOR.sym)

    /**
     * Check CompoundTerm operator symbol
     *
     * @param s The String to be checked
     * @return if the given String is an operator symbol
     */
    @JvmStatic
    fun isOperator(s: Any): Boolean {

        return s in operators

    }

    /**
     * Try to make a compound term from an operator and a list of components
     *
     *
     * Called from StringParser
     *
     * @param op     Term operator
     * @param arg    Component list
     * @param memory Reference to the memory
     * @return A compound term or null
     */
    @JvmStatic
    fun make(op: String?, arg: List<Term?>?, memory: BackingStore?): Term? {
        return getTerm(op!!, arg as List<Term>, memory!!)
    }

    /**
     * build a component list from two terms
     *
     * @param t1 the first component
     * @param t2 the second component
     * @return the component list
     */
    @JvmStatic
    internal fun argumentsToList(t1: Term, t2: Term): ArrayList<Term> {
        val list = ArrayList<Term>(2)
        list.add(t1)
        list.add(t2)
        return list
    }

    /**
     * default method to make the oldName of a compound term from given fields
     *
     * @param op  the term operator
     * @param arg the list of components
     * @return the oldName of the term
     * TODO: whoa blinding side effects!
     * */
    @JvmStatic
      fun makeCompoundName(op: Any, arg: Iterable<Term>): String {
        arg.filter { it is CompoundTerm }.forEach { (it as CompoundTermState).apply { setName((this as CompoundTerm).makeName()) } }
        return arg.map(Term::getName).joinToString(prefix = "${COMPOUND_TERM_OPENER.sym}$op", separator = ARGUMENT_SEPARATOR.sym.toString(), postfix = COMPOUND_TERM_CLOSER.sym.toString())

        //PRESERVED FOR VERIFICATION
//        var name = "${COMPOUND_TERM_OPENER.sym}$op"
//        arg.forEach { t ->
//            name+=(ARGUMENT_SEPARATOR.sym)
//            if (t is CompoundTerm) (t as CompoundTermState).setName(t.makeName())
//            name+=(t.getName())
//        }
//        name+=(COMPOUND_TERM_CLOSER.sym)
//        return name
    }

    /**
     * make the oldName of an ExtensionSet or IntensionSet
     *
     * @param opener the set opener
     * @param closer the set closer
     * @param arg    the list of components
     * @return the oldName of the term
     */
    @JvmStatic
    internal fun makeSetName(opener: Char, arg: Collection<Term>, closer: Char): String {
        val joiner = StringJoiner(ARGUMENT_SEPARATOR.sym.toString(), opener.toString(), closer.toString())
        for (term in arg) {
            val termName = term.getName()
            joiner.add(termName)
        }
        return joiner.toString()
    }

    /**
     * default method to make the oldName of an image term from given fields
     *
     * @param op            the term operator
     * @param arg           the list of components
     * @param relationIndex the location of the place holder
     * @return the oldName of the term
     */
    @JvmStatic
    internal fun makeImageName(op: String?, arg: List<Term>, relationIndex: Int): String {
        val name = StringBuilder()
        name.append(COMPOUND_TERM_OPENER.sym)
        name.append(op)
        name.append(ARGUMENT_SEPARATOR.sym)
        name.append(arg[relationIndex].getName())
        val bound = arg.size
        for (i in 0 until bound) {
            name.append(ARGUMENT_SEPARATOR.sym)
            if (i == relationIndex) {
                name.append(IMAGE_PLACE_HOLDER.sym)
            } else {
                name.append(arg[i].getName())
            }
        }
        name.append(COMPOUND_TERM_CLOSER.sym)
        return name.toString()
    }

    /**
     * Deep clone an array list of terms
     *
     * @param original The original component list
     * @return an identical and separate copy of the list
     */
    @JvmStatic
    fun cloneList(original: Collection<Term>?): List<Term>? = original?.map(::Term)

    /**
     * Try to remove a component from a compound
     *
     * @param t1     The compound
     * @param t2     The component
     * @param memory Reference to the memory
     * @return The new compound
     */
    @JvmStatic
    fun reduceComponents(t1: CompoundTerm, t2: Term, memory: BackingStore?): Term? {
        val success: Boolean
        val list = t1.cloneComponents()
        success = if (t1.javaClass == t2.javaClass) {
            list.removeAll((t2 as CompoundTermState).components)
        } else {
            list.remove(t2)
        }
        return if (success) make(t1, list, memory!!) else null
    }


}