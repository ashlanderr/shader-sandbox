package app

import io.akryl.redux.MsgAction
import io.akryl.redux.createStore
import kotlinx.collections.immutable.persistentListOf
import redux.StoreEnhancer

val store by lazy {
  createStore(
    init = Pair(
      Model(
        types = NODE_TYPES,
        nodes = INITIAL_NODES,
        joints = INITIAL_JOINTS,
        lines = persistentListOf(),
        move = null,
        compiled = compile(NODE_TYPES, INITIAL_NODES, INITIAL_JOINTS).orElse { null },
        selection = null
      ),
      null
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
    is Msg.StartMove -> startMove(model, msg)
    is Msg.StopMove -> stopMove(model)
    is Msg.DoMove -> doMove(model, msg)
    is Msg.PutNodeParam -> putNodeParam(model, msg)
    is Msg.SelectJoint -> selectJoint(model, msg)
    is Msg.ClearSelection -> clearSelection(model)
    is Msg.DeleteSelected -> deleteSelected(model)
  }
}

private suspend fun execute(cmd: Cmd): List<Msg> {
  return emptyList()
}

private fun setLines(model: Model, msg: Msg.SetLines): Pair<Model, Nothing?> {
  return Pair(
    model.copy(lines = msg.lines),
    null
  )
}

private fun startMove(model: Model, msg: Msg.StartMove): Pair<Model, Nothing?> {
  return Pair(
    model.copy(move = NodeMove(msg.node, msg.x, msg.y)),
    null
  )
}

private fun stopMove(model: Model): Pair<Model, Nothing?> {
  return Pair(
    model.copy(move = null),
    null
  )
}

private fun doMove(model: Model, msg: Msg.DoMove): Pair<Model, Nothing?> {
  val move = model.move ?: return Pair(model, null)
  val dx = msg.x - move.x
  val dy = msg.y - move.y

  val node = model.nodes[move.id] ?: return Pair(model, null)
  val newOffset = Point(node.offset.x + dx, node.offset.y + dy)
  val newNode = node.copy(offset = newOffset)
  val newNodes = model.nodes.put(newNode)

  val newMove = move.copy(x = msg.x, y = msg.y)

  return Pair(
    model.copy(nodes = newNodes, move = newMove),
    null
  )
}

private fun triggerCompile(model: Model): Pair<Model, Cmd?> {
  // todo async compile
  val newCompiled = compile(model.types, model.nodes, model.joints).orElse { err ->
    console.error(err)
    model.compiled
  }
  val newModel = if (newCompiled != model.compiled) {
    model.copy(
      compiled = newCompiled
    )
  } else {
    model
  }
  return Pair(newModel, null)
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

private fun selectJoint(model: Model, msg: Msg.SelectJoint): Pair<Model, Cmd?> {
  return Pair(
    model.copy(selection = Selection.Joint(msg.joint)),
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
        joints = model.joints.remove(model.selection.value.dest)
      )

    null ->
      model
  }

  return triggerCompile(newModel)
}
