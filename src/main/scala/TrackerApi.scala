package pirc.kpi

import com.typesafe.config.Config

import akka.actor.ActorSystem

class TrackerApiImpl(val system: ActorSystem) extends TrackerApi {

  val counterTracker = classOf[CounterTrackerClient]
  val logTracker  = classOf[LogTrackerClient]

  def locate[A <: TrackerClient](c: Class[A], path: String): A = {
    c match {
      case `counterTracker` =>
        new CounterTrackerClientImpl(system, path).asInstanceOf[A]
      case `logTracker` =>
        new LogTrackerClientImpl(system, path).asInstanceOf[A]
      case _ =>
        new TrackerClientImpl(system, path).asInstanceOf[A]
    }
  }
}
