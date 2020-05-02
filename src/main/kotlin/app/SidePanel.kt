package app

import io.akryl.component
import io.akryl.dom.css.properties.*
import io.akryl.dom.html.div
import io.akryl.dom.html.input
import io.akryl.dom.html.text
import io.akryl.redux.provider
import io.akryl.redux.useDispatch
import io.akryl.redux.useSelector
import js.blueprint.TreeNode
import js.blueprint.inputGroup
import js.blueprint.tree
import kotlinx.collections.immutable.persistentMapOf
import org.w3c.dom.DragEvent
import org.w3c.dom.HTMLInputElement
import react_dom.ReactDom
import kotlin.browser.document
import kotlin.math.roundToInt

fun sidePanel() = component {
  val model = useSelector<Model>()

  div(
    css = listOf(
      display.flex(),
      flexDirection.column(),
      width(512.px),
      height(100.pct),
      backgroundColor(0x293742)
    ),
    className = "bp3-dark",
    children = listOf(
      topBar(model),
      shaderPreview(model),
      when (val selection = model.selection) {
        null -> typesCatalogue(model)
        is Selection.Joint -> typesCatalogue(model)
        is Selection.Node -> nodeEditor(model, selection.node)
      }
    )
  )
}

private fun typesCatalogue(model: Model) = component {
  val dispatch = useDispatch<Msg>()

  div(
    css = listOf(
      flex(1, 1, 100.pct),
      overflow.hidden(),
      display.flex(),
      flexDirection.column()
    ),
    children = listOf(
      div(
        css = listOf(
          padding(4.px)
        ),
        child = inputGroup(
          placeholder = "Search for node types...",
          leftIcon = "search",
          value = model.search ?: "",
          onChange = { dispatch(Msg.SetSearch((it.target as HTMLInputElement).value)) }
        )
      ),
      categories(model)
    )
  )
}

private fun categories(model: Model) = component {
  val types = model.types.values
    .filterNot { it.hidden }

  val searchTypes = if (model.search != null) {
    types.filter { it.id.name.toLowerCase().contains(model.search.toLowerCase()) }
  } else {
    types
  }

  val categories = searchTypes.groupBy { it.id.category }

  fun onDrag(type: NodeTypeId, e: DragEvent) {
    e.dataTransfer?.setData("text", type.toString())

    val image = document.getElementById("types-catalogue-drag-image")?.parentElement ?: run {
      document.createElement("div").also {
        document.body?.appendChild(it)
      }
    }

    val node = Node(
      id = NodeId(-1),
      type = type,
      params = persistentMapOf(),
      offset = WorldPoint(0.0, 0.0)
    )
    val tree = div(
      id = "types-catalogue-drag-image",
      css = listOf(
        position.fixed(),
        left(-10000.px),
        top(-10000.px)
      ),
      child = div(
        style = listOf(
          transformOrigin(0.px, 0.px),
          transform.scale(model.transform.scale),
          padding(16.px)
        ),
        child = store.provider(listOf(
          node(model.types, model.selection, model.joints, node)
        ))
      )
    )
    ReactDom.render(tree, image)

    e.dataTransfer?.setDragImage(
      image,
      x = (16 * model.transform.scale).roundToInt(),
      y = (16 * model.transform.scale).roundToInt()
    )
  }

  val contents = categories.map { category ->
    TreeNode(
      id = category.key,
      label = text(category.key),
      nodeData = null,
      isExpanded = true,
      childNodes = category.value.map { type ->
        TreeNode(
          id = type.id.name,
          css = listOf(
            cursor.pointer()
          ),
          label = div(
            draggable = true,
            text = type.id.name,
            onDragStart = { onDrag(type.id, it) }
          ),
          nodeData = null
        )
      }
    )
  }

  tree(
    contents = contents,
    className = "custom-scrollbar"
  )
}

private fun nodeEditor(model: Model, nodeId: NodeId) = component {
  val node = model.nodes[nodeId] ?: UNKNOWN_NODE
  val type = model.types[node.type] ?: UNKNOWN_TYPE

  div(
    css = listOf(
      flex(1, 1, 100.pct),
      overflow.auto()
    ),
    children = type.params.map { (_, param) -> paramEditor(node, param) }
  )
}

private fun paramEditor(node: Node, param: ParamType) = component {
  val value = node.params[param.id]

  div(
    css = listOf(
      display.flex(),
      alignItems.center()
    ),
    children = listOf(
      div(text = param.id.value),
      when (param.type) {
        DataType.Scalar ->
          scalarEditor(node.id, param.id, value as? DataValue.Scalar)
        DataType.Color ->
          colorEditor(node.id, param.id, value as? DataValue.Color)
      }
    )
  )
}

private fun scalarEditor(node: NodeId, param: ParamId, value: DataValue.Scalar?) = component {
  val dispatch = useDispatch<Msg>()
  val float = value?.value ?: 0.0f

  input(
    css = listOf(
      flex(1, 1, 100.pct),
      marginLeft(16.px)
    ),
    type = "number",
    value = float.toString(),
    onChange = { e ->
      val newValue = (e.target as HTMLInputElement).value.toFloatOrNull()
      if (newValue != null) {
        dispatch(Msg.PutNodeParam(node, param, DataValue.Scalar(newValue)))
      }
    }
  )
}

private fun colorEditor(node: NodeId, param: ParamId, value: DataValue.Color?) = component {
  val dispatch = useDispatch<Msg>()
  val color = value ?: DataValue.Color(0.0f, 0.0f, 0.0f, 0.0f)

  data class Component(
    val name: String,
    val value: Float,
    val setter: (Float) -> DataValue.Color
  )

  val components = listOf(
    Component("Red", color.r) { v -> color.copy(r = v) },
    Component("Green", color.g) { v -> color.copy(g = v) },
    Component("Blue", color.b) { v -> color.copy(b = v) },
    Component("Alpha", color.a) { v -> color.copy(a = v) }
  )

  div(
    css = listOf(
      display.flex(),
      flex(1, 1, 100.pct),
      marginLeft(16.px)
    ),
    children = components.map { (name, v, set) ->
      input(
        css = listOf(
          flex(1, 1, 100.pct),
          width(100.pct)
        ),
        type = "number",
        value = v.toString(),
        placeholder = name,
        onChange = { e ->
          val newV = (e.target as HTMLInputElement).value.toFloatOrNull()
          if (newV != null) {
            dispatch(Msg.PutNodeParam(node, param, set(newV)))
          }
        }
      )
    }
  )
}
