package app

import io.akryl.ComponentScope
import io.akryl.component
import io.akryl.dom.css.properties.*
import io.akryl.dom.html.Div
import io.akryl.dom.html.Path
import io.akryl.dom.html.Svg
import io.akryl.redux.*
import io.akryl.useEffect
import kotlinx.collections.immutable.*
import org.w3c.dom.HTMLElement
import react_dom.ReactDom
import redux.StoreEnhancer
import kotlin.browser.document
import kotlin.math.max

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
  val sourceNode: NodeId,
  val sourceOutput: OutputId,
  val destInput: InputId
)

data class Node(
  val id: NodeId,
  val type: NodeTypeId,
  val offset: Point,
  val joints: PersistentMap<InputId, Joint>
)

data class NodeType(
  val id: NodeTypeId,
  val name: String,
  val inputs: PersistentList<InputId>,
  val outputs: PersistentSet<OutputId>
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

data class NodeMove(
  val id: NodeId,
  val x: Int,
  val y: Int
)

data class Model(
  val types: PersistentMap<NodeTypeId, NodeType>,
  val nodes: PersistentMap<NodeId, Node>,
  val lines: PersistentList<Line>,
  val move: NodeMove?
)

val unknownType = NodeType(
  id = NodeTypeId("unknown"),
  inputs = persistentListOf(),
  outputs = persistentSetOf(),
  name = "<Unknown>"
)

val store = createStore<Model, Msg, Cmd>(
  init = Pair(
    Model(
      types = persistentMapOf(
        // constants
        NodeTypeId("const") to NodeType(
          id = NodeTypeId("const"),
          name = "Constant",
          inputs = persistentListOf(),
          outputs = persistentSetOf(
            OutputId.All
          )
        ),
        NodeTypeId("color") to NodeType(
          id = NodeTypeId("color"),
          name = "Color",
          inputs = persistentListOf(),
          outputs = persistentSetOf(
            OutputId.All
          )
        ),
        // globals
        NodeTypeId("time") to NodeType(
          id = NodeTypeId("time"),
          name = "Time",
          inputs = persistentListOf(),
          outputs = persistentSetOf(
            OutputId.All
          )
        ),
        NodeTypeId("result") to NodeType(
          id = NodeTypeId("result"),
          name = "Result",
          inputs = persistentListOf(
            InputId("Color")
          ),
          outputs = persistentSetOf()
        ),
        // operations
        NodeTypeId("add") to NodeType(
          id = NodeTypeId("add"),
          name = "Add",
          inputs = persistentListOf(
            InputId("A"),
            InputId("B")
          ),
          outputs = persistentSetOf(
            OutputId.All
          )
        ),
        NodeTypeId("sub") to NodeType(
          id = NodeTypeId("sub"),
          name = "Subtract",
          inputs = persistentListOf(
            InputId("A"),
            InputId("B")
          ),
          outputs = persistentSetOf(
            OutputId.All
          )
        ),
        NodeTypeId("mul") to NodeType(
          id = NodeTypeId("mul"),
          name = "Multiply",
          inputs = persistentListOf(
            InputId("A"),
            InputId("B")
          ),
          outputs = persistentSetOf(
            OutputId.All
          )
        ),
        // trigonometry
        NodeTypeId("sin") to NodeType(
          id = NodeTypeId("sin"),
          name = "Sin",
          inputs = persistentListOf(
            InputId("X")
          ),
          outputs = persistentSetOf(
            OutputId.All
          )
        )
      ),
      nodes = persistentMapOf(
        NodeId("1") to Node(
          id = NodeId("1"),
          type = NodeTypeId("color"),
          offset = Point(600.0, 0.0),
          joints = persistentMapOf()
        ),
        NodeId("3") to Node(
          id = NodeId("3"),
          type = NodeTypeId("time"),
          offset = Point(0.0, 200.0),
          joints = persistentMapOf()
        ),
        NodeId("4") to Node(
          id = NodeId("4"),
          type = NodeTypeId("sin"),
          offset = Point(200.0, 200.0),
          joints = persistentMapOf(
            InputId("X") to Joint(
              sourceNode = NodeId("3"),
              sourceOutput = OutputId.All,
              destInput = InputId("X")
            )
          )
        ),
        NodeId("5") to Node(
          id = NodeId("5"),
          type = NodeTypeId("const"),
          offset = Point(200.0, 300.0),
          joints = persistentMapOf()
        ),
        NodeId("6") to Node(
          id = NodeId("6"),
          type = NodeTypeId("mul"),
          offset = Point(400.0, 200.0),
          joints = persistentMapOf(
            InputId("A") to Joint(
              sourceNode = NodeId("4"),
              sourceOutput = OutputId.All,
              destInput = InputId("A")
            ),
            InputId("B") to Joint(
              sourceNode = NodeId("5"),
              sourceOutput = OutputId.All,
              destInput = InputId("B")
            )
          )
        ),
        NodeId("7") to Node(
          id = NodeId("7"),
          type = NodeTypeId("const"),
          offset = Point(400.0, 300.0),
          joints = persistentMapOf()
        ),
        NodeId("8") to Node(
          id = NodeId("8"),
          type = NodeTypeId("add"),
          offset = Point(600.0, 200.0),
          joints = persistentMapOf(
            InputId("A") to Joint(
              sourceNode = NodeId("6"),
              sourceOutput = OutputId.All,
              destInput = InputId("A")
            ),
            InputId("B") to Joint(
              sourceNode = NodeId("7"),
              sourceOutput = OutputId.All,
              destInput = InputId("B")
            )
          )
        ),
        NodeId("9") to Node(
          id = NodeId("9"),
          type = NodeTypeId("mul"),
          offset = Point(800.0, 0.0),
          joints = persistentMapOf(
            InputId("A") to Joint(
              sourceNode = NodeId("1"),
              sourceOutput = OutputId.All,
              destInput = InputId("A")
            ),
            InputId("B") to Joint(
              sourceNode = NodeId("8"),
              sourceOutput = OutputId.All,
              destInput = InputId("B")
            )
          )
        ),
        NodeId("2") to Node(
          id = NodeId("2"),
          type = NodeTypeId("color"),
          offset = Point(800.0, 400.0),
          joints = persistentMapOf()
        ),
        NodeId("10") to Node(
          id = NodeId("10"),
          type = NodeTypeId("const"),
          offset = Point(600.0, 400.0),
          joints = persistentMapOf()
        ),
        NodeId("11") to Node(
          id = NodeId("11"),
          type = NodeTypeId("sub"),
          offset = Point(800.0, 200.0),
          joints = persistentMapOf(
            InputId("A") to Joint(
              sourceNode = NodeId("8"),
              sourceOutput = OutputId.All,
              destInput = InputId("A")
            ),
            InputId("B") to Joint(
              sourceNode = NodeId("10"),
              sourceOutput = OutputId.All,
              destInput = InputId("B")
            )
          )
        ),
        NodeId("12") to Node(
          id = NodeId("12"),
          type = NodeTypeId("mul"),
          offset = Point(1000.0, 200.0),
          joints = persistentMapOf(
            InputId("A") to Joint(
              sourceNode = NodeId("11"),
              sourceOutput = OutputId.All,
              destInput = InputId("A")
            ),
            InputId("B") to Joint(
              sourceNode = NodeId("2"),
              sourceOutput = OutputId.All,
              destInput = InputId("B")
            )
          )
        ),
        NodeId("13") to Node(
          id = NodeId("13"),
          type = NodeTypeId("add"),
          offset = Point(1200.0, 100.0),
          joints = persistentMapOf(
            InputId("A") to Joint(
              sourceNode = NodeId("9"),
              sourceOutput = OutputId.All,
              destInput = InputId("A")
            ),
            InputId("B") to Joint(
              sourceNode = NodeId("12"),
              sourceOutput = OutputId.All,
              destInput = InputId("B")
            )
          )
        ),
        NodeId("14") to Node(
          id = NodeId("14"),
          type = NodeTypeId("result"),
          offset = Point(1400.0, 100.0),
          joints = persistentMapOf(
            InputId("Color") to Joint(
              sourceNode = NodeId("13"),
              sourceOutput = OutputId.All,
              destInput = InputId("Color")
            )
          )
        )
      ),
      lines = persistentListOf(),
      move = null
    ),
    null
  ),
  update = { model, msg: Msg ->
    when (msg) {
      is Msg.SetLines -> {
        Pair(
          model.copy(lines = msg.lines),
          null
        )
      }
      is Msg.StartMove -> {
        Pair(
          model.copy(move = NodeMove(msg.node, msg.x, msg.y)),
          null
        )
      }
      is Msg.StopMove -> {
        Pair(
          model.copy(move = null),
          null
        )
      }
      is Msg.DoMove -> {
        doMove(model, msg)
      }
    }
  },
  execute = { emptyList() },
  enhancer = js("window.__REDUX_DEVTOOLS_EXTENSION__ && window.__REDUX_DEVTOOLS_EXTENSION__()")
    .unsafeCast<StoreEnhancer<Model, MsgAction<Msg>>>()
)

private fun doMove(model: Model, msg: Msg.DoMove): Pair<Model, Nothing?> {
  val move = model.move ?: return Pair(model, null)
  val dx = msg.x - move.x
  val dy = msg.y - move.y

  val node = model.nodes[move.id] ?: return Pair(model, null)
  val newOffset = Point(node.offset.x + dx, node.offset.y + dy)
  val newNode = node.copy(offset = newOffset)
  val newNodes = model.nodes.put(newNode.id, newNode)

  val newMove = move.copy(x = msg.x, y = msg.y)

  return Pair(
    model.copy(nodes = newNodes, move = newMove),
    null
  )
}

fun input(node: NodeId, input: InputId) = component {
  Div(
    css = listOf(
      display.flex(),
      alignItems.center(),
      height(1.em),
      cursor.pointer()
    ),
    children = listOf(
      Div(
        id = "node-$node-input-$input",
        css = listOf(
          width(8.px),
          height(8.px),
          borderRadius(100.pct),
          backgroundColor(0xFFFFFF),
          marginRight(4.px)
        )
      ),
      Div(text = input.value)
    )
  )
}

fun output(node: NodeId, output: OutputId) = component {
  val color = when (output) {
    OutputId.All, OutputId.Alpha -> Color.white
    OutputId.Red -> Color.red
    OutputId.Green -> Color.lime
    OutputId.Blue -> Color.blue
  }

  Div(
    css = listOf(
      display.flex(),
      alignItems.center(),
      height(1.em),
      cursor.pointer()
    ),
    children = listOf(
      Div(
        id = "node-$node-output-$output",
        css = listOf(
          width(8.px),
          height(8.px),
          borderRadius(100.pct),
          backgroundColor(color),
          marginLeft(16.px)
        )
      )
    )
  )
}

fun node(model: Model, node: Node) = component {
  val type = model.types[node.type] ?: unknownType

  Div(
    css = listOf(
      display.flex(),
      flexDirection.column(),
      position.absolute(),
      backgroundColor(0xBBBBBB),
      left(0.px),
      top(0.px),
      minWidth(150.px),
      userSelect("none")
    ),
    style = listOf(
      transform.translate(node.offset.x.px, node.offset.y.px)
    ),
    children = listOf(
      nodeHeader(node.id, type),
      nodeBody(type, node)
    )
  )
}

private fun nodeHeader(node: NodeId, type: NodeType) = component {
  val dispatch = useDispatch<Msg>()

  Div(
    css = listOf(
      padding(vertical = 8.px, horizontal = 4.px),
      textAlign.center(),
      cursor.move()
    ),
    text = type.name,
    onMouseDown = { dispatch(Msg.StartMove(node, it.clientX, it.clientY)) },
    onMouseUp = { dispatch(Msg.StopMove) }
  )
}

private fun nodeBody(type: NodeType, node: Node) = component {
  Div(
    css = listOf(
      display.flex(),
      backgroundColor(0x505050),
      color(0xFFFFFF),
      justifyContent("space-between")
    ),
    children = listOf(
      Div(
        css = listOf(
          display.flex(),
          flexDirection.column(),
          padding(vertical = 4.px, horizontal = 4.px)
        ),
        children = type.inputs.map { input(node.id, it) }
      ),
      Div(
        css = listOf(
          display.flex(),
          flexDirection.column(),
          padding(vertical = 4.px, horizontal = 4.px)
        ),
        children = type.outputs.map { output(node.id, it) }
      )
    )
  )
}

fun line(line: Line) = component {
  Path(
    css = listOf(
      stroke("white"),
      strokeWidth("2"),
      fill("transparent")
    ),
    d = "M${line.x1} ${line.y1} C${line.x2} ${line.y2}, ${line.x3} ${line.y3}, ${line.x4} ${line.y4}"
  )
}

fun viewport() = component {
  val model = useSelector<Model>()
  val dispatch = useDispatch<Msg>()
  useBuildLines(model.nodes)

  Div(
    css = listOf(
      width(100.pct),
      height(100.pct),
      backgroundColor(0x252525)
    ),
    child = Div(
      Svg(
        css = listOf(
          position.absolute(),
          left(0.px),
          top(0.px),
          width(100.pct),
          height(100.pct)
        ),
        children = model.lines.map { line(it) }
      ),
      Div(
        css = listOf(
          position.absolute(),
          left(0.px),
          top(0.px)
        ),
        children = model.nodes.map { node(model, it.value) }
      )
    ),
    onMouseMove = {
      if (model.move != null)
        dispatch(Msg.DoMove(it.clientX, it.clientY))
    }
  )
}

fun ComponentScope.useBuildLines(nodes: PersistentMap<NodeId, Node>) {
  val dispatch = useDispatch<Msg>()

  useEffect(listOf(nodes)) {
    val lines = ArrayList<Line>()

    for ((_, node) in nodes) {
      for ((_, joint) in node.joints) {
        val inputId = "node-${node.id}-input-${joint.destInput}"
        val outputId = "node-${joint.sourceNode}-output-${joint.sourceOutput}"
        val input = document.getElementById(inputId) as? HTMLElement ?: continue
        val output = document.getElementById(outputId) as? HTMLElement ?: continue
        val inputBox = input.getBoundingClientRect()
        val outputBox = output.getBoundingClientRect()

        val x1 = outputBox.left + outputBox.width / 2
        val y1 = outputBox.top + outputBox.height / 2
        val x4 = inputBox.left + inputBox.width / 2
        val y4 = inputBox.top + inputBox.height / 2
        val w = max(x4 - x1, 200.0)
        val x2 = x1 + w / 2
        val x3 = x4 - w / 2

        lines.add(Line(x1, y1, x2, y1, x3, y4, x4, y4))
      }
    }

    dispatch(Msg.SetLines(lines.toPersistentList()))
  }
}

fun app() = component {
  store.provider(
    children = listOf(
      viewport()
    )
  )
}

fun main() {
  ReactDom.render(app(), document.getElementById("app"))
}
