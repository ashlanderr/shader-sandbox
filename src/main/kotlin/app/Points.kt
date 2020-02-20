package app

import org.w3c.dom.DOMRect
import org.w3c.dom.HTMLElement
import org.w3c.dom.events.MouseEvent
import kotlin.browser.document

data class WorldPoint(
  val x: Double,
  val y: Double
)

data class ViewPoint(
  val x: Double,
  val y: Double
)

data class ClientPoint(
  val x: Double,
  val y: Double
)

fun ViewPoint.toWorld(model: Model): WorldPoint {
  // todo transform
  return WorldPoint(x, y)
}

val MouseEvent.clientPoint get() = ClientPoint(this.clientX.toDouble(), this.clientY.toDouble())

const val VIEWPORT_ID = "viewport"

fun ClientPoint.toView(): ViewPoint? {
  val viewport = document.getElementById(VIEWPORT_ID) as? HTMLElement ?: return null
  val viewportBox = viewport.getBoundingClientRect()
  return this.toView(viewportBox)
}

fun ClientPoint.toView(viewport: DOMRect): ViewPoint {
  return ViewPoint(this.x - viewport.left, this.y - viewport.top)
}

fun ClientPoint.toWorld(viewport: DOMRect, model: Model) = this.toView(viewport).toWorld(model)

fun ClientPoint.toWorld(model: Model) = this.toView()?.toWorld(model)
