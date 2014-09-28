//Copyright (c) 2013 Nathan Stoddard (nstodda@purdue.edu); see LICENSE.txt

package com.nathanstoddard.common.renderer
import com.nathanstoddard._


/** A 2D renderer using LWJGL.
  *
  * Although it's built on OpenGL, it tries to hide most of its complexity. However, there are a few things you need to know about OpenGL to use this library. OpenGL uses things called shaders to specify how to render things on the screen, and there are two kinds: vertex and fragment shaders. One of each is combined to make a shader program, which in this library is called a GLProgram. Normally, you'll just use UntexturedProgram or TexturedProgram rather than writing your own (TexturedProgram isn't implemented yet). The input to the program is an array of vertices and indices into the vertex array, and a set of uniforms which are the same for every vertex. The former are represented in this library by a Mesh object. The latter use classes ending in Uniform. The builtin shaders only use MatrixUniform, but ColorUniform and RealUniform are provided in case you choose to write a program that requires them. Another thing you need to know is that only one program and texture can be active at once, and certain functins, especially Renderer.print, change them, so you have to reset them after printing text. Finally, you need to remember to use common.exit() to exit the application rather than returning from main or calling some other exit function. Otherwise the display, log, etc won't be closed properly.
  *
  * Here's a small example:
  * TODO: THIS EXAMPLE NO LONGER WORKS! Update it!
  * {{{
  * import common._
  * import common.renderer._
  * 
  * object Main extends InputHandler { //We extend InputHandler so we can handle user input.
  *   def main(args:Array[String]) {
  *     Renderer.create(width=800, height=600, title="renderer test", inputHandler=this, fps=60)
  *     val font = Font.dialog(15) //Dialog is a builtin Java font; renderer provides a wrapper around it
  *     val mesh = new UntexturedProgram.Mesh //A Mesh holds a set of triangles, lines, and points to be rendered
  *     val verts = Seq(Vec2.fromPolar(2,0), Vec2.fromPolar(2,tau/3), Vec2.fromPolar(2,tau*2/3)) //Creates some vertices, using polar coordinates.
  *     val coloredVerts = mesh.verts(verts zip Seq(Color.red, Color.green, Color.blue) :_*) //Adds some colored vertices to the mesh.
  *     mesh.polygon(coloredVerts.strip) //Adds a triangle to the mesh using the vertices.
  *     mesh.lines(coloredVerts.lineLoop) //Adds some lines around the triangle. Without this, the edges of the triangle would be jagged.
  *     val whiteVerts = mesh.verts(verts map ((_,Color.white)) :_*) //Adds the same vertices as before, but colored white.
  *     mesh.points(whiteVerts.strip) //Adds some points. Now the vertices of the triangle will be white.
  * 
  *     while (true) {
  *       UntexturedProgram.use() //This should be called before doing any rendering.
  *       val (viewMatrix, _) = Renderer.centerOn(Vec2.zero, 2.0) //Creates a matrix that scales the view appropriately to view the triangle.
  *       UntexturedProgram.matrix = viewMatrix //Sets the program's matrix. Without this, the matrix would default to the identity matrix.
  *       mesh.draw() //Draws the mesh.
  *       Renderer.print(font, Vec2.zero, "renderer test") //Prints some text.
  *       Renderer.updateScreen() //Updates the screen and delays for up to 1/60 of a second.
  *       Renderer.handleInput() //Handles key presses, mouse clicks, etc
  *     }
  * 
  *     exit() //Exits the application. It isn't really needed in this case, but it doesn't hurt.
  *   }
  *
  *   override def handleKeyPress(key:Key, char:Option[Char]) = exit() //When any key is pressed, quit.
  * }
  * }}}
  */

import scala.collection.mutable

