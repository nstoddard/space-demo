//Copyright (c) 2013 Nathan Stoddard (nstodda@purdue.edu); see LICENSE.txt

package com.nathanstoddard.common.renderer
import com.nathanstoddard._

import scala.collection.mutable

import org.lwjgl
import org.lwjgl._
import org.lwjgl.opengl._
import org.lwjgl.input._
import org.lwjgl.opengl.GL11._
import org.lwjgl.opengl.GL12._
import org.lwjgl.opengl.GL13._
import org.lwjgl.opengl.GL15._
import org.lwjgl.opengl.GL20._
import org.lwjgl.opengl.GL21._

import org.newdawn.slick.{Color=>_, _}

import java.io._
import java.nio._
import java.awt.image._
import javax.imageio._

import common._
import common.geom._
import common.io._


/** Implemented by objects which should handle input. Usually implemented by the Main object.
  * Not all functions have to be overridden, but at least one should be, otherwise there will be no way to interact with the application.
  */
trait InputHandler {
  def handleKeyPress(key:Key, char:Option[Char]) {}
  def handleKeyRelease(key:Key) {}
  def handleMouseMove(pos:IVec2, movement:IVec2) {}
  def handleMousePress(button:MouseButton, pos:IVec2) {}
  def handleMouseRelease(button:MouseButton, pos:IVec2) {}
  def handleScrollWheel(wheelChange:Int) {}
}

object Mouse {
  //TODO: verify that we invert the y axis only when we should
  def position() = IVec2(lwjgl.input.Mouse.getX(), Display.getHeight-lwjgl.input.Mouse.getY())

  //Transforms from a viewport usable for GUI elements to the default OpenGL viewport
  private val guiTransform = Matrix4.scale(Vec2(1,-1)).translate(Vec2(-1,-1)).scale(Vec2(2.0/Display.getWidth, 2.0/Display.getHeight))
  //def getWorldPosition(pos:IVec2) = Renderer.curProgram.viewMatrix.inverse*guiTransform * pos
}

abstract class MouseButton private[renderer](private[renderer] lwjglButton:Int) {
  /** Whether the mouse button is currently pressed */
  def pressed = lwjgl.input.Mouse.isButtonDown(lwjglButton)
}
object MouseButton {
  private[renderer] def fromLWJGLButton(button:Int) = if (button == 0) Left
    else if (button == 1) Right
    else Unknown(button)
  /** The left mouse button */
  case object Left extends MouseButton(0)
  /** The right mouse button */
  case object Right extends MouseButton(1)
  /** A mouse button that the renderer doesn't know about */
  case class Unknown(button:Int) extends MouseButton(button)
}


/** Represents a keypress */
case class Key private[renderer](lwjglKeys:Set[Int]) {
  /** Whether the key is currently pressed */
  def pressed = lwjglKeys.exists(Keyboard.isKeyDown(_))
  def name = if (lwjglKeys.size==1) Keyboard.getKeyName(lwjglKeys.head)
    else if (lwjglKeys.size == 2 && lwjglKeys.contains(Keyboard.KEY_LSHIFT) && lwjglKeys.contains(Keyboard.KEY_RSHIFT)) "SHIFT"
    else "UNKNOWN KEY"
}

