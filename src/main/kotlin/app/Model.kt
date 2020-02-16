package app

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentSet

sealed class Cmd {}

sealed class Msg {
  class SetLines(val lines: PersistentList<Line>) : Msg()
  class StartMove(val node: NodeId, val x: Int, val y: Int) : Msg()
  class DoMove(val x: Int, val y: Int) : Msg()
  object StopMove : Msg()
}

data class NodeTypeId(val value: String) {
  override fun toString() = value
}

data class NodeId(val value: String) {
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

data class Point(
  val x: Double,
  val y: Double
)

data class Joint(
  val destInput: InputId,
  val sourceNode: NodeId,
  val sourceOutput: OutputId
) : Entity<InputId>

data class Node(
  val id: NodeId,
  val type: NodeTypeId,
  val offset: Point,
  val joints: EntityMap<InputId, Joint>
) : Entity<NodeId>

data class NodeType(
  val id: NodeTypeId,
  val name: String,
  val inputs: PersistentSet<InputId>,
  val outputs: PersistentSet<OutputId>
) : Entity<NodeTypeId>

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

data class NodeMove(
  val id: NodeId,
  val x: Int,
  val y: Int
)

data class Model(
  val types: EntityMap<NodeTypeId, NodeType>,
  val nodes: EntityMap<NodeId, Node>,
  val lines: PersistentList<Line>,
  val move: NodeMove?
)
