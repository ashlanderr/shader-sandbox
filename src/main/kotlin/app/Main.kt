package app

import io.akryl.ComponentScope
import io.akryl.component
import io.akryl.dom.css.properties.display
import io.akryl.dom.css.properties.height
import io.akryl.dom.css.properties.pct
import io.akryl.dom.css.properties.width
import io.akryl.dom.html.Div
import io.akryl.redux.provider
import io.akryl.useEffect
import io.akryl.useRef
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.events.Event
import org.w3c.dom.events.WheelEvent
import react.MutableRefObject
import react_dom.ReactDom
import kotlin.browser.document

fun app() = component {
  val ref = useRef<HTMLDivElement?>(null)
  usePreventZoom(ref)

  store.provider(
    children = listOf(
      Div(
        ref = ref,
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

private fun ComponentScope.usePreventZoom(ref: MutableRefObject<HTMLDivElement?>) {
  useEffect(emptyList()) {
    val listener = { e: Event ->
      e as WheelEvent
      if (e.ctrlKey) {
        e.preventDefault()
      }
    }
    ref.current?.addEventListener("wheel", listener)
    dispose { ref.current?.removeEventListener("wheel", listener) }
  }
}

fun main() {
  ReactDom.render(app(), document.getElementById("app"))
}
