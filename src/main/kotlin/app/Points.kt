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

fun ViewPoint.toWorld(transform: Transform, w: Double = 1.0): WorldPoint {
  return WorldPoint(
    x / transform.scale - transform.offset.x * w,
    y / transform.scale - transform.offset.y * w
  )
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

fun ClientPoint.toWorld(viewport: DOMRect, transform: Transform) = this.toView(viewport).toWorld(transform)

fun ClientPoint.toWorld(transform: Transform) = this.toView()?.toWorld(transform)