import org.lwjgl
import lwjgl._
import lwjgl.opengl._
import lwjgl.input._
import lwjgl.opengl.GL11._
import lwjgl.opengl.GL12._
import lwjgl.opengl.GL13._
import lwjgl.opengl.GL15._
import lwjgl.opengl.GL20._
import lwjgl.opengl.GL21._
import lwjgl.opengl.GL30._
import lwjgl.opengl.EXTTextureCompressionS3TC._
import lwjgl.opengl.ARBTextureCompression._

import org.newdawn.slick.{Color=>_, _}

import java.io._
import java.nio._
import java.awt.image._
import javax.imageio._

import common._
import common.log._
import common.geom._
import common.io._

// TODO: support more texture formats, esp. transparency
class Texture(image:BufferedImage, smooth:Boolean) {
  def this(image:BufferedImage) = this(image, true)
  def this(file:File, smooth:Boolean=true) = this(ImageIO.read(file), smooth)

  Renderer.checkGLError("before loading texture")
  private val src = image.getRaster.getDataBuffer.asInstanceOf[DataBufferByte].getData
  private val pixels = BufferUtils.createByteBuffer(src.length).put(src, 0, src.length).flip().asInstanceOf[ByteBuffer]
  private val textures = BufferUtils.createIntBuffer(1)
  glGenTextures(textures)
  glBindTexture(GL_TEXTURE_2D, textures.get(0))
  glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
  glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)
  glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, if (smooth) GL_LINEAR_MIPMAP_LINEAR else GL_NEAREST_MIPMAP_NEAREST)
  glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, if (smooth) GL_LINEAR else GL_NEAREST)
  private val format = image.getType match {
    case BufferedImage.TYPE_4BYTE_ABGR => EXTAbgr.GL_ABGR_EXT
    case BufferedImage.TYPE_3BYTE_BGR => GL_BGR
    case x => Log.abort("Unsupported image format: " + x)
  }
  println("Loading texture of size " + IVec2(image.getWidth, image.getHeight))
  Renderer.checkGLError("before glTexImage2D")
  glTexImage2D(GL_TEXTURE_2D, 0, GL_COMPRESSED_RGB_S3TC_DXT1_EXT, image.getWidth, image.getHeight, 0, format, GL_UNSIGNED_BYTE, pixels)
  glGenerateMipmap(GL_TEXTURE_2D)
  Renderer.checkGLError("after loading texture")
  println("Compressed: " + glGetTexLevelParameteri(GL_TEXTURE_2D, 0, GL_TEXTURE_COMPRESSED_ARB))
  private val glID = textures.get(0)

  def bind(textureUnit:Int) {
    glActiveTexture(GL_TEXTURE0 + textureUnit);
    glBindTexture(GL_TEXTURE_2D, textures.get(0))
  }

  def destroy() {
    println("TODO: implement texture destruction")
  }
}
object Texture {
  def bindNone(textureUnit:Int) {
    glActiveTexture(GL_TEXTURE0 + textureUnit);
    glBindTexture(GL_TEXTURE_2D, 0)
  }
}





/** The most important object in this module. Initializing the renderer (by calling create) should be the first thing your program does.
  */
