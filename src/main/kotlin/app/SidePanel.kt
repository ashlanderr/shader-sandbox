package app

import io.akryl.component
import io.akryl.dom.css.invoke
import io.akryl.dom.css.properties.*
import io.akryl.dom.html.Div
import io.akryl.dom.html.For
import io.akryl.dom.html.Input
import io.akryl.dom.html.Text
import io.akryl.redux.provider
import io.akryl.redux.useDispatch
import io.akryl.redux.useSelector
import kotlinx.collections.immutable.persistentMapOf
import org.w3c.dom.DragEvent
import org.w3c.dom.HTMLInputElement
import react_dom.ReactDom
import kotlin.browser.document

fun sidePanel() = component {
  val model = useSelector<Model>()

  Div(
    css = listOf(
      display.flex(),
      flexDirection.column(),
      width(512.px),
      height(100.pct),
      backgroundColor(0xBBBBBB)
    ),
    children = listOf(
      mainPreview(model.compiled),
      typesCatalogue(model)
    )
  )
}

private fun typesCatalogue(model: Model) = component {
  val dispatch = useDispatch<Msg>()

  Div(
    css = listOf(
      flex(1, 1, 100.pct),
      overflow.auto()
    ),
    children = listOf(
      Input(
        css = listOf(
          width(100.pct),
          boxSizing.borderBox()
        ),
        placeholder = "Search",
        value = model.search,
        onChange = { dispatch(Msg.SetSearch((it.target as HTMLInputElement).value)) }
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

    val image = document.getElementById("types-catalogue-drag-image") ?: run {
      document.createElement("div").also {
        document.body?.appendChild(it)
      }
    }

    val node = Node(
      id = NodeId(-1),
      type = type,
      params = persistentMapOf(),
      offset = Point(0.0, 0.0)
    )
    val tree = Div(
      id = "types-catalogue-drag-image",
      css = listOf(
        position.fixed(),
        left(-10000.px),
        top(-10000.px),
        padding(16.px)
      ),
      child = store.provider(listOf(
        node(model, node)
      ))
    )
    ReactDom.render(tree, image)

    e.dataTransfer?.setDragImage(image, 16, 16)
  }

  Div(
    *For(categories.entries) { (category, types) ->
      Div(
        Text(category),
        Div(
          *For(types) { type ->
            Div(
              draggable = true,
              css = listOf(
                paddingLeft(16.px),
                cursor.pointer(),
                userSelect("none"),
                hover(
                  backgroundColor(0xCCCCCC)
                )
              ),
              text = type.id.name,
              onDragStart = { onDrag(type.id, it) }
            )
          }
        )
      )
    }
  )
}
