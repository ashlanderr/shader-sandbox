package app

import hotkeys.HotKeysEvents
import hotkeys.useHotKeys
import io.akryl.ComponentScope
import io.akryl.component
import io.akryl.dom.css.invoke
import io.akryl.dom.css.properties.*
import io.akryl.dom.html.Div
import io.akryl.dom.html.For
import io.akryl.dom.html.Path
import io.akryl.dom.html.Svg
import io.akryl.redux.useDispatch
import io.akryl.redux.useSelector
import io.akryl.useEffect
import kotlinx.collections.immutable.toPersistentList
import org.w3c.dom.DragEvent
import org.w3c.dom.HTMLElement
import org.w3c.dom.events.Event
import org.w3c.dom.events.MouseEvent
import org.w3c.dom.events.WheelEvent
import react.ReactElement
import react_redux.Dispatch
import kotlin.browser.document
import kotlin.experimental.and
import kotlin.math.max

private const val MOUSE_BUTTON_MIDDLE = 4.toShort()

fun viewport() = component {
  val model = useSelector<Model>()
  val dispatch = useDispatch<Msg>()
  val events = useViewportHotKeys(dispatch)
  useBuildLines(model)

  fun onDragOver(e: DragEvent) {
    e.preventDefault()
  }

  fun onDrop(e: DragEvent) {
    val type = e.dataTransfer?.getData("text")?.toNodeTypeId() ?: return
    val offset = e.clientPoint.toWorld(model) ?: return
    dispatch(Msg.AddNode(type, offset))
  }

  fun onMouseDown(e: MouseEvent) {
    if ((e.buttons and MOUSE_BUTTON_MIDDLE) == MOUSE_BUTTON_MIDDLE) {
      e.clientPoint.toWorld(model)?.let {
        dispatch(Msg.MoveViewport(it))
      }
    }
  }

  fun onMouseMove(e: MouseEvent) {
    if (model.move != null) {
      val point = e.clientPoint.toWorld(model)
      point?.let { dispatch(Msg.DoMove(it)) }
    }
  }

  fun onClick(e: Event) {
    if (e.target === e.currentTarget) {
      dispatch(Msg.ClearSelection)
    }
  }

  fun onWheel(e: WheelEvent) {
    val point = e.clientPoint.toWorld(model) ?: return
    val speed = 1.3
    val factor = if (e.deltaY < 0) {
      speed
    } else {
      1.0 / speed
    }
    dispatch(Msg.ScaleViewport(factor, point))
  }

  Div(
    onKeyDown = events.onKeyDown,
    onKeyUp = events.onKeyUp,
    onFocus = events.onFocus,
    tabIndex = -1,
    id = VIEWPORT_ID,
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
      style = listOf(
        transformOrigin("0 0"),
        transform
          .scale(model.scale)
          .translate(model.offset.x.px, model.offset.y.px)
      ),
      children = listOf(
        lines(model),
        nodes(model)
      )
    ),
    onMouseMove = ::onMouseMove,
    onMouseDown = ::onMouseDown,
    onMouseUp = { dispatch(Msg.StopOnViewport) },
    onClick = ::onClick,
    onDragOver = ::onDragOver,
    onDrop = ::onDrop,
    onWheel = ::onWheel
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
      width(1000.px),
      height(1000.px)
    ),
    children = listOf(
      when (val move = model.move) {
        is ViewportMove.SourceJoint -> pointLine(move.begin, move.end)
        is ViewportMove.Node -> null
        is ViewportMove.Viewport -> null
        null -> null
      },
      *For(model.lines) { jointLine(selection?.value == it.joint, it) }
    )
  )
}

private fun nodes(model: Model): ReactElement<*> {
  return Div(
    css = listOf(
      position.absolute(),
      left(0.px),
      top(0.px)
    ),
    children = model.nodes.map {
      Div(
        css = listOf(
          position.absolute(),
          left(0.px),
          top(0.px)
        ),
        style = listOf(
          transform.translate(it.value.offset.x.px, it.value.offset.y.px)
        ),
        child = node(model, it.value)
      )
    }
  )
}

private fun jointLine(selected: Boolean, jointLine: JointLine) = component {
  val dispatch = useDispatch<Msg>()
  val line = jointLine.line

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
    onClick = { dispatch(Msg.SelectJoint(jointLine.joint)) }
  )
}

private fun pointLine(a: WorldPoint, b: WorldPoint) = component {
  val line = lineFromPoints(a, b)

  Path(
    css = listOf(
      stroke("#ffffff"),
      strokeWidth("2"),
      fill("transparent")
    ),
    d = "M${line.x1} ${line.y1} C${line.x2} ${line.y2}, ${line.x3} ${line.y3}, ${line.x4} ${line.y4}"
  )
}

private fun lineFromPoints(a: WorldPoint, b: WorldPoint): Line {
  val x1 = a.x
  val y1 = a.y
  val x4 = b.x
  val y4 = b.y
  val w = max(x4 - x1, 50.0)
  val x2 = x1 + w / 2
  val x3 = x4 - w / 2

  return Line(x1, y1, x2, y1, x3, y4, x4, y4)
}

private fun ComponentScope.useBuildLines(model: Model) {
  val dispatch = useDispatch<Msg>()

  useEffect(listOf(model.nodes, model.joints)) {
    val lines = ArrayList<JointLine>()

    val viewport = document.getElementById(VIEWPORT_ID) as? HTMLElement ?: return@useEffect
    val viewportBox = viewport.getBoundingClientRect()

    for ((_, joint) in model.joints) {
      val inputId = "node-${joint.dest.first}-input-${joint.dest.second}"
      val outputId = "node-${joint.source.first}-output-${joint.source.second}"
      val input = document.getElementById(inputId) as? HTMLElement ?: continue
      val output = document.getElementById(outputId) as? HTMLElement ?: continue
      val inputBox = input.getBoundingClientRect()
      val outputBox = output.getBoundingClientRect()

      val p1 = ClientPoint(
        outputBox.left + outputBox.width / 2,
        outputBox.top + outputBox.height / 2
      ).toWorld(viewportBox, model)

      val p2 = ClientPoint(
        inputBox.left + inputBox.width / 2,
        inputBox.top + inputBox.height / 2
      ).toWorld(viewportBox, model)

      lines.add(JointLine(joint, lineFromPoints(p1, p2)))
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
