package app

import app.DataType.Color
import app.DataType.Scalar
import kotlinx.collections.immutable.persistentSetOf

val RESULT_NODE_ID = NodeId("result")
val RESULT_TYPE_ID = NodeTypeId("result")
val RESULT_INPUT_COLOR = InputId("Color")

const val NODE_RESULT_VAR = "#node_result"

val NODE_TYPES = entityMapOf(
  // constants
  NodeType(
    id = NodeTypeId("const"),
    name = "Constant",
    params = entityMapOf(
      ParamType(
        id = ParamId("Value"),
        type = Scalar
      )
    ),
    inputs = persistentSetOf(),
    outputs = persistentSetOf(
      OutputId.All
    ),
    globals = emptySet(),
    code = mapOf(
      listOf(Scalar) to listOf("float $NODE_RESULT_VAR = #paramValue;")
    )
  ),
  NodeType(
    id = NodeTypeId("color"),
    name = "Color",
    params = entityMapOf(
      ParamType(
        id = ParamId("Value"),
        type = Color
      )
    ),
    inputs = persistentSetOf(),
    outputs = persistentSetOf(
      OutputId.All
    ),
    globals = emptySet(),
    code = mapOf(
      listOf(Color) to listOf("vec4 $NODE_RESULT_VAR = #paramValue;")
    )
  ),
  // globals
  NodeType(
    id = NodeTypeId("time"),
    name = "Time",
    params = entityMapOf(),
    inputs = persistentSetOf(),
    outputs = persistentSetOf(
      OutputId.All
    ),
    globals = setOf(
      "uniform float time;"
    ),
    code = mapOf(
      listOf(Scalar) to listOf("float $NODE_RESULT_VAR = time;")
    )
  ),
  NodeType(
    id = NodeTypeId("position"),
    name = "Position",
    params = entityMapOf(),
    inputs = persistentSetOf(),
    outputs = persistentSetOf(
      OutputId.All
    ),
    globals = setOf(
      "varying vec4 position;"
    ),
    code = mapOf(
      listOf(Scalar) to listOf("vec4 $NODE_RESULT_VAR = position;")
    )
  ),
  NodeType(
    id = RESULT_TYPE_ID,
    name = "Result",
    params = entityMapOf(),
    inputs = persistentSetOf(
      RESULT_INPUT_COLOR
    ),
    outputs = persistentSetOf(),
    globals = emptySet(),
    code = emptyMap()
  ),
  // operations
  NodeType(
    id = NodeTypeId("add"),
    name = "Add",
    params = entityMapOf(),
    inputs = persistentSetOf(
      InputId("A"),
      InputId("B")
    ),
    outputs = persistentSetOf(
      OutputId.All
    ),
    globals = emptySet(),
    code = mapOf(
      listOf(Scalar, Scalar, Scalar) to listOf("float $NODE_RESULT_VAR = #inputA + #inputB;"),
      listOf(Scalar, Color, Color) to listOf("vec4 $NODE_RESULT_VAR = #inputA + #inputB;"),
      listOf(Color, Scalar, Color) to listOf("vec4 $NODE_RESULT_VAR = #inputA + #inputB;"),
      listOf(Color, Color, Color) to listOf("vec4 $NODE_RESULT_VAR = #inputA + #inputB;")
    )
  ),
  NodeType(
    id = NodeTypeId("sub"),
    name = "Subtract",
    params = entityMapOf(),
    inputs = persistentSetOf(
      InputId("A"),
      InputId("B")
    ),
    outputs = persistentSetOf(
      OutputId.All
    ),
    globals = emptySet(),
    code = mapOf(
      listOf(Scalar, Scalar, Scalar) to listOf("float $NODE_RESULT_VAR = #inputA - #inputB;"),
      listOf(Scalar, Color, Color) to listOf("vec4 $NODE_RESULT_VAR = #inputA - #inputB;"),
      listOf(Color, Scalar, Color) to listOf("vec4 $NODE_RESULT_VAR = #inputA - #inputB;"),
      listOf(Color, Color, Color) to listOf("vec4 $NODE_RESULT_VAR = #inputA - #inputB;")
    )
  ),
  NodeType(
    id = NodeTypeId("mul"),
    name = "Multiply",
    params = entityMapOf(),
    inputs = persistentSetOf(
      InputId("A"),
      InputId("B")
    ),
    outputs = persistentSetOf(
      OutputId.All
    ),
    globals = emptySet(),
    code = mapOf(
      listOf(Scalar, Scalar, Scalar) to listOf("float $NODE_RESULT_VAR = #inputA * #inputB;"),
      listOf(Scalar, Color, Color) to listOf("vec4 $NODE_RESULT_VAR = #inputA * #inputB;"),
      listOf(Color, Scalar, Color) to listOf("vec4 $NODE_RESULT_VAR = #inputA * #inputB;"),
      listOf(Color, Color, Color) to listOf("vec4 $NODE_RESULT_VAR = #inputA * #inputB;")
    )
  ),
  // trigonometry
  NodeType(
    id = NodeTypeId("sin"),
    name = "Sin",
    params = entityMapOf(),
    inputs = persistentSetOf(
      InputId("X")
    ),
    outputs = persistentSetOf(
      OutputId.All
    ),
    globals = emptySet(),
    code = mapOf(
      listOf(Scalar, Scalar) to listOf("float $NODE_RESULT_VAR = sin(#inputX);")
    )
  )
)

// todo implicit type cast from float to vec4