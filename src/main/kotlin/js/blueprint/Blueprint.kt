package js.blueprint

import react.Component

@JsModule("@blueprintjs/core")
@JsNonModule
external object Blueprint {
  val Button: Component<dynamic>
  val ButtonGroup: Component<dynamic>
}

fun includeBlueprintStyles() {
  js("require('normalize.css')")
  js("require('@blueprintjs/core/lib/css/blueprint.css')")
  js("require('@blueprintjs/icons/lib/css/blueprint-icons.css')")
}
