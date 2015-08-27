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
  def execute(fn: String): Unit = findAndSend(Tracker.Execute(fn))
  def list(): String = findAndSend(Tracker.List())
  def status(): String = findAndSend(Tracker.Status())

  private def findAndSend(msg: Any): String = {
    implicit val timeout = Timeout(5.seconds)

    val futureResp = 
      for( tracker <- (root ? Tracker.Find(path)).mapTo[Tracker.Found]
         ; resp <- (tracker.ref ? msg).mapTo[Tracker.Response]
         ) yield resp.json

    Await.result(futureResp, 10.seconds)
  }
}
