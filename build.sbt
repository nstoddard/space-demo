scalaVersion := "2.11.2"

scalaSource in Compile <<= baseDirectory(_ / "src")

//libraryDependencies += "com.typesafe.akka" % "akka-actor_2.10" % "2.1.1"

scalacOptions ++= Seq("-deprecation", "-feature")

scalacOptions += "-optimise"

javaOptions ++= Seq("-Xms256m", "-Xmx1000m") //, "-d64")

seq(com.github.retronym.SbtOneJar.oneJarSettings: _*)

seq(LWJGLPlugin.lwjglSettings: _*)

outputStrategy := Some(StdoutOutput)

connectInput in run := true

fork := true
