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
      ),
      joints = entityMapOf()
    ),
    Node(
      id = NodeId("3"),
      type = NodeTypeId("time"),
      offset = Point(0.0, 200.0),
      params = persistentMapOf(),
      joints = entityMapOf()
    ),
    Node(
      id = NodeId("4"),
      type = NodeTypeId("sin"),
      offset = Point(200.0, 200.0),
      params = persistentMapOf(),
      joints = entityMapOf(
        Joint(
          sourceNode = NodeId("3"),
          sourceOutput = OutputId.All,
          destInput = InputId("X")
        )
      )
    ),
    Node(
      id = NodeId("5"),
      type = NodeTypeId("const"),
      offset = Point(200.0, 300.0),
      params = persistentMapOf(
        ParamId("Value") to DataValue.Scalar(0.5f)
      ),
      joints = entityMapOf()
    ),
    Node(
      id = NodeId("6"),
      type = NodeTypeId("mul"),
      offset = Point(400.0, 200.0),
      params = persistentMapOf(),
      joints = entityMapOf(
        Joint(
          sourceNode = NodeId("4"),
          sourceOutput = OutputId.All,
          destInput = InputId("A")
        ),
        Joint(
          sourceNode = NodeId("5"),
          sourceOutput = OutputId.All,
          destInput = InputId("B")
        )
      )
    ),
    Node(
      id = NodeId("7"),
      type = NodeTypeId("const"),
      offset = Point(400.0, 300.0),
      params = persistentMapOf(
        ParamId("Value") to DataValue.Scalar(0.5f)
      ),
      joints = entityMapOf()
    ),
    Node(
      id = NodeId("8"),
      type = NodeTypeId("add"),
      offset = Point(600.0, 200.0),
      params = persistentMapOf(),
      joints = entityMapOf(
        Joint(
          sourceNode = NodeId("6"),
          sourceOutput = OutputId.All,
          destInput = InputId("A")
        ),
        Joint(
          sourceNode = NodeId("7"),
          sourceOutput = OutputId.All,
          destInput = InputId("B")
        )
      )
    ),
    Node(
      id = NodeId("9"),
      type = NodeTypeId("mul"),
      offset = Point(800.0, 0.0),
      params = persistentMapOf(),
      joints = entityMapOf(
        Joint(
          sourceNode = NodeId("1"),
          sourceOutput = OutputId.All,
          destInput = InputId("A")
        ),
        Joint(
          sourceNode = NodeId("8"),
          sourceOutput = OutputId.All,
          destInput = InputId("B")
        )
      )
    ),
    Node(
      id = NodeId("2"),
      type = NodeTypeId("color"),
      offset = Point(800.0, 400.0),
      params = persistentMapOf(
        ParamId("Value") to DataValue.Color(0.0f, 1.0f, 0.0f, 1.0f)
      ),
      joints = entityMapOf()
    ),
    Node(
      id = NodeId("10"),
      type = NodeTypeId("const"),
      offset = Point(600.0, 400.0),
      params = persistentMapOf(
        ParamId("Value") to DataValue.Scalar(1.0f)
      ),
      joints = entityMapOf()
    ),
    Node(
      id = NodeId("11"),
      type = NodeTypeId("sub"),
      offset = Point(800.0, 200.0),
      params = persistentMapOf(),
      joints = entityMapOf(
        Joint(
          sourceNode = NodeId("10"),
          sourceOutput = OutputId.All,
          destInput = InputId("A")
        ),
        Joint(
          sourceNode = NodeId("8"),
          sourceOutput = OutputId.All,
          destInput = InputId("B")
        )
      )
    ),
    Node(
      id = NodeId("12"),
      type = NodeTypeId("mul"),
      offset = Point(1000.0, 200.0),
      params = persistentMapOf(),
      joints = entityMapOf(
        Joint(
          sourceNode = NodeId("11"),
          sourceOutput = OutputId.All,
          destInput = InputId("A")
        ),
        Joint(
          sourceNode = NodeId("2"),
          sourceOutput = OutputId.All,
          destInput = InputId("B")
        )
      )
    ),
    Node(
      id = NodeId("13"),
      type = NodeTypeId("add"),
      offset = Point(1200.0, 100.0),
      params = persistentMapOf(),
      joints = entityMapOf(
        Joint(
          sourceNode = NodeId("9"),
          sourceOutput = OutputId.All,
          destInput = InputId("A")
        ),
        Joint(
          sourceNode = NodeId("12"),
          sourceOutput = OutputId.All,
          destInput = InputId("B")
        )
      )
    ),
    Node(
      id = RESULT_NODE_ID,
      type = RESULT_TYPE_ID,
      offset = Point(1400.0, 100.0),
      params = persistentMapOf(),
      joints = entityMapOf(
        Joint(
          sourceNode = NodeId("13"),
          sourceOutput = OutputId.All,
          destInput = RESULT_INPUT_COLOR
        )
      )
    )
  )
}