object Renderer {
  private var created = false
  private var ih:InputHandler = null
  private var fps:Int = -1
  private[renderer] def init(width:Int, height:Int, fullscreen:Boolean, title:String, inputHandler:InputHandler, fps:Int, backgroundColor:Color) {
    if (created)
      Log.abort("You can only initalize the renderer once!")
    created = true
    this.ih = inputHandler
    this.fps = fps

    atexit(this.destroy)
    val mode = if (fullscreen) {
      fullscreenModes.find(x => x.getWidth==width && x.getHeight==height) match {
        case Some(x) => x
        case None => Log.abort("Unable to find display mode of size: " + width + " x " + height)
      }
    }
    else new DisplayMode(width, height)

    Keyboard.enableRepeatEvents(true)
    Display.setTitle(title)
    Display.setDisplayMode(mode)
    if (fullscreen)
      Display.setFullscreen(true)
    Display.create()


    this.backgroundColor = backgroundColor
    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT)
    glEnable(GL_DEPTH_TEST)
    glEnable(GL_BLEND)
    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)

    checkGLError("End of Renderer.init")
  }
  /** Initialize the renderer in windowed mode.
    * @param width The width of the window
    * @param height The height of the window
    * @param title The title of the window
    * @param ih The object that'll handle input - usually you'll define the Main object as "object Main extends InputHandler" and implement some of InputHandler's methods within it
    * @param fps How many frames per second to render the window at
    */
  def init(width:Int, height:Int, title:String, inputHandler:InputHandler, fps:Int, backgroundColor:Color=Color.black):Unit = init(width, height, false, title, inputHandler, fps, backgroundColor)
  /** Initialize the renderer in fullscreen.
    * @param title The title of the window
    * @param ih The object that'll handle input - usually you'll define the Main object as "object Main extends InputHandler" and implement some of InputHandler's methods within it
    * @param fps How many frames per second to render the window at
    */
  def initFullscreen(title:String, inputHandler:InputHandler, fps:Int, backgroundColor:Color=Color.black) {
    val mode = fullscreenModes.head
    init(mode.getWidth, mode.getHeight, true, title, inputHandler, fps, backgroundColor)
  }

  private var backgroundColor_ :Color = Color.black
  def backgroundColor = backgroundColor_
  def backgroundColor_=(color:Color) {
    backgroundColor_ = color
    glClearColor(color.r.toFloat, color.g.toFloat, color.b.toFloat, color.a.toFloat)
  }

  def handleInput() {
    if (Display.isCloseRequested)
      exit(0)

    while (Keyboard.next()) {
      Keyboard.getEventKeyState match {
        case true => {
          val char = Keyboard.getEventCharacter
          ih.handleKeyPress(Key.fromLWJGLKey(Keyboard.getEventKey), if (char==Keyboard.CHAR_NONE) None else Some(char))
        }
        case false => ih.handleKeyRelease(Key.fromLWJGLKey(Keyboard.getEventKey))
      }
    }
    while (lwjgl.input.Mouse.next()) {
      val loc = IVec2(lwjgl.input.Mouse.getEventX, Display.getHeight-lwjgl.input.Mouse.getEventY)
      if (lwjgl.input.Mouse.getEventButton == -1)
        ih.handleMouseMove(loc, IVec2(lwjgl.input.Mouse.getEventDX, -lwjgl.input.Mouse.getEventDY))
      else if (lwjgl.input.Mouse.getEventButtonState)
        ih.handleMousePress(MouseButton.fromLWJGLButton(lwjgl.input.Mouse.getEventButton), loc)
      else
        ih.handleMouseRelease(MouseButton.fromLWJGLButton(lwjgl.input.Mouse.getEventButton), loc)
    }

    val wheel = lwjgl.input.Mouse.getDWheel / 120 //I have no idea why getDWheel always returns a multiple of 120
    if (wheel != 0)
      ih.handleScrollWheel(wheel)
  }

  var curProgram:GLProgram = null

  org.newdawn.slick.util.Log.setVerbose(false)

  /** A list of all possible fullscreen display modes. The largest ones are at the beginning of the list. If there's multiple modes
    * with the same size, selects the one with the largest BPP and refresh rate.
    */
  private[renderer] val fullscreenModes = Display.getAvailableDisplayModes.groupBy(mode => mode.getWidth*mode.getHeight).toSeq.sortBy(_._1).map(_._2).reverse.map {xs =>
    val maxBPP = xs.maxBy(_.getBitsPerPixel).getBitsPerPixel
    val maxFreq = xs.maxBy(_.getFrequency).getFrequency
    val xs2 = xs filter (x => x.getBitsPerPixel==maxBPP && x.getFrequency==maxFreq)
    xs2.size match {
      case 1 => xs2.head
      case _ => Log.abort("Invalid display mode!")
    }
  }

  def checkGLError(location:String) {
    val err = glGetError()
    if (err!=0) {
      Log(Error, "OpenGL error in " + location + ": " + org.lwjgl.opengl.Util.translateGLErrorString(err))
      common.exit(1)
    }
  }

  private[renderer] def ensureCreated() {
    if (!created)
      Log.abort("You must initalize the renderer by calling Renderer.create().")
  }

  private[renderer] def loadShader(typ:Int, shaderCode:String) = {
    ensureCreated()
    val shader = glCreateShader(typ)
    glShaderSource(shader, shaderCode)
    glCompileShader(shader)

    //Sometimes the next line has to use glGetShaderi, sometimes it has to use glGetShader. It probably depends on the LWJGL version.
    if (glGetShader(shader, GL_COMPILE_STATUS) != 1) {
      val info = glGetShaderInfoLog(shader,512)
      println("Error for shader:\n" + shaderCode)
      println(info)
      println()
    }

    Renderer.checkGLError("loadShader")
    shader
  }

  def destroy() {
    Display.destroy()
  }

  def makeIntBuffer(array:Array[Int]) = {
    val buf = BufferUtils.createIntBuffer(array.size)
    buf.put(array).position(0)
    buf
  }
  def makeFloatBuffer(array:Array[Float]) = {
    val buf = BufferUtils.createFloatBuffer(array.size)
    buf.put(array).position(0)
    buf
  }

  /** Updates the screen and delays by the appropriate amount to reach the desired FPS.
    */
  def updateScreen() {
    Display.update()
    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT)
    Display.sync(fps)
  }

  /** Restricts the mouse so it can't move outside the window. */
  def grabMouse() {
    lwjgl.input.Mouse.setGrabbed(true)
  }
  /** Allows the mouse to move outside the window again. */
  def ungrabMouse() {
    lwjgl.input.Mouse.setGrabbed(false)
  }

  def windowSize = IVec2(Display.getWidth, Display.getHeight)
  def windowWidth = Display.getWidth
  def windowHeight = Display.getHeight

  /** Sets up an orthographic projection in immediate mode. Intended for use by GUI elements such as text.
    */
  //TODO: this is called anytime a line of text is printed. That's inefficient.
  def orthoImmediate() {
    GLProgram.clearProgram()
    glLoadIdentity()
    glOrtho(0, Display.getWidth, Display.getHeight, 0, -1, 1)
    glDisable(GL_TEXTURE_2D)
  }

  /** Prints a line of text. Uses immediate mode.
    * @param font The font to use
    * @param pos The position of the top-left corner of the text, in pixels
    * @param text The text to print
    * @param color The color to make the text
    * @return The size of the text
    */
  def print(font:Font, pos:Vec2, text:String, color:Color=Color.white):Vec2 = {
    val program = curProgram
    orthoImmediate()
    org.newdawn.slick.opengl.TextureImpl.bindNone() //Slick assumes that I don't bind textures myself
    Texture.bindNone(0)
    font.font.drawString(pos.x.floor.toFloat, pos.y.floor.toFloat, text, new org.newdawn.slick.Color(
      color.r.toFloat, color.g.toFloat, color.b.toFloat, color.a.toFloat))
    Texture.bindNone(0)
    program.use()
    Vec2(font.width(text), font.height)
  }

  def printCentered(font:Font, pos:Vec2, text:String, color:Color=Color.white):Vec2 = {
    val program = curProgram
    orthoImmediate()
    org.newdawn.slick.opengl.TextureImpl.bindNone() //Slick assumes that I don't bind textures myself
    Texture.bindNone(0)
    font.font.drawString(pos.x.floor.toFloat - font.width(text)/2, pos.y.floor.toFloat - font.height/2, text, new org.newdawn.slick.Color(
      color.r.toFloat, color.g.toFloat, color.b.toFloat, color.a.toFloat))
    Texture.bindNone(0)
    if (program != null) program.use()
    Vec2(font.width(text), font.height)
  }

  /** Prints several lines of text. Uses immediate mode.
    * @param font The font to use
    * @param pos The position of the top-left corner of the text, in pixels
    * @param text The text to print
    * @return The height of the text
    */
  def print(font:Font, pos:Vec2, text:Any*):Real = {//Seq[(String,Color)]):Real = {
    var y = pos.y
    for (line <- text) {
      val str:String = line match {
        case (str:String,color:Color) => str
        case str:String => str
      }
      val color = line match {
        case (str:String,color:Color) => color
        case str:String => Color.white
      }
      val size = print(font, new Vec2(pos.x,y), str, color)
      y += size.y
    }
    font.font.getHeight * text.size
  }


  /** Word wraps some text.
    * @param font The font to use
    * @param width The maximum width of the rendered text
    * @param text The text to word wrap, in a tuple of (text, whether to indent subsequent lines, color)
    * @return Text suitable for being passed to print()
    */
  def wordwrap(font:Font, width:Real, text:Seq[(String,Boolean,Color)]):Seq[(String,Color)] = text.map {case (str,indent,color) =>
    StringUtils.wordwrap(str => font.font.getWidth(str)<=width, str, if (indent) "    " else "").map((_,color))
  }.flatten

  /** Takes some text and puts it in a data structure suitable for being passed to wordwrap.
    * @param text The text to format
    * @param indent Whether to indent subsequent lines
    * @param color The color to make the text
    */
  def formatForWrapping(text:Seq[String], indent:Boolean, color:Color) = text.map (text => (text,indent,color))
}




