/*
 * ImmutableItemIdentity.java
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
package nars.entity

/**
 * An item is an object that can be put into a Bag,
 * to participate in the resource competition of the system.
 *
 *
 * It has a key and a budget. Cannot be cloned
 */
abstract class ImmutableItemIdentity
/**
 * Constructor with initial budget
 *
 * @param key    The key value
 * @param budget The initial budget
 */ @JvmOverloads protected constructor(
        /**
         *
         */
        override val key: String, budget: BudgetValue = BudgetValue()) : ItemIdentity(budget)