@file:Suppress("unused")

package js.blueprint

import io.akryl.dom.css.CssElement
import io.akryl.dom.css.cssRegistry
import react.React
import react.ReactElement
import kotlin.js.json

class TreeNode<T>(
  val id: String,
  val label: ReactElement<*>,
  val icon: String? = undefined,
  val isExpanded: Boolean? = undefined,
  val nodeData: T,
  childNodes: List<TreeNode<T>>? = undefined,
  css: List<CssElement>? = null,
  className: CharSequence? = null
) {
  val childNodes = childNodes?.toTypedArray()
  val className = listOfNotNull(
    css?.let { cssRegistry.findOrCreateClassName(it) },
    className
  ).joinToString(" ")
}

fun <T> tree(
  contents: List<TreeNode<T>>,
  className: CharSequence? = undefined
) = React.createElement(
  Blueprint.Tree,
  json(
    "contents" to contents.toTypedArray(),
    "className" to className
  )
)