private[renderer] sealed abstract class Primitive {
  private[renderer] val glConstant:Int
}
private[renderer] case object Points extends Primitive {
  private[renderer] val glConstant = GL_POINTS
}
private[renderer] case object Lines extends Primitive {
  private[renderer] val glConstant = GL_LINES
}
private[renderer] case object LineStrip extends Primitive {
  private[renderer] val glConstant = GL_LINE_STRIP
}
private[renderer] case object LineLoop extends Primitive {
  private[renderer] val glConstant = GL_LINE_LOOP
}
private[renderer] case object Triangles extends Primitive {
  private[renderer] val glConstant = GL_TRIANGLES
}
private[renderer] case object TriangleStrip extends Primitive {
  private[renderer] val glConstant = GL_TRIANGLE_STRIP
}
private[renderer] case object TriangleFan extends Primitive {
  private[renderer] val glConstant = GL_TRIANGLE_FAN
}
//We don't include GL_QUADS or GL_POLYGON because they're deprecated.


/** Provides a hint to the renderer about how the vertex data will be used. Used for optimization. */
sealed abstract class VertexDataUsage {
  private[renderer] val glConstant:Int
}
object VertexDataUsage {
  /** The geometry will be created once, and will then be constant. */
  case object Static extends VertexDataUsage {
    private[renderer] val glConstant = GL_STATIC_DRAW
  }
  /** The geometry will usually be constant, but will change from time to time. */
  case object Dynamic extends VertexDataUsage {
    private[renderer] val glConstant = GL_DYNAMIC_DRAW
  }
  /** The geometry will change pretty much every frame. */
  case object Stream extends VertexDataUsage {
    private[renderer] val glConstant = GL_STREAM_DRAW
  }
}

