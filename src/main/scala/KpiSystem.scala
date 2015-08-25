package pirc.kpi

import com.typesafe.config.ConfigFactory
import akka.actor.{Actor, ActorRef, ActorSelection, ActorSystem, Props}

object Kpi {
  val system = ActorSystem("Kpi", ConfigFactory.load())
  val root = system.actorOf(Props[RootTracker], name = "root")
}

object Client {
  val system = ActorSystem("TrackedApp", ConfigFactory.load())
}

class LocalActor extends Actor {
  var tracker: Option[ActorRef] = None

  override def receive = {
    case Tracker.Find(path) => Kpi.root ! Tracker.Find(path)
    case Tracker.Found(ref) => 
      print(s"received tracker ref back from kpi root: ${ref.path}")
      tracker = Some(ref)
    case Tracker.Response(json) => println(json)
    case a:Any => tracker.map { ref => ref ! a }
  }
}

class TrackerClient(val path: String) {
  val actor = Client.system.actorOf(Props[LocalActor])
  actor ! Tracker.Find(path)
}


/*

In the app:

val unsubs = new TrackerClient("/members/unsubscribes")
unsubs.send(CounterTracker.Bump())
*/
