package pirc.kpi

import java.util.concurrent.TimeUnit

import scala.concurrent.duration._

import com.typesafe.config.ConfigFactory
import akka.actor.{ Actor
                  , ActorRef
                  , ActorSelection
                  , ActorSystem
                  , Cancellable
                  , Props
                  , Terminated
                  }


class LocalActor extends Actor with TrackerClientActor { 
  def pathToBind = None

  override def receive = trackerReceive
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
trait TrackerClientActor extends Actor {
  def pathToBind: Option[String]

 /**
  * Stores the path that was ultimately bound, so that we have it for our
  * attemps to re-bind if the Tracker Tree goes away.
  */
  var pathBound: Option[String] = None

  override def preStart = {
    pathToBind.map { path => self ! Tracker.Bind(path) }
    super.preStart
  }

  override def postStop = {
    shutdown
    super.postStop
  }

  var tracker: Option[ActorRef] = None

  def execute(fn: String) = {
    s"function <${fn}> not defined for ${self.path}"
  }

  private def doExecute(fn: String) = {
    context.sender ! Tracker.Response(execute(fn))
  }

  lazy val root = context.actorSelection(
    "akka.tcp://Kpi@kpi.internal.pirc.com:2562/user/root")

  var ensureBound: Cancellable = _

  def trackerReceive: Receive = {
    case Tracker.Bind(path) => 
      pathBound = Some(path)
      implicit val ec = context.dispatcher
      println(s"sending bind request kpi root: ${path}")
      root ! Tracker.Bind(path)
      ensureBound = context.system.scheduler.scheduleOnce(30.second, self, 
        Tracker.EnsureBound(path))
    case Tracker.EnsureBound(path) =>
      println("Binding did not happen within 30s.  Retrying")
      self ! Tracker.Bind(path)
    case Tracker.Found(ref) => 
      println(s"received tracker ref back from kpi root: ${ref.path}")
      ensureBound.cancel
      context.watch(ref)
      tracker = Some(ref)
    case Tracker.Response(json) => println(json)
    case Tracker.Execute(fn) => doExecute(fn)
    case Terminated(ref) => 
      println(s"tracker ${pathBound.get} terminated, attempting to re-bind.")
      self ! Tracker.Bind(pathBound.get)
    case a:Any => tracker.map { ref => ref ! a }
  }

  def shutdown = { self ! Tracker.Shutdown() }
}

class TrackerClientImpl(val system: ActorSystem, val path: String) {
  val tracker = system.actorOf(Props[LocalActor])
  tracker ! Tracker.Bind(path)

  def shutdown = {
    tracker ! Tracker.Shutdown()
  }
}
