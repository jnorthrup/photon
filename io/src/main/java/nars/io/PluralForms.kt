package nars.io

import nars.io.StringParser.Term
import nars.io.Symbols.*
import java.util.*

operator fun PluralForms.invoke(input: CharSequence) = invoke(input.first() to input.last())
operator fun PluralForms.invoke(input: Pair<Char, Char>) = confix == input
operator fun PluralForms.component1() = confix.first
operator fun PluralForms.component2() = confix.second
operator fun PluralForms.iterator() = let { (a, b) -> arrayOf(a, b) }.iterator()

fun inOperators(s: String) = s in PluralForms.operators
fun inRelations(s: String): Boolean = s in PluralForms.relations

enum class PluralForms(p1: Char, p2: Char, val confix: Pair<Char, Char> = (p1 to p2)) {
    COMPOUND_CONFIX(COMPOUND_TERM_OPENER, COMPOUND_TERM_CLOSER) {
        /**
         * Locate the first top-level separator in a CompoundTerm
         *
         * @param s     The String to be parsed
         * @param first The starting index
         * @return the index of the next seperator in a String
         */
        fun nextSeparator(s: String, first: Int): Int {
            var levelCounter = 0
            var i = 0
            loop@ while (i < s.length - 1) {
                when {
                    StringParser.isOpener(s, i) -> levelCounter++
                    StringParser.isCloser(s, i) -> levelCounter--
                    s[i] == ARGUMENT_SEPARATOR -> if (levelCounter == 0) break@loop
                }
                i++
            }
            return i
        }

        /**
         * Parse a String into the argument get of a CompoundTerm.
         *
         * @param s0 The String to be parsed
         * @return the arguments in an ArrayList
         * @throws nars.io.StringParser.InvalidInputException the String cannot be
         * parsed into an argument get
         */
        fun argSequence(s0: String): Sequence<Term?> {
            val s = s0.trim { it <= ' ' }
            var start = 0
            var end = 0
            return generateSequence {
                takeIf { (end < s.length - 1) }?.let {
                    end = StringParser.nextSeparator(s, start)
                    // recursive call
                    reifyTerm(s.substring(start, end)).also { start = end + 1 }
                }
            }
        }

        override fun reify(source: String): Term? {
            (source.drop(1).dropLast(1)).let { s ->
                val firstSeparator = s.indexOf(ARGUMENT_SEPARATOR)
                val op = s.substring(0, firstSeparator).trim { it <= ' ' }
                assert(inOperators(op)) { "unknown operator: $op" }
                val s = (s.substring(firstSeparator + 1) + ARGUMENT_SEPARATOR).trim { it <= ' ' }
                val list = ArrayList<Term?>()
                var start = 0
                var end = 0
                while (end < s.length - 1) {
                    end = nextSeparator(s, start)
                    // recursive call
                    val t = this.reify(s.substring(start, end))
                    list.add(t)
                    start = end + 1
                }
                assert(!list.isEmpty()) { "null argument" }
                val arg = list

            }
        }
    },
    STATEMENT_CONFIX(STATEMENT_OPENER, STATEMENT_CLOSER){
        override fun reify(input: String): Term? {
            val s = s0.trim { it <= ' ' }
            val i = StringParser.topRelation(s)
            assert(i >= 0) { "invalid statement" }
            val relation = s.substring(i, i + 3)
            val subject = StringParser.parseTerm(s.substring(0, i), memory)
            val predicate = StringParser.parseTerm(s.substring(i + 3), memory)
            return Statement.make(relation, subject, predicate, memory) ?: error("invalid statement")

        }
    },
    SET_EXT_CONFIX(SET_EXT_OPENER, SET_EXT_CLOSER){},
    SET_INT_CONFIX(SET_INT_OPENER, SET_INT_CLOSER){};

    abstract fun reify(input: String): Term?

    companion object {

        val operators by lazy {
            setOf(
                    INTERSECTION_EXT_OPERATOR,
                    INTERSECTION_INT_OPERATOR,
                    DIFFERENCE_EXT_OPERATOR,
                    DIFFERENCE_INT_OPERATOR,
                    PRODUCT_OPERATOR,
                    IMAGE_EXT_OPERATOR,
                    IMAGE_INT_OPERATOR,
                    NEGATION_OPERATOR,
                    DISJUNCTION_OPERATOR,
                    CONJUNCTION_OPERATOR)
        }
        val relations by lazy {
            setOf(INHERITANCE_RELATION,
                    SIMILARITY_RELATION,
                    INSTANCE_RELATION,
                    PROPERTY_RELATION,
                    INSTANCE_PROPERTY_RELATION,
                    IMPLICATION_RELATION,
                    EQUIVALENCE_RELATION)
        }


        fun find(input: String) =

                (input.first() to input.last()).let { p ->
                    values().find { it(p) }
                }


        fun partialMatchOnly(s: String, i: Int, matchTrue: List<Char>, matchFalse: Set<String>, pluralSize: Int) =

                s[i] in matchTrue && (i + pluralSize > s.length || !(s.substring(i, i + pluralSize) in matchFalse))

        val map1 by lazy { values().map { pluralForms -> pluralForms.confix.first } }
        val map2 by lazy { values().map { pluralForms -> pluralForms.confix.second } }

        tailrec fun reifyTerm(input: String): Term? {
            val pluralForm = find(input.replace("\\s+".toRegex(), ""))?.reify(input) ?: let { companion ->
                var result = -1 // need efficiency improvement
                var levelCounter = 0

                companion.let {
                    /*var i = 0*/
                    loop@ for (i in 0 until input.length - 3) { // don't need to check the last 3 characters
                        val substring = input.substring(i, i + 3)
                        when {
                            levelCounter == 0 && substring in this.relations -> {
                                result = i
                                break@loop
                            }
                            (i + 3 > input.length || !(substring in this.relations)) -> {
                                val c = input[i]
                                if (c in this.map1) levelCounter++
                                else if (c in this.map2) levelCounter--
                            }
                        }
                    }
                    val i = result
                    assert(i >= 0) { "invalid statement" }
                    val relation = input.substring(i, i + 3)
                    val subject = this.reifyTerm(input.substring(0, i))
                    val predicate = this.reifyTerm(input.substring(i + 3))
                }
            }
        }


    }

}