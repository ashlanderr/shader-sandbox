@file:Suppress("PropertyName")

package gl

import org.khronos.webgl.Float32Array

external interface WebGLRenderingContext {
  val ARRAY_BUFFER: Int
  val COLOR_BUFFER_BIT: Int
  val COMPILE_STATUS: Int
  val FLOAT: Int
  val FRAGMENT_SHADER: Int
  val LINK_STATUS: Int
  val STATIC_DRAW: Int
  val TRIANGLES: Int
  val VERTEX_SHADER: Int

  fun clearColor(r: Float, g: Float, b: Float, a: Float)
  fun clear(bits: Int)
  fun createShader(type: Int): Int
  fun shaderSource(shader: Int, source: String)
  fun compileShader(shader: Int)
  fun createProgram(): Int
  fun attachShader(program: Int, shader: Int)
  fun linkProgram(program: Int)
  fun detachShader(program: Int, shader: Int)
  fun deleteShader(shader: Int)
  fun getProgramParameter(program: Int, param: Int): dynamic
  fun getProgramInfoLog(program: Int): String
  fun createBuffer(): Int
  fun enableVertexAttribArray(index: Int)
  fun bindBuffer(target: Int, buffer: Int)
  fun bufferData(buffer: Int, data: Float32Array, flags: Int)
  fun useProgram(program: Int)
  fun drawArrays(mode: Int, first: Int, count: Int)
  fun vertexAttribPointer(index: Int, size: Int, type: Int, normalized: Boolean, stride: Int, offset: Int)
  fun getShaderParameter(shader: Int, param: Int): dynamic
  fun getShaderInfoLog(shader: Int): String
  fun deleteBuffer(buffer: Int)
  fun deleteProgram(program: Int)
  fun getUniformLocation(program: Int, uniform: String): Int?
  fun uniform1f(uniform: Int, value: Float)
}