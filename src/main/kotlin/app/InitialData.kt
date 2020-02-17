package app

import kotlinx.collections.immutable.persistentMapOf

val INITIAL_NODES by lazy {
  entityMapOf(
    Node(
      id = NodeId("1"),
      type = NodeTypeId("color"),
      offset = Point(600.0, 0.0),
      params = persistentMapOf(
        ParamId("Value") to DataValue.Color(0.0f, 0.0f, 1.0f, 1.0f)
      )
    ),
    Node(
      id = NodeId("3"),
      type = NodeTypeId("time"),
      offset = Point(0.0, 200.0),
      params = persistentMapOf()
    ),
    Node(
      id = NodeId("4"),
      type = NodeTypeId("sin"),
      offset = Point(200.0, 200.0),
      params = persistentMapOf()
    ),
    Node(
      id = NodeId("5"),
      type = NodeTypeId("const"),
      offset = Point(200.0, 300.0),
      params = persistentMapOf(
        ParamId("Value") to DataValue.Scalar(0.5f)
      )
    ),
    Node(
      id = NodeId("6"),
      type = NodeTypeId("mul"),
      offset = Point(400.0, 200.0),
      params = persistentMapOf()
    ),
    Node(
      id = NodeId("7"),
      type = NodeTypeId("const"),
      offset = Point(400.0, 300.0),
      params = persistentMapOf(
        ParamId("Value") to DataValue.Scalar(0.5f)
      )
    ),
    Node(
      id = NodeId("8"),
      type = NodeTypeId("add"),
      offset = Point(600.0, 200.0),
      params = persistentMapOf()
    ),
    Node(
      id = NodeId("9"),
      type = NodeTypeId("mul"),
      offset = Point(800.0, 0.0),
      params = persistentMapOf()
    ),
    Node(
      id = NodeId("2"),
      type = NodeTypeId("color"),
      offset = Point(800.0, 400.0),
      params = persistentMapOf(
        ParamId("Value") to DataValue.Color(0.0f, 1.0f, 0.0f, 1.0f)
      )
    ),
    Node(
      id = NodeId("10"),
      type = NodeTypeId("const"),
      offset = Point(600.0, 400.0),
      params = persistentMapOf(
        ParamId("Value") to DataValue.Scalar(1.0f)
      )
    ),
    Node(
      id = NodeId("11"),
      type = NodeTypeId("sub"),
      offset = Point(800.0, 200.0),
      params = persistentMapOf()
    ),
    Node(
      id = NodeId("12"),
      type = NodeTypeId("mul"),
      offset = Point(1000.0, 200.0),
      params = persistentMapOf()
    ),
    Node(
      id = NodeId("13"),
      type = NodeTypeId("add"),
      offset = Point(1200.0, 100.0),
      params = persistentMapOf()
    ),
    Node(
      id = RESULT_NODE_ID,
      type = RESULT_TYPE_ID,
      offset = Point(1400.0, 100.0),
      params = persistentMapOf()
    )
  )
}

val INITIAL_JOINTS by lazy {
  entityMapOf(
    Joint(
      source = Pair(NodeId("3"), OutputId.All),
      dest = Pair(NodeId("4"), InputId("X"))
    ),

    Joint(
      source = Pair(NodeId("4"), OutputId.All),
      dest = Pair(NodeId("6"), InputId("A"))
    ),
    Joint(
      source = Pair(NodeId("5"), OutputId.All),
      dest = Pair(NodeId("6"), InputId("B"))
    ),

    Joint(
      source = Pair(NodeId("6"), OutputId.All),
      dest = Pair(NodeId("8"), InputId("A"))
    ),
    Joint(
      source = Pair(NodeId("7"), OutputId.All),
      dest = Pair(NodeId("8"), InputId("B"))
    ),

    Joint(
      source = Pair(NodeId("1"), OutputId.All),
      dest = Pair(NodeId("9"), InputId("A"))
    ),
    Joint(
      source = Pair(NodeId("8"), OutputId.All),
      dest = Pair(NodeId("9"), InputId("B"))
    ),

    Joint(
      source = Pair(NodeId("10"), OutputId.All),
      dest = Pair(NodeId("11"), InputId("A"))
    ),
    Joint(
      source = Pair(NodeId("8"), OutputId.All),
      dest = Pair(NodeId("11"), InputId("B"))
    ),

    Joint(
      source = Pair(NodeId("11"), OutputId.All),
      dest = Pair(NodeId("12"), InputId("A"))
    ),
    Joint(
      source = Pair(NodeId("2"), OutputId.All),
      dest = Pair(NodeId("12"), InputId("B"))
    ),

    Joint(
      source = Pair(NodeId("9"), OutputId.All),
      dest = Pair(NodeId("13"), InputId("A"))
    ),
    Joint(
      source = Pair(NodeId("12"), OutputId.All),
      dest = Pair(NodeId("13"), InputId("B"))
    ),

    Joint(
      source = Pair(NodeId("13"), OutputId.All),
      dest = Pair(RESULT_NODE_ID, RESULT_INPUT_COLOR)
    )
  )
}