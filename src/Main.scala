import com.nathanstoddard._
import common._
import common.geom._
import common.random._
import common.log._
import renderer._

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
import io._

import java.util.Date
import java.text.SimpleDateFormat

import java.io._
import java.awt.image._
import javax.imageio._

import scala.collection.mutable

class Body[M](var pos:Vec3, var rot:Quat, val mesh:M, val program:GLProgram) {
  var vel = Vec3.zero
  var rotVel = Vec3.zero

  def simulate(dt:Real, gravity:Boolean) {
    if (gravity) {
      vel += Main.gravImpulse(this)*dt
    }

    pos += vel*dt

    val theta = rotVel * (dt/2)
    val dq = if (theta.lengthSquared*theta.lengthSquared/24 < 1.0e-10) {
      Quat(1 - theta.lengthSquared/2, theta * (1.0 - theta.lengthSquared/6.0))
    } else {
      Quat(Math.cos(theta.length), theta.normalize * Math.sin(theta.length))
    }

    rot = dq * rot
  }

  def rotate(amount:Real) = {    
    val theta = rotVel * (amount/2)
    val dq = if (theta.lengthSquared*theta.lengthSquared/24 < 1.0e-10) {
      Quat(1 - theta.lengthSquared/2, theta * (1.0 - theta.lengthSquared/6.0))
    } else {
      Quat(Math.cos(theta.length), theta.normalize * Math.sin(theta.length))
    }

    rot = dq * rot
  }

  // TODO: what exactly do these functions do?
  def drawMatrix = Matrix4.translate(pos) * rot.toMatrix
  def getMatrix = rot.toMatrix.translate(-pos)
}

object ObjReader {
  def read(path:String) = {
    Renderer.checkGLError("read start")
    val mesh = new LitProgram.Mesh(VertexDataUsage.Static)
    Renderer.checkGLError("read after creating mesh")
    val _verts = mutable.Queue[Vec3]();
    val verts = mutable.ArrayBuffer[MeshIndex]()

    val lines = FileUtils.read(path).split("\n")
    for (line <- lines) {
      line.head match {
        case 'v' if line(1) != 'n' =>
          Renderer.checkGLError("read vertex")
          val xs = line.drop(2).split(" ").filter(!_.isEmpty).map(java.lang.Double.parseDouble(_))
          _verts += Vec3(xs(0), xs(1), xs(2))
        case 'v' if line(1) == 'n' =>
        Renderer.checkGLError("read normal")
          val xs = line.drop(2).split(" ").filter(!_.isEmpty).map(java.lang.Double.parseDouble(_))
          val normal = Vec3(xs(0), xs(1), xs(2))
          val vert = mesh.verts(Seq((_verts.dequeue(), normal, Color.white)))
          verts += vert(0)
        case 'f' =>
          Renderer.checkGLError("read face")
          val xs = line.drop(2).split(" ").filter(!_.isEmpty)
          val xs2 = xs.map(x => x.take(x.indexOf('/')))
          val verts2 = xs2.map(x => verts(Integer.parseInt(x) - 1))
          mesh.polygon(verts2)
        case _ =>
      }
    }
    mesh
  }
}

object Shader {
  val fragment = new FragmentShader(new File("shaders/fragment.glsl"))
  val fragmentTextured = new FragmentShader(new File("shaders/fragmenttextured.glsl"))
  val fragmentTextured2 = new FragmentShader(new File("shaders/fragmenttextured2.glsl"))
  val litVertex = new VertexShader(new File("shaders/litvertex.glsl"))
  val litVertexTextured = new VertexShader(new File("shaders/litvertextextured.glsl"))
  val litVertexTextured2 = new VertexShader(new File("shaders/litvertextextured2.glsl"))
  val unlitVertex = new VertexShader(new File("shaders/unlitvertex.glsl"))
}

