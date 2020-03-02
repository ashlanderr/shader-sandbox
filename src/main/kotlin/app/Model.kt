package app

import kotlinx.collections.immutable.*

sealed class Cmd {
  class LocalStorageGet(val key: String, val msg: (value: String?) -> Msg) : Cmd()
  class LocalStoragePut(val key: String, val value: String) : Cmd()
}

sealed class Msg {
  class SetLines(val lines: PersistentList<JointLine>) : Msg()
  class SetSearch(val value: String) : Msg()

  // move
  class MoveViewport(val point: WorldPoint) : Msg()
  class ScaleViewport(val factor: Double, val center: WorldPoint) : Msg()
  class TranslateViewport(val offset: WorldPoint) : Msg()
  class MoveNode(val node: NodeId, val point: ViewPoint) : Msg()
  class MoveSourceJoint(val node: NodeId, val output: OutputId, val point: ViewPoint) : Msg()
  class DoMove(val point: WorldPoint) : Msg()
  class StopOnInput(val node: NodeId, val input: InputId) : Msg()
  object StopOnViewport : Msg()

  // selection
  class SelectJoint(val joint: Joint) : Msg()
  class SelectNode(val node: NodeId) : Msg()
  object ClearSelection : Msg()
  object DeleteSelected : Msg()

  // data
  class ApplyPersistentModel(val model: PersistentModel) : Msg()
  class ParseData(val value: String?) : Msg()
  class PutNodeParam(val node: NodeId, val param: ParamId, val value: DataValue) : Msg()
  class AddNode(val type: NodeTypeId, val offset: WorldPoint) : Msg()
}

data class NodeTypeId(val category: String, val name: String) {
  override fun toString() = "$category/$name"
}

data class NodeId(val value: Int) {
  override fun toString() = value.toString()
}

data class ParamId(val value: String) {
  override fun toString() = value
}

data class InputId(val value: String) {
  override fun toString() = value
}

enum class OutputId {
  All,
  Red,
  Green,
  Blue,
  Alpha,
}

enum class DataType {
  Scalar,
  Color
}

sealed class DataValue {
  abstract val type: DataType
  abstract fun toCode(): String

  data class Scalar(val value: Float) : DataValue() {
    override val type = DataType.Scalar
    override fun toCode() = "float($value)"
  }

  data class Color(val r: Float, val g: Float, val b: Float, val a: Float) : DataValue() {
    override val type = DataType.Color
    override fun toCode() = "vec4(${r}, ${g}, ${b}, ${a})"
  }
}

data class Joint(
  val dest: Pair<NodeId, InputId>,
  val source: Pair<NodeId, OutputId>
) : Entity<Pair<NodeId, InputId>>

data class Node(
  val id: NodeId,
  val type: NodeTypeId,
  val offset: WorldPoint,
  val params: PersistentMap<ParamId, DataValue>
) : Entity<NodeId>

data class ParamType(
  val id: ParamId,
  val type: DataType
) : Entity<ParamId>

data class NodeType(
  val id: NodeTypeId,
  val hidden: Boolean,
  val params: EntityMap<ParamId, ParamType>,
  val inputs: PersistentSet<InputId>,
  val outputs: PersistentSet<OutputId>,
  val code: Map<List<DataType>, List<String>>,
  val globals: Set<String>
) : Entity<NodeTypeId>

data class JointLine(
  val joint: Joint,
  val line: Line
)

data class Line(
  val x1: Double,
  val y1: Double,
  val x2: Double,
  val y2: Double,
  val x3: Double,
  val y3: Double,
  val x4: Double,
  val y4: Double
)

sealed class ViewportMove {
  data class Viewport(
    val point: WorldPoint
  ) : ViewportMove()

  data class Node(
    val id: NodeId,
    val point: WorldPoint
  ) : ViewportMove()

  data class SourceJoint(
    val node: NodeId,
    val output: OutputId,
    val begin: WorldPoint,
    val end: WorldPoint
  ) : ViewportMove()
}

sealed class Selection {
  data class Joint(val value: app.Joint) : Selection()
  data class Node(val node: NodeId) : Selection()
}

typealias Joints = EntityMap<Pair<NodeId, InputId>, Joint>

data class Transform(
  val offset: WorldPoint,
  val scale: Double
)

data class Model(
  val types: EntityMap<NodeTypeId, NodeType>,
  val nodes: EntityMap<NodeId, Node>,
  val joints: Joints,
  val lines: PersistentList<JointLine>,
  val move: ViewportMove?,
  val compiled: CompiledNodes,
  val selection: Selection?,
  val search: String?,
  val transform: Transform
)

fun String.toNodeTypeId(): NodeTypeId? {
  val parts = this.split("/")
  if (parts.size != 2) return null
  return NodeTypeId(parts[0], parts[1])
}

val UNKNOWN_TYPE = NodeType(
  id = NodeTypeId("<Unknown>", "<Unknown>"),
  params = entityMapOf(),
  inputs = persistentSetOf(),
  outputs = persistentSetOf(),
  globals = emptySet(),
  code = emptyMap(),
  hidden = true
)

val UNKNOWN_NODE = Node(
  id = NodeId(-1),
  params = persistentMapOf(),
  offset = WorldPoint(0.0, 0.0),
  type = UNKNOWN_TYPE.id
)
