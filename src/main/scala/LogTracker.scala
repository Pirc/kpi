package pirc.kpi

import scala.collection.JavaConverters._

import akka.actor.{Actor, ActorSystem, Props}

// ----------------------------------------------------------------------
//
//                                L   O   G
//
//                        T   R   A   C   K   E   R
//
// A tracker that accumulates log messages from the tracked application.
// ----------------------------------------------------------------------
object LogTracker {
  val Match = "(.*)Log$".r

  Tracker.factories.push({case Match(t) => Props[LogTracker]})

  case class Info(msg: String)
  case class Warning(msg: String)
  case class Error(msg: String)
}

class LogTracker extends Tracker {
  val bufferSize = 20
  var messages = Seq[Message]()

  case class Message(timestamp: java.util.Date, level: Int, message: String)

  override def receive = logReceive orElse super.receive

  private def addMessage(level: Int, msg: String): Seq[Message] = {
    (Message(new java.util.Date, level, msg) +: messages).take(bufferSize)
  }

  def logReceive: Actor.Receive = {
    case LogTracker.Info(msg) => messages = addMessage(3, msg)
    case LogTracker.Warning(msg) => messages = addMessage(2, msg)
    case LogTracker.Error(msg) => messages = addMessage(1, msg)
  }

  override def status: Any = messages.asJava
}

class LogTrackerClientImpl(system: ActorSystem, path: String) 
extends TrackerClientImpl(system, path) 
with LogTrackerClient {
  def info(msg: String) = { tracker ! LogTracker.Info(msg) }
  def warning(msg: String) = { tracker ! LogTracker.Warning(msg) }
  def error(msg: String) = { tracker ! LogTracker.Error(msg) }
}
