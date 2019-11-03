package nars.entity

import nars.storage.BagObserver

/**
 * Observer for a [Concept] object; similar to Observer design pattern, except that here we have a single observer;
 * NOTE: very similar to interface [nars.storage.BagObserver]
 */
interface EntityObserver {
    /**
     * Display the content of the concept
     *
     * @param str The text to be displayed
     */
    fun post(str: String?)

    /**
     * create a [BagObserver] of the right type (Factory design pattern)
     */
    fun createBagObserver(): BagObserver<*>?

    /**
     * Set the observed Concept
     *
     * @param showLinks unused : TODO : is this forgotten ?
     */
    fun startPlay(concept: Concept?, showLinks: Boolean)

    /**
     * put in non-showing state
     */
    fun stop()

    /**
     * Refresh display if in showing state
     */
    fun refresh(message: String?)
}