package app

import io.akryl.ComponentScope
import io.akryl.component
import io.akryl.dom.css.properties.*
import io.akryl.dom.html.Div
import io.akryl.dom.html.Path
import io.akryl.dom.html.Svg
import io.akryl.redux.*
import io.akryl.useEffect
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentList
import org.w3c.dom.HTMLElement
import react_dom.ReactDom
import redux.StoreEnhancer
import kotlin.browser.document
import kotlin.math.max

val unknownType = NodeType(
  id = NodeTypeId("unknown"),
  name = "<Unknown>",
  params = entityMapOf(),
  inputs = persistentSetOf(),
  outputs = persistentSetOf(),
  uniforms = emptySet(),
  code = emptyMap()
)

val store by lazy {
  createStore<Model, Msg, Cmd>(
    init = Pair(
      Model(
        types = NODE_TYPES,
        nodes = INITIAL_NODES,
        lines = persistentListOf(),
        move = null,
        compiled = compile(NODE_TYPES, INITIAL_NODES).orElse { null }
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
        is Msg.PutNodeParam -> {
          putNodeParam(model, msg)
        }
      }
    },
    execute = { emptyList() },
    enhancer = js("window.__REDUX_DEVTOOLS_EXTENSION__ && window.__REDUX_DEVTOOLS_EXTENSION__()")
      .unsafeCast<StoreEnhancer<Model, MsgAction<Msg>>>()
  )
}

private fun doMove(model: Model, msg: Msg.DoMove): Pair<Model, Nothing?> {
  val move = model.move ?: return Pair(model, null)
  val dx = msg.x - move.x
  val dy = msg.y - move.y

  val node = model.nodes[move.id] ?: return Pair(model, null)
  val newOffset = Point(node.offset.x + dx, node.offset.y + dy)
  val newNode = node.copy(offset = newOffset)
  val newNodes = model.nodes.put(newNode)

  val newMove = move.copy(x = msg.x, y = msg.y)

  return Pair(
    model.copy(nodes = newNodes, move = newMove),
    null
  )
}

private fun putNodeParam(model: Model, msg: Msg.PutNodeParam): Pair<Model, Cmd?> {
  val node = model.nodes[msg.node] ?: return Pair(model, null)
  val newModel = model.copy(
    nodes = model.nodes.put(
      node.copy(
        params = node.params.put(msg.param, msg.value)
      )
    )
  )
  return triggerCompile(newModel)
}

private fun triggerCompile(model: Model): Pair<Model, Cmd?> {
  // todo async compile
  val newModel = model.copy(
    compiled = compile(model.types, model.nodes).orElse { err ->
      console.error(err)
      model.compiled
    }
  )
  return Pair(newModel, null)
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
    id = "viewport",
    css = listOf(
      flex(1, 1, 100.pct),
      width(100.pct),
      height(100.pct),
      backgroundColor(0x252525),
      position.relative(),
      overflow.hidden()
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

fun ComponentScope.useBuildLines(nodes: EntityMap<NodeId, Node>) {
  val dispatch = useDispatch<Msg>()

  useEffect(listOf(nodes)) {
    val lines = ArrayList<Line>()

    val viewport = document.getElementById("viewport") as? HTMLElement ?: return@useEffect
    val viewportBox = viewport.getBoundingClientRect()

    for ((_, node) in nodes) {
      for ((_, joint) in node.joints) {
        val inputId = "node-${node.id}-input-${joint.destInput}"
        val outputId = "node-${joint.sourceNode}-output-${joint.sourceOutput}"
        val input = document.getElementById(inputId) as? HTMLElement ?: continue
        val output = document.getElementById(outputId) as? HTMLElement ?: continue
        val inputBox = input.getBoundingClientRect()
        val outputBox = output.getBoundingClientRect()

        val x1 = outputBox.left - viewportBox.left + outputBox.width / 2
        val y1 = outputBox.top - viewportBox.top + outputBox.height / 2
        val x4 = inputBox.left - viewportBox.left + inputBox.width / 2
        val y4 = inputBox.top - viewportBox.top + inputBox.height / 2
        val w = max(x4 - x1, 200.0)
        val x2 = x1 + w / 2
        val x3 = x4 - w / 2

        lines.add(Line(x1, y1, x2, y1, x3, y4, x4, y4))
      }
    }

    dispatch(Msg.SetLines(lines.toPersistentList()))
  }
}

fun sidePanel() = component {
  val model = useSelector<Model>()

  Div(
    css = listOf(
      flex(0, 0, 512.px),
      height(100.pct),
      backgroundColor(0xBBBBBB)
    ),
    children = listOf(
      mainPreview(model.compiled)
    )
  )
}

fun app() = component {
  store.provider(
    children = listOf(
      Div(
        css = listOf(
          width(100.pct),
          height(100.pct),
          display.flex()
        ),
        children = listOf(
          sidePanel(),
          viewport()
        )
      )
    )
  )
}

fun main() {
  ReactDom.render(app(), document.getElementById("app"))
}
