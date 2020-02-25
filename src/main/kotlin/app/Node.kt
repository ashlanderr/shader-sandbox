package app

import io.akryl.component
import io.akryl.dom.css.properties.*
import io.akryl.dom.html.Div
import io.akryl.dom.html.For
import io.akryl.redux.useDispatch

private fun input(joints: Joints, node: NodeId, input: InputId) = component {
  val dispatch = useDispatch<Msg>()
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
    ),
    onMouseUp = { dispatch(Msg.StopOnInput(node, input)) }
  )
}

private fun output(model: Model, node: NodeId, output: OutputId) = component {
  val dispatch = useDispatch<Msg>()

  val connected = model.joints
    .any { it.value.source.first == node && it.value.source.second == output }

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
    onMouseDown = { e ->
      e.clientPoint.toWorld(model)?.let { p ->
        dispatch(Msg.MoveSourceJoint(
          node = node,
          output = output,
          point = p
        ))
      }
    },
    children = listOf(
      Div(
        id = "node-$node-output-$output",
        css = listOf(
          width(8.px),
          height(8.px),
          borderRadius(100.pct),
          border.solid(1.px, color),
          backgroundColor(if (connected) color else Color.black),
          marginRight(-8.px)
        )
      )
    )
  )
}

fun node(model: Model, node: Node) = component {
  val type = model.types[node.type] ?: UNKNOWN_TYPE

  Div(
    css = listOf(
      display.flex(),
      flexDirection.column(),
      backgroundColor(0xBBBBBB),
      userSelect.none()
    ),
    children = listOf(
      nodeHeader(model, node.id, type),
      nodeBody(model, type, node)
    )
  )
}

private fun nodeHeader(model: Model, node: NodeId, type: NodeType) = component {
  val dispatch = useDispatch<Msg>()
  val selected = model.selection is Selection.Node && model.selection.node == node

  Div(
    css = listOf(
      padding(vertical = 8.px, horizontal = 4.px),
      textAlign.center(),
      cursor.move(),
      if (selected) backgroundColor(0x00fffc) else null
    ),
    text = type.id.name,
    onMouseDown = { e ->
      if (e.target === e.currentTarget) {
        dispatch(Msg.SelectNode(node))
        e.clientPoint.toWorld(model)?.let {
          dispatch(Msg.MoveNode(node, it))
        }
      }
    }
  )
}

private fun nodeBody(model: Model, type: NodeType, node: Node) = component {
  val outputsPadding = if (type.outputs.isNotEmpty())
    padding(vertical = 4.px, horizontal = 4.px)
  else
    null

  Div(
    css = listOf(
      display.flex(),
      backgroundColor(0x505050),
      color(0xFFFFFF),
      justifyContent.spaceBetween()
    ),
    children = listOf(
      Div(
        css = listOf(
          display.flex(),
          flex(1, 1, 100.pct),
          padding(vertical = 4.px, horizontal = 4.px),
          flexDirection.column()
        ),
        children = listOf(
          *For(type.inputs) {
            input(model.joints, node.id, it)
          },
          smallShaderPreview(node.id)
        )
      ),
      Div(
        css = listOf(
          display.flex(),
          flexDirection.column(),
          outputsPadding
        ),
        children = type.outputs.map { output(model, node.id, it) }
      )
    )
  )
}