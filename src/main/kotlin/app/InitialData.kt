package app

import kotlinx.collections.immutable.persistentMapOf

val INITIAL_NODES by lazy {
  entityMapOf(
    Node(
      id = NodeId(1),
      type = NodeTypeId("Constant", "Color"),
      offset = Point(100.0, 100.0),
      params = persistentMapOf(
        ParamId("Value") to DataValue.Color(0.1f, 0.2f, 0.3f, 1.0f)
      )
    ),
    Node(
      id = RESULT_NODE_ID,
      type = RESULT_TYPE_ID,
      offset = Point(300.0, 100.0),
      params = persistentMapOf()
    )
  )
}

val INITIAL_JOINTS by lazy {
  entityMapOf(
    Joint(
      source = Pair(NodeId(1), OutputId.All),
      dest = Pair(RESULT_NODE_ID, RESULT_INPUT_COLOR)
    )
  )
}