class Font private(baseFont:java.awt.Font) {
  private[renderer] val font = new TrueTypeFont(baseFont, true)
  lazy val height = font.getHeight
  def width(str:String) = font.getWidth(str)
}
object Font {
  /** Loads a font from a TTF file.
    */
  def load(file:File, size:Int) = {
    val awtFont1 = java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT, file)
    val awtFont2 = awtFont1.deriveFont(java.awt.Font.PLAIN, size)
    new Font(awtFont2)
  }
  def serif(size:Int) = new Font(new java.awt.Font("Serif", java.awt.Font.PLAIN, size))
  def sansSerif(size:Int) = new Font(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, size))
  def monospaced(size:Int) = new Font(new java.awt.Font("Monospaced", java.awt.Font.PLAIN, size))
  def dialog(size:Int) = new Font(new java.awt.Font("Dialog", java.awt.Font.PLAIN, size))
  def dialogInput(size:Int) = new Font(new java.awt.Font("DialogInput", java.awt.Font.PLAIN, size))
  /** Looks much better than the default monospaced font, but not guaranteed to exist on every system. */
  //TODO: figure out how to check if the system supports a certain font
  def dejaVuSansMono(size:Int) = namedFont("DejaVu Sans Mono", size)
  /** Loads a named font. */
  def namedFont(name:String, size:Int) = new Font(new java.awt.Font(name, java.awt.Font.PLAIN, size))
}




