package nars.language

import nars.storage.Memory

/**
 *
 */
object Util11 {
    /**
     * Try to make a compound term from a template and a list of components
     *
     * @param compound   The template
     * @param components The components
     * @param memory     Reference to the memory
     * @return A compound term or null
     */
    @JvmStatic
    fun make(compound: CompoundTerm, components: List<Term > , memory: Memory ): Term {
        return if (compound is ImageExt) {
            ImageExt.make(components , compound.relationIndex, memory)
        } else if (compound is ImageInt) {
            ImageInt.make(components, compound.relationIndex, memory)
        } else {
            CompoundTerm.make(compound.operator(), components, memory)
        }
    }
}