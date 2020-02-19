package app

import app.DataType.Color
import app.DataType.Scalar
import kotlinx.collections.immutable.persistentSetOf

val RESULT_NODE_ID = NodeId(-1)
val RESULT_TYPE_ID = NodeTypeId("Global", "Result")
val RESULT_INPUT_COLOR = InputId("Color")

const val NODE_RESULT_VAR = "#node_result"

val NODE_TYPES = entityMapOf(
  // constants
  NodeType(
    id = NodeTypeId("Constant", "Scalar"),
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
    id = NodeTypeId("Constant", "Color"),
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
  // builders
  NodeType(
    id = NodeTypeId("Builder", "Color"),
    params = entityMapOf(),
    inputs = persistentSetOf(
      InputId("R"),
      InputId("G"),
      InputId("B"),
      InputId("A")
    ),
    outputs = persistentSetOf(
      OutputId.All
    ),
    globals = emptySet(),
    code = mapOf(
      listOf(Scalar, Scalar, Scalar, Scalar, Color) to listOf("vec4 $NODE_RESULT_VAR = vec4(#inputR, #inputG, #inputB, #inputA);")
    )
  ),
  // globals
  NodeType(
    id = NodeTypeId("Global", "Time"),
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
    id = NodeTypeId("Global", "Position"),
    params = entityMapOf(),
    inputs = persistentSetOf(),
    outputs = persistentSetOf(
      OutputId.All
    ),
    globals = setOf(
      "varying vec4 position;"
    ),
    code = mapOf(
      listOf(Color) to listOf("vec4 $NODE_RESULT_VAR = position;")
    )
  ),
  NodeType(
    id = RESULT_TYPE_ID,
    params = entityMapOf(),
    inputs = persistentSetOf(
      RESULT_INPUT_COLOR
    ),
    outputs = persistentSetOf(),
    globals = emptySet(),
    code = emptyMap()
  ),
  // math
  NodeType(
    id = NodeTypeId("Math", "Add"),
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
    id = NodeTypeId("Math", "Subtract"),
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
    id = NodeTypeId("Math", "Multiply"),
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
  NodeType(
    id = NodeTypeId("Math", "Divide"),
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
      listOf(Scalar, Scalar, Scalar) to listOf("float $NODE_RESULT_VAR = #inputA / #inputB;"),
      listOf(Scalar, Color, Color) to listOf("vec4 $NODE_RESULT_VAR = #inputA / #inputB;"),
      listOf(Color, Scalar, Color) to listOf("vec4 $NODE_RESULT_VAR = #inputA / #inputB;"),
      listOf(Color, Color, Color) to listOf("vec4 $NODE_RESULT_VAR = #inputA / #inputB;")
    )
  ),
  NodeType(
    id = NodeTypeId("Math", "Max"),
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
      listOf(Scalar, Scalar, Scalar) to listOf("float $NODE_RESULT_VAR = max(#inputA, #inputB);"),
      listOf(Color, Color, Color) to listOf("vec4 $NODE_RESULT_VAR = max(#inputA, #inputB);")
    )
  ),
  NodeType(
    id = NodeTypeId("Math", "Min"),
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
      listOf(Scalar, Scalar, Scalar) to listOf("float $NODE_RESULT_VAR = min(#inputA, #inputB);"),
      listOf(Color, Color, Color) to listOf("vec4 $NODE_RESULT_VAR = min(#inputA, #inputB);")
    )
  ),
  // vectors
  NodeType(
    id = NodeTypeId("Vector", "Distance"),
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
      listOf(Color, Color, Scalar) to listOf("float $NODE_RESULT_VAR = distance(#inputA, #inputB);")
    )
  ),
  // trigonometry
  NodeType(
    id = NodeTypeId("Trigonometry", "Sin"),
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
  ),
  NodeType(
    id = NodeTypeId("Trigonometry", "Cos"),
    params = entityMapOf(),
    inputs = persistentSetOf(
      InputId("X")
    ),
    outputs = persistentSetOf(
      OutputId.All
    ),
    globals = emptySet(),
    code = mapOf(
      listOf(Scalar, Scalar) to listOf("float $NODE_RESULT_VAR = cos(#inputX);")
    )
  )
)

// todo implicit type cast from float to vec4
// todo rename color to vec4
// todo add vec2, vec3
