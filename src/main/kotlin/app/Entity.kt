package app

import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.toPersistentMap

interface Entity<Id> {
  operator fun component1(): Id
}

class EntityMap<Id, E : Entity<Id>> private constructor (private val map: PersistentMap<Id, E>) : Iterable<Map.Entry<Id, E>> {
  constructor (vararg items: E) : this(items.associateBy { it.component1() }.toPersistentMap())

  override fun iterator() = map.iterator()
  operator fun get(id: Id) = map[id]
  fun put(entity: E) = EntityMap(map.put(entity.component1(), entity))
}

fun <Id, E: Entity<Id>> entityMapOf(vararg items: E) = EntityMap(*items)