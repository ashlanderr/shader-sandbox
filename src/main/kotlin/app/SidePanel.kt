package app

import io.akryl.component
import io.akryl.dom.css.properties.*
import io.akryl.dom.html.Div
import io.akryl.redux.useSelector

fun sidePanel() = component {
  val model = useSelector<Model>()

  Div(
    css = listOf(
      flex(0, 0, 512.px),
      height(100.pct),
      backgroundColor(0xBBBBBB)
    ),
    children = listOf(
      mainPreview(model.compiled)
    )
  )
}