object UnlitProgram extends GLProgram(Shader.unlitVertex, Shader.fragment, Seq(("position",3), ("color",4))) {
  val _modelViewMatrix = new MatrixUniform("modelViewMatrix", this)
  val _projMatrix = new MatrixUniform("projMatrix", this)
  val _fcoef = new RealUniform("Fcoef", this)
  val _fcoefHalf = new RealUniform("Fcoef_half", this)
  class Mesh(usage:VertexDataUsage) extends renderer.Mesh(this, usage) {
    def verts(xs:Seq[(Vec3,Color)]) = _verts(xs.map{case (pos,color) => Seq(pos.x, pos.y, pos.z, color.r, color.g, color.b, color.a)})
    def draw(modelViewMatrix:Matrix4, projMatrix:Matrix4, fcoef:Real, fcoefHalf:Real) = {
      _modelViewMatrix := modelViewMatrix
      _projMatrix := projMatrix
      _fcoef := fcoef
      _fcoefHalf := fcoefHalf
      _draw()
    }
  }
}

object LitProgram extends GLProgram(Shader.litVertex, Shader.fragment, Seq(("position",3), ("normal",3), ("color",4))) {
  val _modelViewMatrix = new MatrixUniform("modelViewMatrix", this)
  val _projMatrix = new MatrixUniform("projMatrix", this)
  val _fcoef = new RealUniform("Fcoef", this)
  val _fcoefHalf = new RealUniform("Fcoef_half", this)
  val _lightDir = new Vec4Uniform("lightDir", this)
  val _lightColor = new ColorUniform("lightColor", this)
  val _ambientColor = new ColorUniform("ambientColor", this)
  class Mesh(usage:VertexDataUsage) extends renderer.Mesh(this, usage) {
    def verts(xs:Seq[(Vec3,Vec3,Color)]) = _verts(xs.map{case (pos,normal,color) =>
      Seq(pos.x, pos.y, pos.z, normal.x, normal.y, normal.z, color.r, color.g, color.b, color.a)})
    def draw(modelViewMatrix:Matrix4, projMatrix:Matrix4, lightDir:Vec4, lightColor:Color, ambientColor:Color, fcoef:Real, fcoefHalf:Real) = {
      _modelViewMatrix := modelViewMatrix
      _projMatrix := projMatrix
      _lightDir := lightDir
      _lightColor := lightColor
      _ambientColor := ambientColor
      _fcoef := fcoef
      _fcoefHalf := fcoefHalf
      _draw()
    }
  }
}

object LitProgramTextured extends GLProgram(Shader.litVertexTextured, Shader.fragmentTextured, Seq(("position",3), ("normal",3), ("color",4), ("texcoord",2))) {
  val _modelViewMatrix = new MatrixUniform("modelViewMatrix", this)
  val _projMatrix = new MatrixUniform("projMatrix", this)
  val _fcoef = new RealUniform("Fcoef", this)
  val _fcoefHalf = new RealUniform("Fcoef_half", this)
  val _lightDir = new Vec4Uniform("lightDir", this)
  val _lightColor = new ColorUniform("lightColor", this)
  class Mesh(usage:VertexDataUsage) extends renderer.Mesh(this, usage) {
    def verts(xs:Seq[(Vec3,Vec3,Color,Vec2)]) = _verts(xs.map{case (pos,normal,color,texcoord) =>
      Seq(pos.x, pos.y, pos.z, normal.x, normal.y, normal.z, color.r, color.g, color.b, color.a, texcoord.x, texcoord.y)})
    def draw(modelViewMatrix:Matrix4, projMatrix:Matrix4, lightDir:Vec4, lightColor:Color, fcoef:Real, fcoefHalf:Real) = {
      _modelViewMatrix := modelViewMatrix
      _projMatrix := projMatrix
      _lightDir := lightDir
      _lightColor := lightColor
      _fcoef := fcoef
      _fcoefHalf := fcoefHalf
      _draw()
    }
  }
}

object LitProgramTextured2 extends GLProgram(Shader.litVertexTextured2, Shader.fragmentTextured2, Seq(("position",3), ("normal",3), ("color",4), ("texcoord",2))) {
  val _modelViewMatrix = new MatrixUniform("modelViewMatrix", this)
  val _projMatrix = new MatrixUniform("projMatrix", this)
  val _fcoef = new RealUniform("Fcoef", this)
  val _fcoefHalf = new RealUniform("Fcoef_half", this)
  val _lightDir = new Vec4Uniform("lightDir", this)
  val _lightColor = new ColorUniform("lightColor", this)
  val _ambientColor = new ColorUniform("ambientColor", this)
  class Mesh(usage:VertexDataUsage) extends renderer.Mesh(this, usage) {
    def verts(xs:Seq[(Vec3,Vec3,Color,Vec2)]) = _verts(xs.map{case (pos,normal,color,texcoord) =>
      Seq(pos.x, pos.y, pos.z, normal.x, normal.y, normal.z, color.r, color.g, color.b, color.a, texcoord.x, texcoord.y)})
    def draw(modelViewMatrix:Matrix4, projMatrix:Matrix4, lightDir:Vec4, lightColor:Color, ambientColor:Color, fcoef:Real, fcoefHalf:Real) = {
      _modelViewMatrix := modelViewMatrix
      _projMatrix := projMatrix
      _lightDir := lightDir
      _lightColor := lightColor
      _ambientColor := ambientColor
      _fcoef := fcoef
      _fcoefHalf := fcoefHalf
      _draw()
    }
  }
}

