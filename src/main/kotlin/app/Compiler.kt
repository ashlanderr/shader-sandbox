package app

import kotlinx.collections.immutable.*

data class CompilerError(val nodeId: NodeId, val message: String)

data class CompiledShader(val lines: List<String>)

private data class OutputDesc(
  val variable: String,
  val type: DataType
)

private typealias CompiledCache = PersistentMap<NodeId, CompiledResult>

private typealias CompiledResult = Result<List<CompilerError>, CompiledNode>

private data class CompiledNode(
  val globals: Set<String>,
  val code: List<String>,
  val output: PersistentMap<OutputId, OutputDesc>
)

private data class OverloadResult(
  val code: List<String>,
  val replaces: List<Pair<String, String>>,
  val outputType: DataType
)

fun compile(
  types: EntityMap<NodeTypeId, NodeType>,
  nodes: EntityMap<NodeId, Node>,
  joints: EntityMap<Pair<NodeId, InputId>, Joint>
): Result<List<CompilerError>, CompiledShader> {
  val result = nodes[RESULT_NODE_ID]
    ?: return Err(listOf(CompilerError(RESULT_NODE_ID, "Result node not found")))

  if (result.type != RESULT_TYPE_ID)
    return Err(listOf(CompilerError(RESULT_NODE_ID, "Result node has wrong type")))

  val colorJoint = joints[Pair(RESULT_NODE_ID, RESULT_INPUT_COLOR)]
    ?: return Err(listOf(CompilerError(RESULT_NODE_ID, "Result 'Color' input is not connected")))

  val compiled = compileNode(colorJoint.source.first, types, nodes, joints)
  return compileToString(compiled, colorJoint)
}

private fun compileToString(compiled: CompiledCache, color: Joint): Result<List<CompilerError>, CompiledShader> {
  val outputNode = compiled[color.source.first]
    ?: return Err(listOf(CompilerError(RESULT_NODE_ID, "Color output not found")))

  when (outputNode) {
    is Ok -> {
      val output = outputNode.value.output[color.source.second]
        ?: return Err(listOf(CompilerError(RESULT_NODE_ID, "Color output not found")))

      val resultLine = when (output.type) {
        DataType.Scalar -> "gl_FragColor = vec4(${output.variable}, ${output.variable}, ${output.variable}, 1.0);"
        DataType.Color -> "gl_FragColor = vec4(${output.variable}.rgb, 1.0);"
      }

      val successfullyCompiled = compiled.values
        .filterIsInstance<Ok<CompiledNode>>()
        .map { it.value }

      val globals = successfullyCompiled.flatMapTo(HashSet()) { it.globals }
      val code = successfullyCompiled.flatMap { it.code } + resultLine

      val lines = listOf(
        "#version 100",
        "precision mediump float;",
        *globals.toTypedArray(),
        "void main( void ) {",
        *code.map { "  $it" }.toTypedArray(),
        "}"
      )

      return Ok(CompiledShader(lines))
    }

    is Err -> {
      return Err(
        compiled.values
          .flatMap { if (it is Err) it.error else emptyList() }
      )
    }
  }
}

