package pirc.kpi

import com.typesafe.config.ConfigFactory
import akka.actor.{Actor, ActorRef, ActorSelection, ActorSystem, Props}

class LocalActor extends Actor {
  var tracker: Option[ActorRef] = None
  lazy val root = context.actorSelection("akka.tcp://Kpi@kpi.internal.pirc.com:2562/user/root")

  override def receive = {
    case Tracker.Find(path) => root ! Tracker.Find(path)
    case Tracker.Found(ref) => 
      print(s"received tracker ref back from kpi root: ${ref.path}")
      tracker = Some(ref)
    case Tracker.Response(json) => println(json)
    case a:Any => tracker.map { ref => ref ! a }
  }
}

class TrackerClientImpl(val system: ActorSystem, val path: String) {
  val tracker = system.actorOf(Props[LocalActor])
  tracker ! Tracker.Find(path)
}