object Main extends InputHandler {
  val fps = 120
  val dt = 1.0/fps

  val startFov = tau/6.0
  var fov = startFov

  val defaultFlySpeed = 100.0
  var flySpeed = defaultFlySpeed

  def rotSpeed = 1.44 * fov/startFov
  val keyRotSpeed = 2.0

  val earthRadius = 6.37e+6
  val orbitHeight = 424.1e+3
  val earthPos = Vec3(earthRadius+orbitHeight,0,0)
  val earthMass = 5.97e+24


  val g = 6.67e-11

  val dayLength = 60 * 60 * 24

  val moonPos = Vec3(0,0,-381435e+3)
  val moonRadius = 1737.5e+3
  val moonMass = 7.349e+22

  val sunPos = Vec3(-1.521e+11,0,0)
  val sunRadius = 695.5e+6
  val sunColor = Color(toReal(0xff)/255, toReal(0xf3)/255, toReal(0xea)/255)

  val starDist = 1e+16

  val useSmallTextures = false
  val fullscreen = false
  val projRadius = 0.1

  val weaponCooldown = 0.5

  val defaultProjSpeed = 10.0
  val defaultProjColor = Color.green
  var projSpeed = defaultProjSpeed
  var projColor = defaultProjColor



  val you = new Body(Vec3.zero, Quat.one, null, null)
  you.vel = Vec3(0,0,speedAtHeight(orbitHeight + earthRadius))
  val projs = mutable.Set[Body[LitProgram.Mesh]]()
  val planets = mutable.Set[Body[LitProgramTextured.Mesh]]()
  val farPlanets = mutable.Set[Body[UnlitProgram.Mesh]]()
  val moons = mutable.Set[Body[LitProgramTextured2.Mesh]]()

  var earthDayTexture:Texture = _
  var earthNightTexture:Texture = _
  var moonTexture:Texture = _

  def moveInDir(dist:Real, dir:Vec3) = (you.rot * Quat.fromAngleAxis(tau/4, dir)).rotate(Vec3(dist,0,0))

  def makeSphere(r:Real, isStar:Boolean, isMoon:Boolean) = {
    Renderer.checkGLError("makeSphere start")
    val mesh = if (isStar) new UnlitProgram.Mesh(VertexDataUsage.Static)
      else if (isMoon) new LitProgramTextured2.Mesh(VertexDataUsage.Static)
      else new LitProgramTextured.Mesh(VertexDataUsage.Static)
    var theta = 0.0
    val diff = tau/360
    def next(x:Real) = x + diff
    def point(theta:Real, phi:Real) = Vec3(
      r * Math.sin(theta) * Math.cos(phi),
      r * Math.sin(theta) * Math.sin(phi),
      r * Math.cos(theta)
    )
    def texAt(theta:Real, phi:Real) = Vec2(phi/tau, theta/(tau*0.5))
    while (theta < tau*0.5) {
      var phi = 0.0
      while (phi < tau) {
        val a = point(theta, phi)
        val b = point(next(theta), phi)
        val c = point(next(theta), next(phi))
        val d = point(theta, next(phi))
        val at = texAt(theta, phi)
        val bt = texAt(next(theta), phi)
        val ct = texAt(next(theta), next(phi))
        val dt = texAt(theta, next(phi))
        def planetColorAt(x:Vec3) = if (isStar) sunColor else Color.white
        val verts = if (isStar) mesh.asInstanceOf[UnlitProgram.Mesh].verts(Seq(a, b, c, d).map(x => (x, planetColorAt(x))))
        else if (isMoon) mesh.asInstanceOf[LitProgramTextured2.Mesh].verts(Seq((a,at), (b,bt), (c,ct), (d,dt)).map{case (x,y) => (x, x, planetColorAt(x), y)})
        else mesh.asInstanceOf[LitProgramTextured.Mesh].verts(Seq((a,at), (b,bt), (c,ct), (d,dt)).map{case (x,y) => (x, x, planetColorAt(x), y)})
        mesh.polygon(verts)

        phi = next(phi)
      }
      theta = next(theta)
    }
    mesh
  }

