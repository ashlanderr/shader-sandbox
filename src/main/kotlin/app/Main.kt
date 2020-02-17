package app

import io.akryl.component
import io.akryl.dom.css.properties.display
import io.akryl.dom.css.properties.height
import io.akryl.dom.css.properties.pct
import io.akryl.dom.css.properties.width
import io.akryl.dom.html.Div
import io.akryl.redux.provider
import react_dom.ReactDom
import kotlin.browser.document

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
