package app

import hotkeys.HotKeysEvents
import hotkeys.useHotKeys
import io.akryl.ComponentScope
import io.akryl.component
import io.akryl.dom.css.invoke
import io.akryl.dom.css.properties.*
import io.akryl.dom.html.Div
import io.akryl.dom.html.Path
import io.akryl.dom.html.Svg
import io.akryl.redux.useDispatch
import io.akryl.redux.useSelector
import io.akryl.useEffect
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentList
import org.w3c.dom.HTMLElement
import react.ReactElement
import react_redux.Dispatch
import kotlin.browser.document
import kotlin.math.max

private val unknownType = NodeType(
  id = NodeTypeId("unknown"),
  name = "<Unknown>",
  params = entityMapOf(),
  inputs = persistentSetOf(),
  outputs = persistentSetOf(),
  uniforms = emptySet(),
  code = emptyMap()
)

fun viewport() = component {
  val model = useSelector<Model>()
  val dispatch = useDispatch<Msg>()
  val events = useViewportHotKeys(dispatch)
  useBuildLines(model)

  Div(
    onKeyDown = events.onKeyDown,
    onKeyUp = events.onKeyUp,
    onFocus = events.onFocus,
    tabIndex = -1,
    id = "viewport",
    css = listOf(
      flex(1, 1, 100.pct),
      width(100.pct),
      height(100.pct),
      backgroundColor(0x252525),
      outline.none(),
      position.relative(),
      overflow.hidden()
    ),
    child = Div(
      lines(model),
      nodes(model)
    ),
    onMouseMove = {
      if (model.move != null)
        dispatch(Msg.DoMove(it.clientX, it.clientY))
    },
    onClick = { e ->
      if (e.target === e.currentTarget) {
        dispatch(Msg.ClearSelection)
      }
    }
  )
}

private fun lines(model: Model): ReactElement<*> {
  val selection = model.selection as? Selection.Joint

  return Svg(
    css = listOf(
      position.absolute(),
      pointerEvents("none"),
      left(0.px),
      top(0.px),
      width(100.pct),
      height(100.pct)
    ),
    children = model.lines.map { line(selection?.value == it.joint, it) }
  )
}

private fun nodes(model: Model): ReactElement<*> {
  return Div(
    css = listOf(
      position.absolute(),
      left(0.px),
      top(0.px)
    ),
    children = model.nodes.map { node(model, it.value) }
  )
}

private fun input(joints: Joints, node: NodeId, input: InputId) = component {
  val connected = Pair(node, input) in joints
  val color = Color.white

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
          border.solid(1.px, color),
          backgroundColor(if (connected) color else Color.black),
          marginLeft(-8.px),
          marginRight(4.px)
        )
      ),
      Div(text = input.value)
    )
  )
}

private fun output(joints: Joints, node: NodeId, output: OutputId) = component {
  val connected = joints.any { it.value.source.first == node && it.value.source.second == output }

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
          border.solid(1.px, color),
          backgroundColor(if (connected) color else Color.black),
          marginLeft(16.px),
          marginRight(-8.px)
        )
      )
    )
  )
}

private fun node(model: Model, node: Node) = component {
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
      nodeBody(model.joints, type, node)
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

private fun nodeBody(joints: Joints, type: NodeType, node: Node) = component {
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
        children = type.inputs.map { input(joints, node.id, it) }
      ),
      Div(
        css = listOf(
          display.flex(),
          flexDirection.column(),
          padding(vertical = 4.px, horizontal = 4.px)
        ),
        children = type.outputs.map { output(joints, node.id, it) }
      )
    )
  )
}

private fun line(selected: Boolean, line: Line) = component {
  val dispatch = useDispatch<Msg>()

  Path(
    css = listOf(
      cursor.pointer(),
      pointerEvents("all"),
      stroke(if (selected) "#00fffc" else "#ffffff"),
      strokeWidth("2"),
      fill("transparent"),
      hover(
        strokeWidth("4")
      )
    ),
    d = "M${line.x1} ${line.y1} C${line.x2} ${line.y2}, ${line.x3} ${line.y3}, ${line.x4} ${line.y4}",
    onClick = { dispatch(Msg.SelectJoint(line.joint)) }
  )
}

private fun ComponentScope.useBuildLines(model: Model) {
  val dispatch = useDispatch<Msg>()

  useEffect(listOf(model.nodes, model.joints)) {
    val lines = ArrayList<Line>()

    val viewport = document.getElementById("viewport") as? HTMLElement ?: return@useEffect
    val viewportBox = viewport.getBoundingClientRect()

    for ((_, joint) in model.joints) {
      val inputId = "node-${joint.dest.first}-input-${joint.dest.second}"
      val outputId = "node-${joint.source.first}-output-${joint.source.second}"
      val input = document.getElementById(inputId) as? HTMLElement ?: continue
      val output = document.getElementById(outputId) as? HTMLElement ?: continue
      val inputBox = input.getBoundingClientRect()
      val outputBox = output.getBoundingClientRect()

      val x1 = outputBox.left - viewportBox.left + outputBox.width / 2
      val y1 = outputBox.top - viewportBox.top + outputBox.height / 2
      val x4 = inputBox.left - viewportBox.left + inputBox.width / 2
      val y4 = inputBox.top - viewportBox.top + inputBox.height / 2
      val w = max(x4 - x1, 50.0)
      val x2 = x1 + w / 2
      val x3 = x4 - w / 2

      lines.add(Line(joint, x1, y1, x2, y1, x3, y4, x4, y4))
    }

    dispatch(Msg.SetLines(lines.toPersistentList()))
  }
}

private fun ComponentScope.useViewportHotKeys(dispatch: Dispatch<Msg>): HotKeysEvents {
  return useHotKeys {
    delete {
      dispatch(Msg.DeleteSelected)
    }
  }
}
