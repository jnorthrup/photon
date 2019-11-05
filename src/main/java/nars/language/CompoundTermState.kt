package nars.language

import nars.language.Util11.make
import nars.storage.BackingStore

abstract class CompoundTermState(n:String) : Term(name=n,complexity = 0.toInt(),constant = true)  {
    /**
     * Get the component list
     *
     * @return The component list
     */
      var components: List<Term>?=null

    companion object {
        /**
         * Try to replace a component in a compound at a given index by another one
         *
         * @param compound The compound
         * @param index    The location of replacement
         * @param t        The new component
         * @param memory   Reference to the memory
         * @return The new compound
         */
        fun setComponent(compound: CompoundTerm, index: Int, t: Term?, memory: BackingStore?): Term? {
            val list = compound.cloneComponents()
            list!!.removeAt(index)
            if (t != null) {
                if (compound.javaClass != t.javaClass) {
                    list.add(index, t)
                } else {
                    val list2 = (t as CompoundTerm).cloneComponents()
                    val bound = list2!!.size
                    for (i in 0 until bound) {
                        list!!.add(index + i, list2[i])
                    }
                }
            }
            return make(compound, list!!, memory!!)
        }
    }
}