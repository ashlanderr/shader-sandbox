package app

import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.toPersistentMap
import kotlin.js.json

data class PersistentModel(
  val nodes: EntityMap<NodeId, Node>,
  val joints: Joints
) {
  companion object
}

// Deserializer

fun persistenceModelFromJson(json: dynamic) = PersistentModel(
  nodes = entityMapFromJson(json.nodes) { nodeFromJson(it) },
  joints = entityMapFromJson(json.joints) { jointFromJson(it) }
)

fun <Id, E : Entity<Id>> entityMapFromJson(map: dynamic, entityMapper: (dynamic) -> E): EntityMap<Id, E> {
  return entityMapOf(*(map as Array<dynamic>).map(entityMapper).toTypedArray())
}

fun nodeFromJson(json: dynamic) = Node(
  id = NodeId(json.id as Int),
  type = (json.type as String).toNodeTypeId() ?: UNKNOWN_TYPE.id,
  offset = offsetFromJson(json.offset),
  params = persistentMapFromJson(json.params, ::ParamId) { paramFromJson(it) }
)

fun paramFromJson(json: dynamic) = when (json.type as String) {
  "scalar" -> DataValue.Scalar(json.value as Float)
  "color" -> DataValue.Color(json.value[0] as Float, json.value[1] as Float, json.value[2] as Float, json.value[3] as Float)
  else -> error("Unknown DataValue type '${json.type}'")
}

fun <K, V> persistentMapFromJson(map: dynamic, keyMapper: (String) -> K, valueMapper: (dynamic) -> V): PersistentMap<K, V> {
  val result = HashMap<K, V>()
  for (k in js("Object.keys(map)")) {
    val key = keyMapper(k as String)
    val value = valueMapper(map[k])
    result[key] = value
  }
  return result.toPersistentMap()
}

fun offsetFromJson(offset: dynamic) = WorldPoint(
  x = offset.x as Double,
  y = offset.y as Double
)

fun jointFromJson(json: dynamic) = Joint(
  source = Pair(
    NodeId(json.sourceNode as Int),
    OutputId.valueOf(json.sourceOutput as String)
  ),
  dest = Pair(
    NodeId(json.destNode as Int),
    InputId(json.destInput as String)
  )
)

// Serializer

fun PersistentModel.toJson() = json(
  "nodes" to nodes.toJson { it.toJson() },
  "joints" to joints.toJson { it.toJson() }
)

@Suppress("UnsafeCastFromDynamic")
private fun <Id, E : Entity<Id>> EntityMap<Id, E>.toJson(entityMapper: (E) -> dynamic) =
  this.map { entityMapper(it.value) }
    .toTypedArray()

private fun Node.toJson() = json(
  "id" to id.value,
  "type" to type.toString(),
  "offset" to offset.toJson(),
  "params" to params.toJson(ParamId::toString) { it.toJson() }
)

private fun WorldPoint.toJson() = json(
  "x" to x,
  "y" to y
)

private fun <K, V> PersistentMap<K, V>.toJson(keyMapper: (K) -> String, valueMapper: (V) -> dynamic)
  = json(*this.map { Pair(keyMapper(it.key), valueMapper(it.value)) }.toTypedArray())

private fun DataValue.toJson() = when (this) {
  is DataValue.Scalar -> json(
    "type" to "scalar",
    "value" to value
  )
  is DataValue.Color -> json(
    "type" to "color",
    "value" to arrayOf(r, g, b, a)
  )
}

private fun Joint.toJson() = json(
  "sourceNode" to source.first.value,
  "sourceOutput" to source.second.toString(),
  "destNode" to dest.first.value,
  "destInput" to dest.second.toString()
)
