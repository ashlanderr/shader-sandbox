package app

import io.akryl.redux.MsgAction
import io.akryl.redux.createStore
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentMap
import org.w3c.dom.get
import org.w3c.dom.set
import react_redux.Dispatch
import redux.StoreEnhancer
import kotlin.browser.localStorage
import kotlin.math.max
import kotlin.math.min

private const val LOCAL_STORAGE_DATA_KEY = "data"

val store by lazy {
  createStore(
    init = Pair(
      Model(
        types = NODE_TYPES,
        nodes = INITIAL_NODES,
        joints = INITIAL_JOINTS,
        lines = persistentListOf(),
        move = null,
        compiled = compile(NODE_TYPES, INITIAL_NODES, INITIAL_JOINTS),
        selection = null,
        search = null,
        transform = Transform(
          offset = WorldPoint(0.0, 0.0),
          scale = 1.0
        )
      ),
      Cmd.LocalStorageGet(LOCAL_STORAGE_DATA_KEY, Msg::ParseData)
    ),
    update = ::update,
    execute = ::execute,
    enhancer = js("window.__REDUX_DEVTOOLS_EXTENSION__ && window.__REDUX_DEVTOOLS_EXTENSION__()")
      .unsafeCast<StoreEnhancer<Model, MsgAction<Msg>>>()
  )
}

private fun update(model: Model, msg: Msg): Pair<Model, Cmd?> {
  return when (msg) {
    is Msg.SetLines -> setLines(model, msg)
    is Msg.SetSearch -> setSearch(model, msg)
    is Msg.MoveViewport -> moveViewport(model, msg)
    is Msg.ScaleViewport -> scaleViewport(model, msg)
    is Msg.TranslateViewport -> translateViewport(model, msg)
    is Msg.MoveNode -> moveNode(model, msg)
    is Msg.MoveSourceJoint -> moveSourceJoint(model, msg)
    is Msg.StopOnInput -> stopOnInput(model, msg)
    is Msg.StopOnViewport -> stopOnViewport(model)
    is Msg.DoMove -> doMove(model, msg)
    is Msg.SelectJoint -> selectJoint(model, msg)
    is Msg.SelectNode -> selectNode(model, msg)
    is Msg.ClearSelection -> clearSelection(model)
    is Msg.DeleteSelected -> deleteSelected(model)
    is Msg.ApplyPersistentModel -> applyPersistentModel(model, msg)
    is Msg.ParseData -> parseData(model, msg)
    is Msg.PutNodeParam -> putNodeParam(model, msg)
    is Msg.AddNode -> addNode(model, msg)
  }
}

private fun setLines(model: Model, msg: Msg.SetLines): Pair<Model, Nothing?> {
  return Pair(
    model.copy(lines = msg.lines),
    null
  )
}

fun setSearch(model: Model, msg: Msg.SetSearch): Pair<Model, Cmd?> {
  val search = msg.value.takeIf { it.isNotBlank() }

  return Pair(
    model.copy(search = search),
    null
  )
}

private fun moveViewport(model: Model, msg: Msg.MoveViewport): Pair<Model, Nothing?> {
  return Pair(
    model.copy(move = ViewportMove.Viewport(msg.point)),
    null
  )
}

private fun scaleViewport(model: Model, msg: Msg.ScaleViewport): Pair<Model, Nothing?> {
  val newScale = max(0.1, min(msg.factor * model.transform.scale, 4.0))

  return Pair(
    model.copy(
      transform = Transform(
        scale = newScale,
        offset = WorldPoint(
          (msg.center.x + model.transform.offset.x) * model.transform.scale / newScale - msg.center.x,
          (msg.center.y + model.transform.offset.y) * model.transform.scale / newScale - msg.center.y
        )
      )
    ),
    null
  )
}

private fun translateViewport(model: Model, msg: Msg.TranslateViewport): Pair<Model, Cmd?> {
  return Pair(
    model.copy(
      transform = model.transform.copy(
        offset = WorldPoint(model.transform.offset.x - msg.offset.x, model.transform.offset.y - msg.offset.y)
      )
    ),
    null
  )
}

private fun moveNode(model: Model, msg: Msg.MoveNode): Pair<Model, Nothing?> {
  return Pair(
    model.copy(move = ViewportMove.Node(msg.node, msg.point.toWorld(model.transform))),
    null
  )
}

private fun moveSourceJoint(model: Model, msg: Msg.MoveSourceJoint): Pair<Model, Cmd?> {
  val worldPoint = msg.point.toWorld(model.transform)
  return Pair(
    model.copy(move = ViewportMove.SourceJoint(msg.node, msg.output, worldPoint, worldPoint)),
    null
  )
}

fun stopOnInput(model: Model, msg: Msg.StopOnInput): Pair<Model, Cmd?> {
  return when (val move = model.move) {
    is ViewportMove.SourceJoint -> {
      val newModel = model.copy(
        joints = model.joints.put(
          Joint(
            source = Pair(move.node, move.output),
            dest = Pair(msg.node, msg.input)
          )
        ),
        move = null
      )
      triggerCompile(newModel)
    }

    is ViewportMove.Viewport ->
      Pair(model, null)

    is ViewportMove.Node ->
      Pair(model, null)

    null ->
      Pair(model, null)
  }
}

private fun stopOnViewport(model: Model): Pair<Model, Nothing?> {
  return Pair(
    model.copy(move = null),
    null
  )
}

