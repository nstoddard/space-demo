//Copyright (c) 2013 Nathan Stoddard (nstodda@purdue.edu); see LICENSE.txt

package com.nathanstoddard.common.renderer
import com.nathanstoddard._

import common._
import common.log._
import common.geom._
import common.io._
import common.random._

import scala.collection.mutable

import org.lwjgl._
import org.lwjgl.opengl._
import org.lwjgl.input._
import org.lwjgl.opengl.GL11._
import org.lwjgl.opengl.GL12._
import org.lwjgl.opengl.GL13._
import org.lwjgl.opengl.GL15._
import org.lwjgl.opengl.GL20._
import org.lwjgl.opengl.GL21._
import org.lwjgl.opengl.GL30._

import org.newdawn.slick.{Color=>_, _}

import java.io._
import java.nio._
import java.awt.image._
import javax.imageio._


class VertexShader(code:String) {
  def this(file:File) = this(FileUtils.read(file))
  private[renderer] val shader = Renderer.loadShader(GL_VERTEX_SHADER, code)
  private var destroyed = false
  private[renderer] def destroy() {
    if (!destroyed) {
      glDeleteShader(shader)
      destroyed = true
    }
  }
}

class FragmentShader(code:String) {
  def this(file:File) = this(FileUtils.read(file))
  private[renderer] val shader = Renderer.loadShader(GL_FRAGMENT_SHADER, code)
  private var destroyed = false
  private[renderer] def destroy() {
    if (!destroyed) {
      glDeleteShader(shader)
      destroyed = true
    }
  }
}

abstract class GLProgram(vertex:VertexShader, fragment:FragmentShader, attributes:Seq[(String,Int)]) {
  val glProgram = glCreateProgram()
  glAttachShader(glProgram, vertex.shader)
  glAttachShader(glProgram, fragment.shader)
  glBindFragDataLocation(glProgram, 0, "outColor");
  glLinkProgram(glProgram)
  this.use()

  def use() {
    if (Renderer.curProgram != this) {
      //println("Switching to " + this)
      glUseProgram(glProgram)
      Renderer.curProgram = this
    }
  }

  private[renderer] def setVertexAttribPointers() {
    Renderer.checkGLError("setVertexAttribPointers start")
    handles.foreach(glEnableVertexAttribArray(_))
    Renderer.checkGLError("setVertexAttribPointers after glEnableVertexAttribArray")
    for (i <- 0 until attributes.size) {
      glVertexAttribPointer(handles(i), attributes(i)._2, GL_FLOAT, false, stride*4, offsets(i)*4)
      Renderer.checkGLError("setVertexAttribPointers after " + i + ", " + handles(i) + ", " + attributes(i))
    }
  }

  def destroy() {
    glDeleteProgram(glProgram)
    fragment.destroy()
    vertex.destroy()
  }

  private[renderer] val stride = attributes.map(_._2).sum
  private val offsets = {
    var offset = 0
    for ((name,size) <- attributes) yield {
      val result = offset
      offset += size
      result
    }
  }
  
  private val handles = for (i <- 0 until attributes.size) yield {
    Renderer.checkGLError("Before Mesh.handles " + attributes(i)._1)
    val res = glGetAttribLocation(glProgram, attributes(i)._1)
    println(attributes(i),res)
    res
  }
}
//TODO: this should probably go in Renderer
object GLProgram {
  def clearProgram() {
    if (Renderer.curProgram != null) {
      glUseProgram(0)
      Renderer.curProgram = null
    }
  }
}
