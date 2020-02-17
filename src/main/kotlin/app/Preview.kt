package app

import gl.WebGLRenderingContext
import io.akryl.component
import io.akryl.dom.html.Canvas
import io.akryl.useEffect
import io.akryl.useRef
import org.khronos.webgl.Float32Array
import org.w3c.dom.HTMLCanvasElement
import kotlin.browser.window

private const val VERTEX_SHADER = """
#version 100
precision highp float;

attribute vec4 vertex_position;

void main() {
  gl_Position = vertex_position;
}
"""

private const val FRAGMENT_SHADER = """
#version 100
precision mediump float;

uniform float time;

void main( void ) {
  vec4 node1_result = vec4(1, 0, 0, 1);
  float node3_result = time;
  float node4_result = sin(node3_result);
  float node5_result = float(0.5);
  float node6_result = node4_result * node5_result;
  float node7_result = float(0.5);
  float node8_result = node6_result + node7_result;
  vec4 node9_result = node1_result * node8_result;
  float node10_result = float(1);
  float node11_result = node10_result - node8_result;
  vec4 node2_result = vec4(0, 1, 0, 1);
  vec4 node12_result = node11_result * node2_result;
  vec4 node13_result = node9_result + node12_result;
  gl_FragColor = node13_result;
}
"""

private class Renderer private constructor(
  private val gl: WebGLRenderingContext,
  private val program: Int,
  private val buffer: Int
) {
  private val uniformTime = gl.getUniformLocation(program, "time")
  private var time = 0.0f

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
    gl.uniform1f(uniformTime, time)
    gl.bindBuffer(gl.ARRAY_BUFFER, buffer)
    gl.drawArrays(gl.TRIANGLES, 0, 6)

    time += 1.0f / 60.0f
  }

  fun dispose() {
    gl.deleteBuffer(buffer)
    gl.deleteProgram(program)
  }
}

fun mainPreview() = component {
  val ref = useRef<HTMLCanvasElement?>(null)

  useEffect(emptyList()) {
    ref.current?.let { canvas ->
      when (val renderer = Renderer.create(canvas, FRAGMENT_SHADER)) {
        is Ok -> {
          fun render() {
            window.requestAnimationFrame { render() }
            renderer.value.render()
          }
          render()
        }
        is Err -> console.error(renderer.error)
      }
    }
  }

  Canvas(
    width = 512,
    height = 512,
    ref = ref
  )
}