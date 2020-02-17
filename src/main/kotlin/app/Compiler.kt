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
  val lines: List<String>,
  val output: PersistentMap<OutputId, OutputDesc>
)

private data class OverloadResult(
  val code: List<String>,
  val replaces: List<Pair<String, String>>,
  val outputType: DataType
)

fun compile(
  types: EntityMap<NodeTypeId, NodeType>,
  nodes: EntityMap<NodeId, Node>
): Result<List<CompilerError>, CompiledShader> {
  val result = nodes[RESULT_NODE_ID]
    ?: return Err(listOf(CompilerError(RESULT_NODE_ID, "Result node not found")))

  if (result.type != RESULT_TYPE_ID)
    return Err(listOf(CompilerError(RESULT_NODE_ID, "Result node has wrong type")))

  val colorJoint = result.joints[RESULT_INPUT_COLOR]
    ?: return Err(listOf(CompilerError(RESULT_NODE_ID, "Result 'Color' input is not connected")))

  val compiled = compileNode(colorJoint.sourceNode, types, nodes)
  return compileToString(compiled, colorJoint)
}

private fun compileToString(compiled: CompiledCache, color: Joint): Result<List<CompilerError>, CompiledShader> {
  val outputNode = compiled[color.sourceNode]
    ?: return Err(listOf(CompilerError(RESULT_NODE_ID, "Color output not found")))

  when (outputNode) {
    is Ok -> {
      val output = outputNode.value.output[color.sourceOutput]
        ?: return Err(listOf(CompilerError(RESULT_NODE_ID, "Color output not found")))

      val resultLine = when (output.type) {
        DataType.Scalar -> "gl_FragColor = vec4(${output.variable});"
        DataType.Color -> "gl_FragColor = ${output.variable};"
      }
      val lines = compiled.values
        .flatMap { if (it is Ok) it.value.lines else persistentListOf() }

      return Ok(CompiledShader(lines + resultLine))
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

  val newCompiled = node.joints.fold(compiled) { acc, joint ->
    compileNode(joint.value.sourceNode, types, nodes, newStack, acc)
  }

  val inputsResult = type.inputs
    .map { findInput(newCompiled, node, it) }
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
    val lines = replaces.fold(code) { acc, input ->
      acc.map { line ->
        line.replace(input.first, input.second)
      }
    }
    val output = persistentMapOf(
      // todo all outputs
      OutputId.All to OutputDesc(
        variable = "node${node.id}_result",
        type = outputType
      )
    )
    CompiledNode(lines, output)
  }

  return newCompiled.put(nodeId, result)
}

private fun findInput(compiled: CompiledCache, node: Node, input: InputId): Result<List<CompilerError>, Pair<String, OutputDesc>> {
  val joint = node.joints[input]
    ?: return Err(listOf(CompilerError(node.id, "Input '$input' not connected")))

  val sourceNode = compiled[joint.sourceNode]
    ?: return Err(listOf(CompilerError(node.id, "Node for input '$input' not found")))

  return sourceNode
    .map { it.output[joint.sourceOutput] }
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