private fun compileNode(
  nodeId: NodeId,
  types: EntityMap<NodeTypeId, NodeType>,
  nodes: EntityMap<NodeId, Node>,
  joints: EntityMap<Pair<NodeId, InputId>, Joint>,
  stack: PersistentList<NodeId> = persistentListOf(),
  compiled: CompiledCache = persistentMapOf()
): CompiledCache {
  if (nodeId in stack) {
    val stackStr = (stack + nodeId).joinToString(" -> ")
    val result = Err(listOf(CompilerError(nodeId, "Cyclic dependency found: $stackStr")))
    return compiled.put(nodeId, result)
  }

  if (nodeId in compiled)
    return compiled

  val node = nodes[nodeId]
  if (node == null) {
    val result = Err(listOf(CompilerError(nodeId, "Node not found")))
    return compiled.put(nodeId, result)
  }

  val type = types[node.type]
  if (type == null) {
    val result = Err(listOf(CompilerError(nodeId, "Type '${node.type}' not found")))
    return compiled.put(nodeId, result)
  }

  val newStack = stack.add(nodeId)

  val nodeJoints = joints
    .filter { it.key.first == nodeId }
    .associate { Pair(it.key.second, it.value) }

  val newCompiled = nodeJoints.values.fold(compiled) { acc, joint ->
    compileNode(joint.source.first, types, nodes, joints, newStack, acc)
  }

  val inputsResult = type.inputs
    .map { findInput(newCompiled, nodeJoints, node, it) }
    .flatten()
    .mapError { it.flatten() }

  val overloadResult = inputsResult.flatMap { inputs ->
    val code = type.code
      .mapNotNull { checkOverload(it, inputs) }
      .firstOrNull()
    if (code != null)
      Ok(code)
    else
      Err(listOf(CompilerError(node.id, "No suitable overload found")))
  }

  val paramsResult = overloadResult.flatMap { overload ->
    val withNode = overload.replaces + Pair("#node", "node${node.id}")
    type.params.map { buildParamReplace(node, it.value) }
      .flatten()
      .map { withNode + it }
      .map { overload.copy(replaces = it) }
  }

  val result = paramsResult.map { (code, inputs, outputType) ->
    val replaces = inputs + Pair("#node", "node${node.id}")
    val codeReplaced = replaces.fold(code) { acc, input ->
      acc.map { line ->
        line.replace(input.first, input.second)
      }
    }
    val variable = "node${node.id}_result"
    val output = buildOutput(outputType, variable)
    CompiledNode(
      globals = type.globals,
      code = codeReplaced,
      output = output
    )
  }

  return newCompiled.put(nodeId, result)
}

private fun buildOutput(outputType: DataType, variable: String): PersistentMap<OutputId, OutputDesc> {
  return when (outputType) {
    DataType.Scalar -> persistentMapOf(
      OutputId.All to OutputDesc(variable, outputType)
    )
    DataType.Color -> persistentMapOf(
      OutputId.All to OutputDesc(variable, outputType),
      OutputId.Red to OutputDesc("$variable.r", DataType.Scalar),
      OutputId.Green to OutputDesc("$variable.g", DataType.Scalar),
      OutputId.Blue to OutputDesc("$variable.b", DataType.Scalar),
      OutputId.Alpha to OutputDesc("$variable.a", DataType.Scalar)
    )
  }
}

private fun findInput(
  compiled: CompiledCache,
  joints: Map<InputId, Joint>,
  node: Node,
  input: InputId
): Result<List<CompilerError>, Pair<String, OutputDesc>> {
  val joint = joints[input]
    ?: return Err(listOf(CompilerError(node.id, "Input '$input' not connected")))

  val sourceNode = compiled[joint.source.first]
    ?: return Err(listOf(CompilerError(node.id, "Node for input '$input' not found")))

  return sourceNode
    .map { it.output[joint.source.second] }
    .flatMap { if (it == null) Err(emptyList<CompilerError>()) else Ok(it) }
    .map { Pair("#input${input.value}", it) }
    .mapError { emptyList<CompilerError>() }
}

private fun checkOverload(
  code: Map.Entry<List<DataType>, List<String>>,
  inputs: List<Pair<String, OutputDesc>>
): OverloadResult? {
  val overloadInputTypes = code.key.dropLast(1)
  val overloadReturnType = code.key.lastOrNull() ?: return null
  val realInputTypes = inputs.map { it.second.type }

  return if (overloadInputTypes == realInputTypes) {
    OverloadResult(
      code.value,
      inputs.map { Pair(it.first, it.second.variable) },
      overloadReturnType
    )
  } else {
    null
  }
}

private fun buildParamReplace(node: Node, param: ParamType): Result<CompilerError, Pair<String, String>> {
  val value = node.params[param.id]
    ?: return Err(CompilerError(node.id, "Parameter '${param.id}' not set"))

  if (value.type != param.type)
    return Err(CompilerError(node.id, "Parameter '${param.id}' has wrong type, expected: '${param.type}', got: '${value.type}'"))

  return Ok(
    Pair("#param${param.id}", value.toCode())
  )
}
