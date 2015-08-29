package pirc.kpi

import com.typesafe.config.ConfigFactory
import akka.actor.{Actor, ActorRef, ActorSelection, ActorSystem, Props}

class LocalActor extends Actor with TrackerClientActor { 
  override def receive = trackerReceive

  def execute(fn: String) = { }
}

/**
 * The TrackerClientActor trait knows how to communicate with the 
 * TrackerTree in order to accomplish tracking tasks.  Applications that
 * are actually Actors themselves may mix in this trait and pick up the
 * additional knowledge needed to become a TrackerClient, whereas non-Actor
 * clients will just use the TrackerClientImpl and invoke methods directly 
 * on that interface.
 *
 * The value of mixing the TrackerClientActor in with the receive function
 * on an already existing actor is that it gives us the ability to receive
 * Tracker messages and act on them right from within the app.
 */
trait TrackerClientActor {
  actor: Actor =>

  var tracker: Option[ActorRef] = None

  def execute(fn: String)

  lazy val root = context.actorSelection(
    "akka.tcp://Kpi@kpi.internal.pirc.com:2562/user/root")

  def trackerReceive: Receive = {
    case Tracker.Bind(path) => 
      println(s"sending bind request kpi root: ${path}")
      root ! Tracker.Bind(path)
    case Tracker.Found(ref) => 
      println(s"received tracker ref back from kpi root: ${ref.path}")
      tracker = Some(ref)
    case Tracker.Response(json) => println(json)
    case Tracker.Execute(fn) => execute(fn)
    case a:Any => tracker.map { ref => ref ! a }
  }
}

class TrackerClientImpl(val system: ActorSystem, val path: String) {
  val tracker = system.actorOf(Props[LocalActor])
  tracker ! Tracker.Bind(path)

  def shutdown = {
    tracker ! Tracker.Shutdown()
  }
}