  def gravImpulseFrom[M](x:Body[M], bodyPos:Vec3, bodyMass:Real) = (bodyPos-x.pos).normalize * (g * bodyMass / (x.pos distSquared bodyPos))
  def gravImpulse[M](x:Body[M]) = gravImpulseFrom(x, earthPos, earthMass) + gravImpulseFrom(x, moonPos, moonMass)
  def speedAtHeight(r:Real) = Math.sqrt(g * earthMass / r)

  def main(args:Array[String]) {
    if (fullscreen) Renderer.initFullscreen("Space combat", this, fps)
      else Renderer.init(800, 600, "Space combat", this, fps)

    Renderer.checkGLError("before loading font")

    val font = Font.dialog(15)
    val largeFont = Font.dialog(30)


    Renderer.printCentered(largeFont, Vec2(Renderer.windowWidth/2, Renderer.windowHeight/2),
      "Loading textures. This may take a while...")

    Renderer.updateScreen()


    val is64Bit = System.getProperty("sun.arch.data.model") == "64"

    val maxTexSize = glGetInteger(GL_MAX_TEXTURE_SIZE)
    println("Maximum texture size: " + maxTexSize)

    val texSize = if (maxTexSize >= 16200 && !useSmallTextures && is64Bit) 16200
      else if (maxTexSize >= 8192 && !useSmallTextures && is64Bit) 8192
      else 4096
    val (dayTex,nightTex) = if (texSize >= 16200) ("1_earth_16k.jpg", "5_night_16k.jpg")
      else if (texSize >= 8192) ("1_earth_8k.jpg", "5_night_8k.jpg")
      else ("1_earth_4k.jpg", "5_night_4k.jpg")
    val moonTex = if (texSize >= 8192) "moon_8k_color_brim16.jpg"
      else "moon_4k_color_brim16.jpg"

    var texSizeString = "Texture size: " + texSize
    if (!is64Bit && !useSmallTextures && texSize < maxTexSize) texSizeString += " (use 64-bit JVM for larger textures)"

    LitProgramTextured2.use()
    moonTexture = new Texture(new File("textures/" + moonTex), true)
    glUniform1i(glGetUniformLocation(LitProgramTextured2.glProgram, "tex"), 0);
    Renderer.checkGLError("after loading moon tex")

    LitProgramTextured.use()
    earthDayTexture = new Texture(new File("textures/" + dayTex), true)
    glUniform1i(glGetUniformLocation(LitProgramTextured.glProgram, "dayTex"), 0);
    Renderer.checkGLError("after loading day tex")

    earthNightTexture = new Texture(new File("textures/" + nightTex), true) // TODO: this doesn't include clouds
    glUniform1i(glGetUniformLocation(LitProgramTextured.glProgram, "nightTex"), 1);
    Renderer.checkGLError("after loading night tex")

    // This allows you to see essentially forever (1e+25 units away). It could possibly cause z-fighting issues, but I haven't seen any.
    val fcoef = 2.0 / (Math.log(1e+25 + 1.0) / Math.log(2.0))
    val fcoefHalf = fcoef*0.5

    Renderer.grabMouse()

    val fpsLogger = new FPSLogger(1.0)

    Renderer.checkGLError("before loading ship1")

    val objBody = new Body(Vec3(0,0,-10), Quat.one, ObjReader.read("ship1.obj"), LitProgram)
    objBody.vel = Vec3(0,0,speedAtHeight(orbitHeight + earthRadius))

    Renderer.checkGLError("after loading ship1")

    val earth = new Body(earthPos, Quat.one, makeSphere(earthRadius, false, false).asInstanceOf[LitProgramTextured.Mesh], LitProgramTextured)
    earth.rotVel = Vec3(0,0,tau/dayLength)
    planets += earth
    val moon = new Body(moonPos, Quat.one, makeSphere(moonRadius, false, true).asInstanceOf[LitProgramTextured2.Mesh], LitProgramTextured2)
    moons += moon
    farPlanets += new Body(sunPos, Quat.one, makeSphere(sunRadius, true, false).asInstanceOf[UnlitProgram.Mesh], UnlitProgram)


    val random = new java.util.Random()
    val skyboxMesh = new UnlitProgram.Mesh(VertexDataUsage.Static)
    for (i <- 0 until 5000) {
      val point = Vec3(random.nextGaussian(), random.nextGaussian(), random.nextGaussian()).normalize * starDist
      val vert = skyboxMesh.verts(Seq((point, getStarColor())))
      skyboxMesh.point(vert(0))
    }

    var cooldown = 0.0

    while (true) {
      Renderer.checkGLError("Main loop")
      fpsLogger.update()

      val _lightColor = sunColor

      val lightDir = Vec4(-1,0,0,0)
      val lightColorAmb = _lightColor*0.9
      val ambientColor = _lightColor*0.1
      val lightColorNoAmb = _lightColor

      val projMatrix = Matrix4.perspective(fov, toReal(Renderer.windowWidth)/Renderer.windowHeight)


      skyboxMesh.draw(you.rot.toMatrix, projMatrix, fcoef, fcoefHalf)

      var walkDir = Vec3.zero
      if (Key.A.pressed) walkDir += moveInDir(-1, Vec3.xAxis)
      if (Key.D.pressed) walkDir += moveInDir(1,  Vec3.xAxis)
      if (Key.W.pressed) walkDir += moveInDir(1,  Vec3.yAxis)
      if (Key.S.pressed) walkDir += moveInDir(-1, Vec3.yAxis)
      if (Key.Shift.pressed) walkDir += moveInDir(1, Vec3.zAxis)
      if (Key.Ctrl.pressed) walkDir += moveInDir(-1, Vec3.zAxis)
      if (Key.Q.pressed) you.rotVel += you.rot * Vec3(0, 0, keyRotSpeed*rotSpeed*dt)
      if (Key.E.pressed) you.rotVel += you.rot * Vec3(0, 0, -keyRotSpeed*rotSpeed*dt)
      if (Key.T.pressed) earth.rotate(60*30*dt)
      if (Key.G.pressed) earth.rotate(-60*30*dt)

      cooldown -= dt
      if (Key.Space.pressed && cooldown <= 0) {
        cooldown = weaponCooldown
        val mesh = new LitProgram.Mesh(VertexDataUsage.Static)
        val centralSquare = Seq(Vec3(1,0,0), Vec3(0,1,0), Vec3(-1,0,0), Vec3(0,-1,0), Vec3(1,0,0)).map(_*projRadius)
        val top = Vec3(0,0,1)*projRadius
        val bottom = Vec3(0,0,-1)*projRadius
        for (i <- 0 until 4) {
          val vertsTop = mesh.verts(Seq(top, centralSquare(i), centralSquare(i+1)).map(vert => (vert,vert,projColor)))
          val vertsBot = mesh.verts(Seq(bottom, centralSquare(i), centralSquare(i+1)).map(vert => (vert,vert,projColor)))
          mesh.polygon(vertsTop.strip)
          mesh.polygon(vertsBot.strip)
        }
        val body = new Body(you.pos, you.rot, mesh, LitProgram)
        body.vel = you.vel + you.rot.rotate(Vec3(0,0,-projSpeed))
        projs += body

      }

      if (Key.R.pressed) {
        fov /= 1.005
        println(round(fov))
      }
      if (Key.F.pressed) {
        fov *= 1.005
        println(round(fov))
      }

      var walkDist = if (walkDir == Vec3.zero) Vec3.zero else walkDir.normalize * (flySpeed*dt)
      you.vel += walkDist

      def objects = Seq(you, objBody) ++ projs.toSeq

      objects.foreach(_.simulate(dt, true))
      planets.foreach(_.simulate(dt, false))

      val matrix = you.getMatrix

      for (star <- farPlanets) star.mesh.draw(matrix*star.drawMatrix, projMatrix, fcoef, fcoefHalf)

      moonTexture.bind(0)
      for (moon <- moons) moon.mesh.draw(matrix*moon.drawMatrix, projMatrix, matrix*lightDir, lightColorAmb*1.5, ambientColor*0.5, fcoef, fcoefHalf)

      earthDayTexture.bind(0)
      earthNightTexture.bind(1)
      for (planet <- planets) planet.mesh.draw(matrix*planet.drawMatrix, projMatrix, matrix*lightDir, lightColorNoAmb, fcoef, fcoefHalf)

      for (obj <- objects if obj.mesh != null) obj.mesh.draw(matrix*obj.drawMatrix, projMatrix, matrix*lightDir, lightColorAmb, ambientColor, fcoef, fcoefHalf)

      def round2(x:Vec3) = "(" + round(x.x) + ", " + round(x.y) + ", " + round(x.z) + ")"

      glDisable(GL_DEPTH_TEST)
      Renderer.print(font, Vec2.zero,
        "pos: " + round2(you.pos/1000) + " km",
        "pos': " + round2(you.vel/1000) + " km/s",
        "speed: " + round(you.vel.length/1000) + " km/s",
        "rot: " + you.rot.round2,
        "rot': " + round2(you.rotVel),
        "dist to earth: " + round((earthPos.dist(you.pos) - earthRadius)/1000) + " km",
        "dist to moon: " + round((moonPos.dist(you.pos) - moonRadius)/1000) + " km",
        "dist to sun: " + round((sunPos.dist(you.pos) - sunRadius)/1000) + " km",
        texSizeString
      )
      glEnable(GL_DEPTH_TEST)

      Renderer.updateScreen()
      Renderer.handleInput()
    }

    exit()
  }

