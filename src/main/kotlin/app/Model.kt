package app

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.PersistentSet

sealed class Cmd {}

sealed class Msg {
  class SetLines(val lines: PersistentList<JointLine>) : Msg()

  // move
  class MoveNode(val node: NodeId, val point: Point) : Msg()
  class MoveSourceJoint(val node: NodeId, val output: OutputId, val point: Point) : Msg()
  class DoMove(val point: Point) : Msg()
  class StopOnInput(val node: NodeId, val input: InputId) : Msg()
  object StopOnViewport : Msg()

  // selection
  class SelectJoint(val joint: Joint) : Msg()
  object ClearSelection : Msg()
  object DeleteSelected : Msg()

  // data
  class PutNodeParam(val node: NodeId, val param: ParamId, val value: DataValue) : Msg()
}

data class NodeTypeId(val category: String, val name: String) {
  override fun toString() = "$category/$name"
}

data class NodeId(val value: String) {
  override fun toString() = value
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

data class Point(
  val x: Double,
  val y: Double
)

data class Joint(
  val dest: Pair<NodeId, InputId>,
  val source: Pair<NodeId, OutputId>
) : Entity<Pair<NodeId, InputId>>

data class Node(
  val id: NodeId,
  val type: NodeTypeId,
  val offset: Point,
  val params: PersistentMap<ParamId, DataValue>
) : Entity<NodeId>

data class ParamType(
  val id: ParamId,
  val type: DataType
) : Entity<ParamId>

data class NodeType(
  val id: NodeTypeId,
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
  data class Node(
    val id: NodeId,
    val point: Point
  ) : ViewportMove()

  data class SourceJoint(
    val node: NodeId,
    val output: OutputId,
    val begin: Point,
    val end: Point
  ) : ViewportMove()
}

sealed class Selection {
  data class Joint(val value: app.Joint) : Selection()
}

typealias Joints = EntityMap<Pair<NodeId, InputId>, Joint>

data class Model(
  val types: EntityMap<NodeTypeId, NodeType>,
  val nodes: EntityMap<NodeId, Node>,
  val joints: Joints,
  val lines: PersistentList<JointLine>,
  val move: ViewportMove?,
  val compiled: CompiledShader?,
  val selection: Selection?
)
