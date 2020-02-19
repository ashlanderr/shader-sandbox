package app

import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.toPersistentMap

interface Entity<Id> {
  operator fun component1(): Id
}

class EntityMap<Id, E : Entity<Id>> private constructor (private val map: PersistentMap<Id, E>) : Iterable<Map.Entry<Id, E>> {
  constructor (vararg items: E) : this(items.associateBy { it.component1() }.toPersistentMap())

  val keys get() = map.keys
  val values get() = map.values
  override fun iterator() = map.iterator()
  operator fun get(id: Id) = map[id]
  operator fun contains(id: Id) = id in map
  fun put(entity: E) = EntityMap(map.put(entity.component1(), entity))
  fun remove(id: Id) = EntityMap(map.remove(id))
}

fun <Id, E: Entity<Id>> entityMapOf(vararg items: E) = EntityMap(*items)