  def getStarColor() = {
    Color(1 - Math.sqrt(Random.nextReal())) //This ensures that there's more dim stars than bright ones
  }

  override def handleKeyPress(key:Key, char:Option[Char]) = key match {
    case Key.Escape => exit(0)
    case Key.One =>
      projSpeed = defaultProjSpeed/3
      projColor = Color.blue
      flySpeed = defaultFlySpeed/10
    case Key.Two =>
      projSpeed = defaultProjSpeed
      projColor = defaultProjColor
      flySpeed = defaultFlySpeed
    case Key.Three =>
      projSpeed = defaultProjSpeed*2
      projColor = Color.red
      flySpeed = defaultFlySpeed*10
    case Key.Four =>
      projSpeed = defaultProjSpeed*4
      projColor = Color.yellow
      flySpeed = defaultFlySpeed*100
    case Key.Five =>
      projSpeed = defaultProjSpeed*8
      projColor = Color.cyan
      flySpeed = defaultFlySpeed*1000
    case Key.Six =>
      projSpeed = defaultProjSpeed*16
      projColor = Color.magenta
      flySpeed = defaultFlySpeed*10000
    case Key.Seven =>
      projSpeed = defaultProjSpeed*32
      projColor = Color.blue
      flySpeed = defaultFlySpeed*100000
    case Key.Eight =>
      projSpeed = defaultProjSpeed*64
      projColor = Color.green
      flySpeed = defaultFlySpeed*10000000
    case Key.Nine =>
      projSpeed = defaultProjSpeed*128
      projColor = Color.red
      flySpeed = defaultFlySpeed*100000000
    case Key.Period => you.rotVel = Vec3(0,0,0)
    case Key.SysRq =>
      glReadBuffer(GL_FRONT)
      val width = Renderer.windowWidth
      val height = Renderer.windowHeight
      val bpp = 4
      val buffer = BufferUtils.createByteBuffer(width * height * bpp)
      glReadPixels(0, 0, width, height, GL_RGBA, GL_UNSIGNED_BYTE, buffer)

      val dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS").format(new Date())
      val file = new File("screenshots/screenshot-" + dateFormat + ".png")
      val format = "PNG"
      val image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
      for (x <- 0 until width; y <- 0 until height) {
        val i = (x + width*y) * bpp
        val r = buffer.get(i) & 0xff
        val g = buffer.get(i+1) & 0xff
        val b = buffer.get(i+2) & 0xff
        image.setRGB(x, height - (y+1), (0xff << 24) | (r << 16) | (g << 8) | b)
      }
      ImageIO.write(image, format, file)
    case _ =>
  }

  override def handleMouseMove(pos:IVec2, d:IVec2) {
    you.rotVel += you.rot * Vec3(-d.y*rotSpeed*dt, 0, 0)
    you.rotVel += you.rot * Vec3(0, -d.x*rotSpeed*dt, 0)
  }
}
