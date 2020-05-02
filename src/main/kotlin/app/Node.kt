package app

import io.akryl.component
import io.akryl.dom.css.properties.*
import io.akryl.dom.html.For
import io.akryl.dom.html.div
import io.akryl.memo
import io.akryl.redux.useDispatch

private fun input(joints: Joints, node: NodeId, input: InputId) = component {
  val dispatch = useDispatch<Msg>()
  val connected = Pair(node, input) in joints
  val color = Color.white

  div(
    css = listOf(
      display.flex(),
      alignItems.center(),
      height(1.em),
      cursor.pointer()
    ),
    children = listOf(
      div(
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
      div(text = input.value)
    ),
    onMouseUp = { dispatch(Msg.StopOnInput(node, input)) }
  )
}

private fun output(joints: Joints, node: NodeId, output: OutputId) = memo {
  val dispatch = useDispatch<Msg>()

  val connected = joints
    .any { it.value.source.first == node && it.value.source.second == output }

  val color = when (output) {
    OutputId.All, OutputId.Alpha -> Color.white
    OutputId.Red -> Color.red
    OutputId.Green -> Color.lime
    OutputId.Blue -> Color.blue
  }

  div(
    css = listOf(
      display.flex(),
      alignItems.center(),
      height(1.em),
      cursor.pointer()
    ),
    onMouseDown = { e ->
      e.clientPoint.toView()?.let { p ->
        dispatch(Msg.MoveSourceJoint(
          node = node,
          output = output,
          point = p
        ))
      }
    },
    children = listOf(
      div(
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

fun node(types: EntityMap<NodeTypeId, NodeType>, selection: Selection?, joints: Joints, node: Node) = memo {
  val type = types[node.type] ?: UNKNOWN_TYPE

  div(
    css = listOf(
      display.flex(),
      flexDirection.column(),
      backgroundColor(0xBBBBBB),
      userSelect.none()
    ),
    children = listOf(
      nodeHeader(selection, node.id, type),
      nodeBody(joints, type, node)
    )
  )
}

private fun nodeHeader(selection: Selection?, node: NodeId, type: NodeType) = memo {
  val dispatch = useDispatch<Msg>()
  val selected = selection is Selection.Node && selection.node == node

  div(
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
        e.clientPoint.toView()?.let {
          dispatch(Msg.MoveNode(node, it))
        }
      }
    }
  )
}

private fun nodeBody(joints: Joints, type: NodeType, node: Node) = memo {
  val outputsPadding = if (type.outputs.isNotEmpty())
    padding(vertical = 4.px, horizontal = 4.px)
  else
    null

  div(
    css = listOf(
      display.flex(),
      backgroundColor(0x505050),
      color(0xFFFFFF),
      justifyContent.spaceBetween()
    ),
    children = listOf(
      div(
        css = listOf(
          display.flex(),
          flex(1, 1, 100.pct),
          padding(vertical = 4.px, horizontal = 4.px),
          flexDirection.column()
        ),
        children = listOf(
          *For(type.inputs) {
            input(joints, node.id, it)
          },
          smallShaderPreview(node.id)
        )
      ),
      div(
        css = listOf(
          display.flex(),
          flexDirection.column(),
          outputsPadding
        ),
        children = type.outputs.map { output(joints, node.id, it) }
      )
    )
  )
}