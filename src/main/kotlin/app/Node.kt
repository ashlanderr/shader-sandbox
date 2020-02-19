package app

import io.akryl.component
import io.akryl.dom.css.properties.*
import io.akryl.dom.html.Div
import io.akryl.redux.useDispatch
import kotlinx.collections.immutable.persistentSetOf

private val unknownType = NodeType(
  id = NodeTypeId("<Unknown>", "<Unknown>"),
  params = entityMapOf(),
  inputs = persistentSetOf(),
  outputs = persistentSetOf(),
  globals = emptySet(),
  code = emptyMap()
)

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

private fun output(joints: Joints, node: NodeId, output: OutputId) = component {
  val dispatch = useDispatch<Msg>()
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
    onMouseDown = { e ->
      e.viewportOffset?.let { p ->
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
          marginLeft(16.px),
          marginRight(-8.px)
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
      backgroundColor(0xBBBBBB),
      minWidth(150.px),
      userSelect("none")
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
    text = type.id.name,
    onMouseDown = { e ->
      e.viewportOffset?.let { dispatch(Msg.MoveNode(node, it)) }
    }
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