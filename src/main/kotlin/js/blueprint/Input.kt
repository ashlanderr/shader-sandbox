package js.blueprint

import org.w3c.dom.events.Event
import react.React
import kotlin.js.json

fun inputGroup(
  onChange: (Event) -> Unit,
  value: String,
  leftIcon: String? = undefined,
  placeholder: String? = undefined
) = React.createElement(
  Blueprint.InputGroup,
  json(
    "onChange" to onChange,
    "value" to value,
    "leftIcon" to leftIcon,
    "placeholder" to placeholder
  )
)
