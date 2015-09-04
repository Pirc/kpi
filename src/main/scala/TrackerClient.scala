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

  def localReceive: Receive = { PartialFunction.empty }
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
  * Actors mixing in the TrackerClientActor should define their receive
  * operation in this localReceive method.  The TrackerClient will then 
  * add in it's own message handling to be executed for Tracker-specific
  * calls.
  */
  def localReceive: Receive

  override def receive = localReceive orElse trackerReceive

  def become(r: Receive) = context.become(r orElse trackerReceive)

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
      // Save off the path that we are binding to, in case we need it
      // later to reconnect.  Also set a timeout for the Found() response
      // to come back.
      println(s"received bind request for ${path}")
      pathBound = Some(path)
      implicit val ec = context.dispatcher
      root ! Tracker.Bind(path)
      ensureBound = context.system.scheduler.scheduleOnce(30.second, self, 
        Tracker.EnsureBound(path))

    case Tracker.EnsureBound(path) =>
      // EnsureBound() is sent on a Bind timeout (see Tracker.Bind() 
      // handler).  If this message is received, then retry the bind.
      self ! Tracker.Bind(path)

    case Tracker.Found(ref) => 
      // The TrackerTree found the requested path!  Cancel our timeout
      // watcher, create a watch on the TrackerTree in case it shuts
      // down, and save a reference to the actor that we are bound to
      // in the tree.
      println(s"path bound ${ref.path}")
      ensureBound.cancel
      context.watch(ref)
      tracker = Some(ref)

    case Tracker.Response(json) => println(json)

    case Tracker.Execute(fn) => doExecute(fn)

    case Terminated(ref) => 
      // The TrackerTree may go down due to failure or maintenance.  Whether
      // it goes down cleanly or not, attempt to re-bind this tracker client
      // to the tree (and enter into the retry cycle for failures to do so).
      self ! Tracker.Bind(pathBound.get)

    case shutdown @ Tracker.Shutdown() =>
      // Shutting down the Tracker Client should also remove the tracker
      // from the tree (assuming that this tracker client represents a
      // transient process that only needs to be tracked for the duration
      // of its work.
      tracker.map { ref => ref ! shutdown }
      context.stop(self)

    case detach @ Tracker.Detach() =>
      // Detaching a tracker client will shut down the local tracker client
      // but not the Tracker in the tree.
      context.stop(self)

    case msg: TrackerMessage => tracker.map { ref => ref ! msg }
  }

  def detach = context.self ! Tracker.Detach()
  def shutdown = context.self ! Tracker.Shutdown()
}

class TrackerClientImpl(val system: ActorSystem, val path: String) 
extends TrackerClient {
  val tracker = system.actorOf(Props[LocalActor])
  tracker ! Tracker.Bind(path)
  println(s"starting tracker client for ${path}")

  def shutdown = tracker ! Tracker.Shutdown()
  def detach = tracker ! Tracker.Detach() 
}
