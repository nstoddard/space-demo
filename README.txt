This is a simple demo to test a control system in space, and also to test rendering a large scene using logarithmic depth buffering (see http://outerra.blogspot.com/2013/07/logarithmic-depth-buffer-optimizations.html).

Run the demo by running "sbt run" in a command line. You'll need to install SBT from http://www.scala-sbt.org/. If you want to configure it, there's a bunch of settings in the Main object in Main.scala: fullscreen, the gravitational constant, etc. Note that the code is fairly ugly since this is just a demo/prototype.

When the demo starts, you're placed in an orbit similar to that of the ISS. Nearby is a simple model of a spaceship that I made in about 2 minutes. Directly behind the spaceship is the moon, which is dim and hard to see. Everything is rendered at accurate distances and sizes, but relative orientations are not correct (the Earth isn't tilted at the right angle, the wrong side of the moon faces Earth, etc).

Controls:
  escape - quit
  mouse - rotate view
  qe - rotate view
  . - stop rotation
  wasd - move forward/left/down/right
  shift/ctrl - move up/down
  t/g - rotate the Earth
  r/f - zoom in/out
  space - fire projectile
  1-9 - change movement speed (1=very slow; 9=very fast)
    Also affects speed and color of projectiles
  print screen - taek screenshot

This application requires a 64-bit JVM to load larger textures. For whatever reason, a 32-bit JVM runs out of memory on larger textures. The largest supported textures are 16200 pixels wide, but not all graphics cards support textures that big. If you don't have a 64-bit JVM, it'll still work, but the textures will be less detailed.


Credits:
  The Earth textures are from http://www.shadedrelief.com/natural3/pages/textures.html
  The moon textures are from http://laps.noaa.gov/albers/sos/sos.html
