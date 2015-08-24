package pirc.kpi

import com.typesafe.config.ConfigFactory
import akka.actor.{ActorRef, ActorSelection, ActorSystem, Props}

object Kpi {
  val system = ActorSystem("Kpi", ConfigFactory.load())
  val root = system.actorOf(Props[RootTracker], name = "root")
}

class TrackerClient {
  class LocalActor extends Actor {
    override def receive = {
    }
  }
  def locate(path: String) = {
  }
}


/*

In the app:

val unsubs = new TrackerClient("/members/unsubscribes")
unsubs.send(CounterTracker.Bump())
*/
