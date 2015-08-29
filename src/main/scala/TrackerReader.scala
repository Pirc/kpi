package pirc.kpi

import com.typesafe.config.ConfigFactory

import akka.actor.{Actor, ActorRef, ActorSelection, ActorSystem, Props}
import akka.pattern.ask
import akka.util._
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

class TrackerReaderImpl(val root: ActorRef, val path: String) 
extends TrackerReader {
  def execute(fn: String): String = findAndSend(Tracker.Execute(fn))
  def list(): String = findAndSend(Tracker.List())
  def status(): String = findAndSend(Tracker.Status())

  private def findAndSend(msg: Any): String = {
    implicit val timeout = Timeout(5.seconds)

    val futureResp = 
      { root ? Tracker.Find(path) }
      .collect { 
        case found: Tracker.Found => found 
        case resp => throw new pirc.kpi.ex.TrackerException(resp)
      }
      .flatMap { tracker => { tracker.ref ? msg }.mapTo[Tracker.Response] }
      .map { resp => resp.json }

    Await.result(futureResp, 10.seconds)
  }
}
