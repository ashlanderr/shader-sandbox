package app

import blueprint.button
import blueprint.buttonGroup
import io.akryl.component
import io.akryl.dom.html.Text

fun topBar(model: Model) = component {
  buttonGroup(className = "bp3-dark", fill = true, children = listOf(
    button(
      icon = "export",
      children = listOf(Text("Export"))
    ),
    button(
      icon = "import",
      children = listOf(Text("Import"))
    )
  ))
}