/** A matrix given to the OpenGL program for rendering. */
class MatrixUniform(name:String, program:GLProgram) {
  Renderer.ensureCreated()
  if (program == null)
    Log.abort("You must set a program before creating a MatrixUniform. Use something like 'UntexturedProgram.use()'.")
  private val buf = BufferUtils.createFloatBuffer(16)
  private val handle = glGetUniformLocation(program.glProgram, name)
  private[renderer] var sourceMatrix:Matrix4 = null
  def :=(matrix:Matrix4) {
    program.use()
    sourceMatrix = matrix
    matrix.store(buf)
    buf.position(0)
    glUniformMatrix4(handle, false, buf)
  }
  this := Matrix4.identity
}


/** A color given to the OpenGL program for rendering. */
class ColorUniform(name:String, program:GLProgram) {
  Renderer.ensureCreated()
  if (program == null)
    Log.abort("You must set a program before creating a ColorUniform. Use something like 'UntexturedProgram.use()'.")
  private val buf = BufferUtils.createFloatBuffer(4)
  private val handle = glGetUniformLocation(program.glProgram, name)
  def :=(color:Color) {
    program.use()
    buf.put(color.asFloatArray)
    buf.position(0)
    glUniform4(handle, buf)
    Renderer.checkGLError("ColorBuffer setData")
  }
}

/** A real (floating point) given to the OpenGL program for rendering. */
class RealUniform(name:String, program:GLProgram) {
  Renderer.ensureCreated()
  if (program == null)
    Log.abort("You must set a program before creating a RealUniform. Use something like 'UntexturedProgram.use()'.")
  private val handle = glGetUniformLocation(program.glProgram, name)
  def :=(x:Real) = {
    program.use()
    glUniform1f(handle, x.toFloat)
  }
}

class Vec2Uniform(name:String, program:GLProgram) {
  Renderer.ensureCreated();
  if (program == null)
    Log.abort("You must set a program before creating a Vec2Uniform. Use something like 'UntexturedProgram.use()'.")
  private val handle = glGetUniformLocation(program.glProgram, name)
  def :=(x:Vec2) = {
    program.use()
    glUniform2f(handle, x.x.toFloat, x.y.toFloat)
  }
}

class Vec3Uniform(name:String, program:GLProgram) {
  Renderer.ensureCreated();
  if (program == null)
    Log.abort("You must set a program before creating a Vec3Uniform. Use something like 'UntexturedProgram.use()'.")
  private val handle = glGetUniformLocation(program.glProgram, name)
  def :=(x:Vec3) = {
    program.use()
    glUniform3f(handle, x.x.toFloat, x.y.toFloat, x.z.toFloat)
  }
}

class Vec4Uniform(name:String, program:GLProgram) {
  Renderer.ensureCreated();
  if (program == null)
    Log.abort("You must set a program before creating a Vec4Uniform. Use something like 'UntexturedProgram.use()'.")
  private val handle = glGetUniformLocation(program.glProgram, name)
  def :=(x:Vec4) = {
    program.use()
    glUniform4f(handle, x.x.toFloat, x.y.toFloat, x.z.toFloat, x.w.toFloat)
  }
}
