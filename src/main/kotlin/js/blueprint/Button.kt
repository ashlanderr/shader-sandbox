package js.blueprint

import react.React
import react.ReactElement
import kotlin.js.json

fun button(
  icon: String? = undefined,
  minimal: Boolean? = undefined,
  onClick: (() -> Unit)? = undefined,
  children: List<ReactElement<*>> = emptyList()
) = React.createElement(
  Blueprint.Button,
  json(
    "icon" to icon,
    "minimal" to minimal,
    "onClick" to onClick
  ),
  *children.toTypedArray()
)

fun buttonGroup(
  className: CharSequence? = undefined,
  fill: Boolean? = undefined,
  minimal: Boolean? = undefined,
  children: List<ReactElement<*>> = emptyList()
) = React.createElement(
  Blueprint.ButtonGroup,
  json(
    "className" to className.toString(),
    "minimal" to minimal,
    "fill" to fill
  ),
  *children.toTypedArray()
)