private fun doMove(model: Model, msg: Msg.DoMove): Pair<Model, Nothing?> {
  return when (val move = model.move) {
    is ViewportMove.Node -> {
      val dx = msg.point.x - move.point.x
      val dy = msg.point.y - move.point.y

      val node = model.nodes[move.id] ?: return Pair(model, null)
      val newOffset = WorldPoint(node.offset.x + dx, node.offset.y + dy)
      val newNode = node.copy(offset = newOffset)
      val newNodes = model.nodes.put(newNode)

      val newMove = move.copy(point = msg.point)

      Pair(
        model.copy(nodes = newNodes, move = newMove),
        null
      )
    }

    is ViewportMove.Viewport -> {
      val dx = msg.point.x - move.point.x
      val dy = msg.point.y - move.point.y

      Pair(
        model.copy(
          transform = model.transform.copy(
            offset = WorldPoint(
              model.transform.offset.x + dx,
              model.transform.offset.y + dy
            )
          )
        ),
        null
      )
    }

    is ViewportMove.SourceJoint -> {
      Pair(
        model.copy(
          move = move.copy(end = msg.point)
        ),
        null
      )
    }

    null -> Pair(model, null)
  }
}

private fun triggerCompile(model: Model): Pair<Model, Cmd?> {
  // todo async compile
  val newCompiled = compile(model.types, model.nodes, model.joints)
  val newModel = if (newCompiled != model.compiled) {
    model.copy(
      compiled = newCompiled
    )
  } else {
    model
  }

  val data = JSON.stringify(PersistentModel(model.nodes, model.joints).toJson())
  return Pair(newModel, Cmd.LocalStoragePut(LOCAL_STORAGE_DATA_KEY, data))
}

private fun selectJoint(model: Model, msg: Msg.SelectJoint): Pair<Model, Cmd?> {
  return Pair(
    model.copy(selection = Selection.Joint(msg.joint)),
    null
  )
}

private fun selectNode(model: Model, msg: Msg.SelectNode): Pair<Model, Cmd?> {
  return Pair(
    model.copy(selection = Selection.Node(msg.node)),
    null
  )
}

fun clearSelection(model: Model): Pair<Model, Cmd?> {
  return Pair(
    model.copy(selection = null),
    null
  )
}

fun deleteSelected(model: Model): Pair<Model, Cmd?> {
  val newModel = when (model.selection) {
    is Selection.Joint ->
      model.copy(
        joints = model.joints.remove(model.selection.value.dest),
        selection = null
      )

    is Selection.Node ->
      model.copy(
        nodes = model.nodes.remove(model.selection.node),
        joints = model.joints
          .filterValues { it.source.first != model.selection.node && it.dest.first != model.selection.node },
        selection = null
      )

    null ->
      model
  }

  return triggerCompile(newModel)
}

private fun applyPersistentModel(model: Model, msg: Msg.ApplyPersistentModel): Pair<Model, Cmd?> {
  val json = JSON.stringify(msg.model.toJson())

  return Pair(
    model.copy(
      nodes = msg.model.nodes,
      joints = msg.model.joints,
      compiled = compile(model.types, msg.model.nodes, msg.model.joints)
    ),
    Cmd.LocalStoragePut(LOCAL_STORAGE_DATA_KEY, json)
  )
}

private fun parseData(model: Model, msg: Msg.ParseData): Pair<Model, Cmd?> {
  val data = try {
    msg.value?.let { persistenceModelFromJson(JSON.parse(it)) }
  } catch (ex: Throwable) {
    console.error(ex)
    null
  }

  return if (data != null)
    update(model, Msg.ApplyPersistentModel(data))
  else
    Pair(model, null)
}

private fun putNodeParam(model: Model, msg: Msg.PutNodeParam): Pair<Model, Cmd?> {
  val node = model.nodes[msg.node] ?: return Pair(model, null)
  val newModel = model.copy(
    nodes = model.nodes.put(
      node.copy(
        params = node.params.put(msg.param, msg.value)
      )
    )
  )
  return triggerCompile(newModel)
}

private fun addNode(model: Model, msg: Msg.AddNode): Pair<Model, Cmd?> {
  val type = model.types[msg.type] ?: return Pair(model, null)

  val defaultParams = type.params
    .associate { Pair(it.key, defaultValue(it.value)) }
    .toPersistentMap()

  val lastId = model.nodes.keys
    .map { it.value }
    .max()
    ?: 0

  val node = Node(
    id = NodeId(lastId + 1),
    type = msg.type,
    offset = msg.offset,
    params = defaultParams
  )
  val newModel = model.copy(
    nodes = model.nodes.put(node)
  )
  return triggerCompile(newModel)
}

private fun defaultValue(paramType: ParamType): DataValue {
  return when (paramType.type) {
    DataType.Scalar ->
      DataValue.Scalar(0.0f)
    DataType.Color ->
      DataValue.Color(0.0f, 0.0f, 0.0f, 0.0f)
  }
}

private suspend fun execute(cmd: Cmd, dispatch: Dispatch<Msg>): Unit = when (cmd) {
  is Cmd.LocalStorageGet -> {
    dispatch(cmd.msg(localStorage[cmd.key]))
  }
  is Cmd.LocalStoragePut -> {
    localStorage[cmd.key] = cmd.value
  }
}