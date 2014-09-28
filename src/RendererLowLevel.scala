//Copyright (c) 2013 Nathan Stoddard (nstodda@purdue.edu); see LICENSE.txt

//Fairly low-level and old stuff. Don't use this in new code.

package com.nathanstoddard.common.renderer.lowLevel
import com.nathanstoddard._

import common._
import common.log._
import common.geom._
import renderer._

import org.lwjgl._
import org.lwjgl.opengl._
import org.lwjgl.opengl.GL11._
import org.lwjgl.opengl.GL15._
import org.lwjgl.opengl.GL20._
import org.lwjgl.opengl.GL21._

/*
//Wrappers around OpenGL VBOs
//The usage should be GL_<something>_DRAW
class VertexBuffer(val dat:Array[Float],private val usage:Int,private val stride:Int) {
  private val n = dat.size
  private val vbo = glGenBuffers()

  private val buffer = Renderer.makeFloatBuffer(dat)

  glBindBuffer(GL_ARRAY_BUFFER, vbo)
  glBufferData(GL_ARRAY_BUFFER, buffer, usage)

  //Make sure that this matches the usage given in the constructor
  //TODO: REMOVE THIS! IT'S INEFFICIENT! Modify 'dat' instead!
  def setData(startIndex:Long, dat:Array[Float]) {
    Log.assert(dat.size-startIndex <= n)
    glBindBuffer(GL_ARRAY_BUFFER, vbo)
    buffer.put(dat).position(0).limit(dat.size)
    glBufferSubData(GL_ARRAY_BUFFER, startIndex, buffer)
    //glBufferData(GL_ARRAY_BUFFER, buffer, usage)
  }

  def setData(dat:Array[Float]):Unit = setData(0, dat)

  def updateData() {
    glBindBuffer(GL_ARRAY_BUFFER, vbo)
    buffer.put(dat).position(0)//.limit(dat.size)
    //glBufferSubData(GL_ARRAY_BUFFER, startIndex, buffer)
    glBufferData(GL_ARRAY_BUFFER, buffer, usage)
  }

  //TODO: remove the requirement to 'bind' each VertexBuffer individually
  def bind(program:GLProgram, attrib:String, coordsPerVertex:Int, offset:Int) {
    val h = glGetAttribLocation(program.glProgram, attrib)
    glEnableVertexAttribArray(h)
    glBindBuffer(GL_ARRAY_BUFFER, vbo)
    glVertexAttribPointer(h, coordsPerVertex, GL_FLOAT, false, stride*4, offset*4)
  }
}

class ElementBuffer(dat:Array[Int],usage:Int) {
  require(usage == GL_STATIC_DRAW)
  val n = dat.size
  private val ibo = glGenBuffers()
  glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ibo)
  glBufferData(GL_ELEMENT_ARRAY_BUFFER, Renderer.makeIntBuffer(dat), usage)

  def draw(typ:Int, num:Int=n, start:Long=0) {
    glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ibo)
    glDrawElements(typ, num, GL_UNSIGNED_INT, start)
  }
}

class MatrixBuffer(program:GLProgram, varName:String) {
  val buf = BufferUtils.createFloatBuffer(16)
  val handle = glGetUniformLocation(program.glProgram, varName)
  def setData(matrix:Matrix4) {
    matrix.store(buf)
    buf.position(0)
    glUniformMatrix4(handle, false, buf)
  }
}

class ColorBuffer(program:GLProgram, varName:String) {
  val buf = BufferUtils.createFloatBuffer(4)
  val handle = glGetUniformLocation(program.glProgram, varName)
  def setData(color:Color) {
    buf.put(color.asFloatArray)
    buf.position(0)
    glUniform4(handle, buf)
  }
}

//This class is pretty much useless, but I'm keeping it for consistency.
class GLFloatBuffer(program:GLProgram, varName:String) {
  val handle = glGetUniformLocation(program.glProgram, varName)
  def setData(x:Float) = glUniform1f(handle, x)
}

class Vec2Buffer(program:GLProgram, varName:String) {
  val handle = glGetUniformLocation(program.glProgram, varName)
  def setData(a:Float, b:Float) = glUniform2f(handle, a, b)
}
*/
