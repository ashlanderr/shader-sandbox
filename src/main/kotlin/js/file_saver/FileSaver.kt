package js.file_saver

import org.w3c.files.Blob

@JsModule("file-saver")
@JsNonModule
external object FileSaver {
  fun saveAs(blob: Blob, name: String)
}
