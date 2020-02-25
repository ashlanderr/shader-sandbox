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
import kotlin.math.min
import kotlin.math.pow

private const val MOUSE_BUTTON_MIDDLE = 4.toShort()

private const val WHEEL_SIZE_LINE = 17.0
private const val WHEEL_MODE_PIXELS = 0
private const val WHEEL_MODE_LINES = 1
private const val ZOOM_BASE_FACTOR = 1.1

fun wheelToPixels(value: Double, mode: Int) = when (mode) {
  WHEEL_MODE_PIXELS -> value
  WHEEL_MODE_LINES -> value * WHEEL_SIZE_LINE
  else -> value
}

fun viewport() = component {
  val model = useSelector<Model>()
  val dispatch = useDispatch<Msg>()
  val events = useViewportHotKeys(dispatch)
  useBuildLines(model)
  val moving = model.move is ViewportMove.Viewport

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
    val offsetX = wheelToPixels(e.deltaX, e.deltaMode)
    val offsetY = wheelToPixels(e.deltaY, e.deltaMode)
    when {
      e.ctrlKey -> {
        val point = e.clientPoint.toWorld(model) ?: return
        val zoomAmount = -offsetY / 53.0
        val factor = ZOOM_BASE_FACTOR.pow(zoomAmount)
        dispatch(Msg.ScaleViewport(factor, point))
      }
      e.shiftKey -> {
        val offset = ViewPoint(offsetY, offsetX).toWorld(model, 0.0)
        dispatch(Msg.TranslateViewport(offset))
      }
      else -> {
        val offset = ViewPoint(offsetX, offsetY).toWorld(model, 0.0)
        dispatch(Msg.TranslateViewport(offset))
      }
    }
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
        willChange(if (moving) "transform" else null),
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

private data class LinesRect(
  val left: Double,
  val top: Double,
  val width: Double,
  val height: Double
)

private const val LINES_PADDING = 16.0

private fun computeLinesRect(model: Model): LinesRect {
  var points = model.lines.flatMap {
    listOf(
      WorldPoint(it.line.x1, it.line.y1),
      WorldPoint(it.line.x4, it.line.y4)
    )
  }
  points += when (val move = model.move) {
    is ViewportMove.SourceJoint -> listOf(move.begin, move.end)
    is ViewportMove.Node -> emptyList()
    is ViewportMove.Viewport -> emptyList()
    null -> emptyList()
  }

  var left = Double.POSITIVE_INFINITY
  var top = Double.POSITIVE_INFINITY
  var right = Double.NEGATIVE_INFINITY
  var bottom = Double.NEGATIVE_INFINITY

  for (p in points) {
    left = min(left, p.x)
    top = min(top, p.y)
    right = max(right, p.x)
    bottom = max(bottom, p.y)
  }

  return LinesRect(left, top, right - left, bottom - top)
}

private fun lines(model: Model) = component {
  if (model.lines.isNotEmpty()) {
    val selection = model.selection as? Selection.Joint
    val rect = computeLinesRect(model)

    Svg(
      css = listOf(
        position.absolute(),
        pointerEvents("none")
      ),
      style = listOf(
        left((rect.left - LINES_PADDING).px),
        top((rect.top - LINES_PADDING).px),
        width((rect.width + LINES_PADDING * 2).px),
        height((rect.height + LINES_PADDING * 2).px)
      ),
      children = listOf(
        when (val move = model.move) {
          is ViewportMove.SourceJoint -> pointLine(rect, move.begin, move.end)
          is ViewportMove.Node -> null
          is ViewportMove.Viewport -> null
          null -> null
        },
        *For(model.lines) { jointLine(rect, selection?.value == it.joint, it) }
      )
    )
  } else {
    null
  }
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

private fun jointLine(linesRect: LinesRect, selected: Boolean, jointLine: JointLine) = component {
  val dispatch = useDispatch<Msg>()
  val line = jointLine.line
  val dx = -linesRect.left + LINES_PADDING
  val dy = -linesRect.top + LINES_PADDING

  Path(
    css = listOf(
      cursor.pointer(),
      pointerEvents.all(),
      stroke(if (selected) 0x00fffc else 0xffffff),
      strokeWidth(2),
      fill(Color.transparent),
      hover(
        strokeWidth(4)
      )
    ),
    d = "M${line.x1 + dx} ${line.y1 + dy} C${line.x2 + dx} ${line.y2 + dy}, ${line.x3 + dx} ${line.y3 + dy}, ${line.x4 + dx} ${line.y4 + dy}",
    onClick = { dispatch(Msg.SelectJoint(jointLine.joint)) }
  )
}

private fun pointLine(linesRect: LinesRect, a: WorldPoint, b: WorldPoint) = component {
  val line = lineFromPoints(a, b)
  val dx = -linesRect.left + LINES_PADDING
  val dy = -linesRect.top + LINES_PADDING

  Path(
    css = listOf(
      stroke(0xffffff),
      strokeWidth(2),
      fill(Color.transparent)
    ),
    d = "M${line.x1 + dx} ${line.y1 + dy} C${line.x2 + dx} ${line.y2 + dy}, ${line.x3 + dx} ${line.y3 + dy}, ${line.x4 + dx} ${line.y4 + dy}"
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