object Key {
  //When adding a key here, remember to also add it to fromLWJGLKey(), and fromChar() if applicable.
  /** This is provided so you can use keys not specified here without modifying the source code.
    */
  def apply(lwjglKey:Int):Key = Key(Set(lwjglKey))
  val A = Key(Keyboard.KEY_A)
  val B = Key(Keyboard.KEY_B)
  val C = Key(Keyboard.KEY_C)
  val D = Key(Keyboard.KEY_D)
  val E = Key(Keyboard.KEY_E)
  val F = Key(Keyboard.KEY_F)
  val G = Key(Keyboard.KEY_G)
  val H = Key(Keyboard.KEY_H)
  val I = Key(Keyboard.KEY_I)
  val J = Key(Keyboard.KEY_J)
  val K = Key(Keyboard.KEY_K)
  val L = Key(Keyboard.KEY_L)
  val M = Key(Keyboard.KEY_M)
  val N = Key(Keyboard.KEY_N)
  val O = Key(Keyboard.KEY_O)
  val P = Key(Keyboard.KEY_P)
  val Q = Key(Keyboard.KEY_Q)
  val R = Key(Keyboard.KEY_R)
  val S = Key(Keyboard.KEY_S)
  val T = Key(Keyboard.KEY_T)
  val U = Key(Keyboard.KEY_U)
  val V = Key(Keyboard.KEY_V)
  val W = Key(Keyboard.KEY_W)
  val X = Key(Keyboard.KEY_X)
  val Y = Key(Keyboard.KEY_Y)
  val Z = Key(Keyboard.KEY_Z)
  val Zero = Key(Keyboard.KEY_0)
  val One = Key(Keyboard.KEY_1)
  val Two = Key(Keyboard.KEY_2)
  val Three = Key(Keyboard.KEY_3)
  val Four = Key(Keyboard.KEY_4)
  val Five = Key(Keyboard.KEY_5)
  val Six = Key(Keyboard.KEY_6)
  val Seven = Key(Keyboard.KEY_7)
  val Eight = Key(Keyboard.KEY_8)
  val Nine = Key(Keyboard.KEY_9)
  val Escape = Key(Keyboard.KEY_ESCAPE)
  val Enter = Key(Keyboard.KEY_RETURN)
  val Left = Key(Keyboard.KEY_LEFT)
  val Right = Key(Keyboard.KEY_RIGHT)
  val Up = Key(Keyboard.KEY_UP)
  val Down = Key(Keyboard.KEY_DOWN)
  val Space = Key(Keyboard.KEY_SPACE)
  val Shift = Key(Set(Keyboard.KEY_LSHIFT, Keyboard.KEY_RSHIFT))
  val Ctrl = Key(Set(Keyboard.KEY_LCONTROL, Keyboard.KEY_RCONTROL))
  val F1 = Key(Keyboard.KEY_F1)
  val F2 = Key(Keyboard.KEY_F2)
  val F3 = Key(Keyboard.KEY_F3)
  val F4 = Key(Keyboard.KEY_F4)
  val F5 = Key(Keyboard.KEY_F5)
  val F6 = Key(Keyboard.KEY_F6)
  val F7 = Key(Keyboard.KEY_F7)
  val F8 = Key(Keyboard.KEY_F8)
  val F9 = Key(Keyboard.KEY_F9)
  val F10 = Key(Keyboard.KEY_F10)
  val F11 = Key(Keyboard.KEY_F11)
  val F12 = Key(Keyboard.KEY_F12)
  val Minus = Key(Keyboard.KEY_MINUS)
  val Equals = Key(Keyboard.KEY_EQUALS)
  val Comma = Key(Keyboard.KEY_COMMA)
  val Period = Key(Keyboard.KEY_PERIOD)
  val SysRq = Key(Keyboard.KEY_SYSRQ)
  val Unknown = Key(Set[Int]())
  private[renderer] def fromLWJGLKey(lwjglKey:Int) = lwjglKey match {
    case Keyboard.KEY_A => A
    case Keyboard.KEY_B => B
    case Keyboard.KEY_C => C
    case Keyboard.KEY_D => D
    case Keyboard.KEY_E => E
    case Keyboard.KEY_F => F
    case Keyboard.KEY_G => G
    case Keyboard.KEY_H => H
    case Keyboard.KEY_I => I
    case Keyboard.KEY_J => J
    case Keyboard.KEY_K => K
    case Keyboard.KEY_L => L
    case Keyboard.KEY_M => M
    case Keyboard.KEY_N => N
    case Keyboard.KEY_O => O
    case Keyboard.KEY_P => P
    case Keyboard.KEY_Q => Q
    case Keyboard.KEY_R => R
    case Keyboard.KEY_S => S
    case Keyboard.KEY_T => T
    case Keyboard.KEY_U => U
    case Keyboard.KEY_V => V
    case Keyboard.KEY_W => W
    case Keyboard.KEY_X => X
    case Keyboard.KEY_Y => Y
    case Keyboard.KEY_Z => Z
    case Keyboard.KEY_0 => Zero
    case Keyboard.KEY_1 => One
    case Keyboard.KEY_2 => Two
    case Keyboard.KEY_3 => Three
    case Keyboard.KEY_4 => Four
    case Keyboard.KEY_5 => Five
    case Keyboard.KEY_6 => Six
    case Keyboard.KEY_7 => Seven
    case Keyboard.KEY_8 => Eight
    case Keyboard.KEY_9 => Nine
    case Keyboard.KEY_ESCAPE => Escape
    case Keyboard.KEY_RETURN => Enter
    case Keyboard.KEY_LEFT => Left
    case Keyboard.KEY_RIGHT => Right
    case Keyboard.KEY_UP => Up
    case Keyboard.KEY_DOWN => Down
    case Keyboard.KEY_SPACE => Space
    case Keyboard.KEY_LSHIFT => Shift
    case Keyboard.KEY_RSHIFT => Shift
    case Keyboard.KEY_LCONTROL => Ctrl
    case Keyboard.KEY_RCONTROL => Ctrl
    case Keyboard.KEY_F1 => F1
    case Keyboard.KEY_F2 => F2
    case Keyboard.KEY_F3 => F3
    case Keyboard.KEY_F4 => F4
    case Keyboard.KEY_F5 => F5
    case Keyboard.KEY_F6 => F6
    case Keyboard.KEY_F7 => F7
    case Keyboard.KEY_F8 => F8
    case Keyboard.KEY_F9 => F9
    case Keyboard.KEY_F10 => F10
    case Keyboard.KEY_F11 => F11
    case Keyboard.KEY_F12 => F12
    case Keyboard.KEY_MINUS => Minus
    case Keyboard.KEY_EQUALS => Equals
    case Keyboard.KEY_COMMA => Comma
    case Keyboard.KEY_PERIOD => Period
    case Keyboard.KEY_SYSRQ => SysRq
    case _ => Unknown
  }
  /** Creates a key from a character. If it's a letter, it must be lowercase. */
  //TODO: add more characters here - it doesn't include minus, equals, comma, period, and others
  def fromChar(char:Char) = char.toLower match {
    case 'a' => A
    case 'b' => B
    case 'c' => C
    case 'd' => D
    case 'e' => E
    case 'f' => F
    case 'g' => G
    case 'h' => H
    case 'i' => I
    case 'j' => J
    case 'k' => K
    case 'l' => L
    case 'm' => M
    case 'n' => N
    case 'o' => O
    case 'p' => P
    case 'q' => Q
    case 'r' => R
    case 's' => S
    case 't' => T
    case 'u' => U
    case 'v' => V
    case 'w' => W
    case 'x' => X
    case 'y' => Y
    case 'z' => Z
    case '0' => Zero
    case '1' => One
    case '2' => Two
    case '3' => Three
    case '4' => Four
    case '5' => Five
    case '6' => Six
    case '7' => Seven
    case '8' => Eight
    case '9' => Nine
    case ' ' => Space
    case '\n' => Enter
    case _ => Unknown
  }
}
