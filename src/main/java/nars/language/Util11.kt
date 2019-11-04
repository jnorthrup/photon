package nars.language

import nars.storage.BackingStore

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
    fun make(compound: CompoundTerm, components: List<Term > , memory: BackingStore) = when (compound) {
        is ImageExt -> {
            ImageExt.make(components , compound.relationIndex, memory)
        }
        is ImageInt -> {
            ImageInt.make(components, compound.relationIndex, memory)
        }
        else -> {
            Util2.make(compound.operator(), components, memory)
        }
    }
}