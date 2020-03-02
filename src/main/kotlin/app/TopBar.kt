package app

import io.akryl.component
import io.akryl.dom.css.properties.display
import io.akryl.dom.html.Input
import io.akryl.dom.html.Text
import io.akryl.redux.useDispatch
import io.akryl.useRef
import js.blueprint.button
import js.blueprint.buttonGroup
import js.file_saver.FileSaver
import org.w3c.dom.HTMLInputElement
import org.w3c.files.Blob
import org.w3c.files.BlobPropertyBag
import org.w3c.files.FileReader
import org.w3c.files.get
import kotlin.browser.window

fun topBar(model: Model) = component {
  buttonGroup(className = "bp3-dark", fill = true, children = listOf(
    exportButton(model),
    importButton()
  ))
}

fun exportButton(model: Model) = component {
  fun onClick() {
    val json = JSON.stringify(PersistentModel(model.nodes, model.joints).toJson())
    val blob = Blob(arrayOf(json), BlobPropertyBag(type = "application/json;charset=utf-8"))
    FileSaver.saveAs(blob, "shader.json")
  }

  button(
    icon = "export",
    children = listOf(Text("Export")),
    onClick = { onClick() }
  )
}

fun importButton() = component {
  val dispatch = useDispatch<Msg>()
  val ref = useRef<HTMLInputElement?>(null)

  fun onClick() {
    val input = ref.current ?: return
    input.click()
  }

  fun fileChanged() {
    val file = ref.current?.files?.get(0) ?: return
    if (file.type !== "application/json") return
    val reader = FileReader()
    reader.onload = {
      try {
        val data = persistenceModelFromJson(JSON.parse(reader.result as String))
        dispatch(Msg.ApplyPersistentModel(data))
      } catch (ex: Throwable) {
        console.error(ex)
        window.alert("Failed to read file '${file.name}'")
      }
    }
    reader.readAsText(file)
  }

  button(
    icon = "import",
    children = listOf(
      Text("Import"),
      Input(
        ref = ref,
        type = "file",
        css = listOf(display.none()),
        onChange = { fileChanged() },
        accept = "application/json"
      )
    ),
    onClick = { onClick() }
  )
}