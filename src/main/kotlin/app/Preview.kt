package app

import gl.WebGLRenderingContext
import io.akryl.component
import io.akryl.dom.html.Canvas
import io.akryl.useEffect
import io.akryl.useRef
import org.khronos.webgl.Float32Array
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import kotlin.browser.document
import kotlin.browser.window

private const val VERTEX_SHADER = """
#version 100
precision highp float;

attribute vec4 vertex_position;

varying vec4 position;

void main() {
  position = vertex_position;
  gl_Position = vertex_position;
}
"""

private const val DEFAULT_FRAGMENT_SHADER = """
#version 100
precision mediump float;

void main( void ) {
  gl_FragColor = vec4(0.0, 0.0, 0.0, 1.0);
}
"""

private interface UniformState {
  interface Factory {
    fun find(gl: WebGLRenderingContext, program: Int): UniformState?
  }

  fun update()
  fun apply(gl: WebGLRenderingContext)
}

private class TimeUniform(private val index: Int) : UniformState {
  companion object : UniformState.Factory {
    override fun find(gl: WebGLRenderingContext, program: Int) =
      gl.getUniformLocation(program, "time")?.let { TimeUniform(it) }
  }

  private var value = 0.0f

  override fun update() {
    value += 1.0f / 60.0f
  }

  override fun apply(gl: WebGLRenderingContext) {
    gl.uniform1f(index, value)
  }
}

private val UNIFORMS = listOf(
  TimeUniform
)

private class Renderer private constructor(
  private val gl: WebGLRenderingContext,
  private val program: Int,
  private val buffer: Int
) {
  private val uniforms = UNIFORMS
    .mapNotNull { it.find(gl, program) }

  companion object {
    fun create(canvas: HTMLCanvasElement, fragmentShaderSource: String): Result<String, Renderer> {
      val gl = canvas.getContext("webgl").unsafeCast<WebGLRenderingContext>()

      val vertexShader = gl.createShader(gl.VERTEX_SHADER)
      gl.shaderSource(vertexShader, VERTEX_SHADER)
      gl.compileShader(vertexShader)

      if (gl.getShaderParameter(vertexShader, gl.COMPILE_STATUS) == false) {
        val linkErrLog = gl.getShaderInfoLog(vertexShader)
        return Err("Vertex shader did not link successfully. Error log: $linkErrLog")
      }

      val fragmentShader = gl.createShader(gl.FRAGMENT_SHADER)
      gl.shaderSource(fragmentShader, fragmentShaderSource)
      gl.compileShader(fragmentShader)

      if (gl.getShaderParameter(fragmentShader, gl.COMPILE_STATUS) == false) {
        val linkErrLog = gl.getShaderInfoLog(fragmentShader)
        return Err("Fragment shader did not link successfully. Error log: $linkErrLog")
      }

      val program = gl.createProgram()
      gl.attachShader(program, vertexShader)
      gl.attachShader(program, fragmentShader)
      gl.linkProgram(program)
      gl.detachShader(program, vertexShader)
      gl.detachShader(program, fragmentShader)
      gl.deleteShader(vertexShader)
      gl.deleteShader(fragmentShader)

      if (gl.getProgramParameter(program, gl.LINK_STATUS) == false) {
        val linkErrLog = gl.getProgramInfoLog(program)
        return Err("Shader program did not link successfully. Error log: $linkErrLog")
      }

      val buffer = gl.createBuffer()
      val vertexData = Float32Array(arrayOf(
        -1.0f, -1.0f, 0.0f, 1.0f,
        1.0f, -1.0f, 0.0f, 1.0f,
        1.0f, 1.0f, 0.0f, 1.0f,
        1.0f, 1.0f, 0.0f, 1.0f,
        -1.0f, 1.0f, 0.0f, 1.0f,
        -1.0f, -1.0f, 0.0f, 1.0f
      ))
      gl.enableVertexAttribArray(0)
      gl.bindBuffer(gl.ARRAY_BUFFER, buffer)
      gl.bufferData(gl.ARRAY_BUFFER, vertexData, gl.STATIC_DRAW)
      gl.vertexAttribPointer(0, 4, gl.FLOAT, false, 0, 0)

      return Ok(Renderer(gl, program, buffer))
    }
  }

  fun render() {
    gl.useProgram(program)
    uniforms.forEach { it.apply(gl) }
    gl.bindBuffer(gl.ARRAY_BUFFER, buffer)
    gl.drawArrays(gl.TRIANGLES, 0, 6)

    uniforms.forEach { it.update() }
  }

  fun dispose() {
    gl.deleteBuffer(buffer)
    gl.deleteProgram(program)
  }
}

private val offscreenCanvas by lazy {
  val canvas = document.createElement("canvas") as HTMLCanvasElement
  canvas.width = 128
  canvas.height = 128
  canvas
}

fun smallShaderPreview(nodeId: NodeId) = component {
  Canvas(
    id = "node-shader-preview-$nodeId",
    width = 128,
    height = 128
  )
}

private var renderHandle: Int? = null
private var renderersHandle: List<Pair<NodeId, Renderer>>? = null
private var largeRendererHandle: Renderer? = null

fun shaderPreview(nodes: CompiledNodes) = component {
  // todo proper dispose

  val largeRef = useRef<HTMLCanvasElement?>(null)

  useEffect(listOf(nodes)) {
    renderHandle?.let { window.cancelAnimationFrame(it) }
    renderHandle = null
    renderersHandle?.let { it.forEach { r -> r.second.dispose() } }
    renderersHandle = null
    largeRendererHandle?.dispose()
    largeRendererHandle = null

    val renderers = nodes
      .mapNotNull { Pair(it.key, it.value.orElse { null }) }
      .mapNotNull { (id, node) ->
        Renderer.create(offscreenCanvas, node?.lines?.joinToString("\n") ?: DEFAULT_FRAGMENT_SHADER)
          .orElse { console.error(it); null }
          ?.let { Pair(id, it) }
      }
    renderersHandle = renderers

    val resultSource = nodes[RESULT_NODE_ID]
      ?.let { it.orElse { null } }
      ?.lines
      ?.joinToString("\n")
      ?: DEFAULT_FRAGMENT_SHADER

    val largeRenderer = largeRef.current
      ?.let { Renderer.create(it, resultSource) }
      ?.orElse { console.error(it); null }
    largeRendererHandle = largeRenderer

    fun render() {
      renderHandle = window.requestAnimationFrame { render() }
      for ((nodeId, renderer) in renderers) {
        val canvas = document.getElementById("node-shader-preview-$nodeId") as? HTMLCanvasElement ?: continue
        val ctx = canvas.getContext("2d") as? CanvasRenderingContext2D ?: continue
        renderer.render()
        ctx.drawImage(offscreenCanvas, 0.0, 0.0)
      }
      largeRenderer?.render()
    }

    render()
  }

  Canvas(
    width = 512,
    height = 512,
    ref = largeRef
  )
}