//Copyright (c) 2013 Nathan Stoddard (nstodda@purdue.edu); see LICENSE.txt

package com.nathanstoddard.common.sound
import com.nathanstoddard._

import common._

import scala.collection.mutable

import org.lwjgl._
import org.lwjgl.openal.AL
import org.lwjgl.openal.ALC11

import org.newdawn.slick._
import org.newdawn.slick.util._
import org.newdawn.slick.openal._

import java.io._
import java.nio._

/** A simplified wrapper around slick-util's openal module
  */

//TODO: do something with 'resumable'
//TODO: add way to restart a sound from the beginning
//TODO: make sure that changing a sound's volume actually has an effect
//TODO: hide OpenAL and Slick-util primitives such as Audio

class Sound(val audio:Audio, val resumable:Boolean, val name:String, val defaultGain:Real=1.0, val defaultPitch:Real=1.0) {
  protected var storedPos:Option[Float] = None
  protected var storedPitch = 1.0
  protected var storedGain = 1.0
  protected var storedLoop = false

  def play(pitch:Real, gain:Real, loop:Boolean):Unit = if (!Sound.muted) {
    //println("Attempting to play " + name + " (#2); " + isPlaying)
    //if (!isPlaying) {
    //println("Playing sound " + name)
    audio.playAsSoundEffect((pitch*defaultPitch).toFloat, (gain*defaultGain).toFloat, loop)
    storedPitch = pitch
    storedGain = gain
    storedLoop = loop
    storedPos match {
      case Some(storedPos2) => {
        audio.setPosition(storedPos2)
        storedPos = None
      }
      case None =>
    }
  }
  def play(gain:Real, loop:Boolean):Unit = play(storedPitch, gain, loop)
  def play(gain:Real):Unit = play(storedPitch, gain, storedLoop)
  def play(loop:Boolean):Unit = play(storedPitch, storedGain, loop)
  def play():Unit = play(storedPitch, storedGain, storedLoop)
  def isPlaying = audio.isPlaying
  def stop() = {
    if (isPlaying) {
      //println("Stopping sound " + name)
      storedPos = Some(audio.getPosition)
      audio.stop()
    }
  }

  override def toString() = name
}

class Music(audio:Audio, resumable:Boolean, name:String, defaultGain:Real=1.0, defaultPitch:Real=1.0) extends Sound(audio, resumable, name) {
  override def play(pitch:Real, gain:Real, loop:Boolean):Unit = if (!Sound.muted) {
    //println("Playing music " + name)
    audio.playAsMusic((pitch*defaultPitch).toFloat, (gain*defaultGain).toFloat, loop)
    storedPitch = pitch
    storedGain = gain
    storedLoop = loop
    storedPos match {
      case Some(storedPos2) => {
        audio.setPosition(storedPos2)
        storedPos = None
      }
      case None =>
    }
  }
}

object Sound {
  def muted = false
  def muted_=(mute:Boolean) {
    SoundStore.get.setMusicOn(!mute)
    SoundStore.get.setSoundsOn(!mute)
  }

  private var allSounds:Map[String, Sound] = Map()

  def apply(name:String) = allSounds(name)

  def init(muted:Boolean=false, soundVolume:Real=1.0, musicVolume:Real=1.0) {
    SoundStore.get.init()
    SoundStore.get.setMaxSources(32)
    this.muted = muted
    SoundStore.get.setSoundVolume(soundVolume.toFloat)
    SoundStore.get.setMusicVolume(musicVolume.toFloat)
  }

  def destroy() {
    for ((_,sound) <- allSounds) if (sound.isPlaying) sound.stop()
    SoundStore.get.clear()
    AL.destroy()
  }
  atexit(() => destroy())

  //TODO: split this into loadSound and loadMusic
  //TODO: use asset manager
  //TODO: get rid of stupid 'name' system
  //TODO: WTF is resumable for?
  //TODO: get rid of format - figure it out from filename
  def load(name:String, filename:String, format:String, music:Boolean, resumable:Boolean, defaultGain:Real=1.0, defaultPitch:Real=1.0) = {
    val audio = AudioLoader.getAudio(format, ResourceLoader.getResourceAsStream(filename))
    val sound = if (music)
        new Music(audio, resumable, name, defaultGain, defaultPitch)
      else
        new Sound(audio, resumable, name, defaultGain, defaultPitch)
    allSounds += (name -> sound)
    sound
  }

  val fadeDuration = 0.5 //seconds
  val fadeFPS = 60
}
