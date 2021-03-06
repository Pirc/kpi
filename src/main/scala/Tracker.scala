package pirc.kpi.impl

import scala.collection.JavaConverters._
import scala.collection.mutable.Stack
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

import com.fasterxml.jackson.databind.ObjectMapper

import akka.actor.{Actor, ActorRef, Props, Terminated}
import akka.pattern.ask
import akka.util._

/**
 * The Root of the Tracker KPI system will be the Actor that is always known
 * to be present in the system, so that clients will have a starting point
 * for finding their requested Trackers.  Any Tracker discovery calls will
 * be initially routed here, then will walk down the hierarchy.
 */
class RootTracker extends Tracker {
 /**
  * The RootTracker has a slightly different bit of processing for parsing 
  * the path.  It needs to make sure that the path starts with '/', and then
  * to skip that first (empty) element.
  */
  protected override def parseParts( 
                            path: String
                          , findOrBind: (String) => Option[ActorRef]
                          ) : Tuple2[Option[ActorRef], String] = {
    path.split("/").headOption match {
      case  None => (Some(self), "")
      case Some("") => 
        path.split("/").tail.mkString("/") match {
          case "" => super.parseParts("", bindChild _)
          case path: String => super.parseParts(path, findOrBind)
        }
      case _ => throw new RuntimeException(s"path ${path} must start with /")
    }
  }
}

abstract class TrackerMessage()

object Tracker {
  case class Initialize() extends TrackerMessage
 /**
  * Bind messages are sent to Trackers when an application is trying to 
  * obtain an instance of a Tracker to send information to.  The 
  * path should be a slash-delimited String that indicates a path to follow
  * from the root to get down to a particular tracker.  If the path does
  * not exist, then the path elements will be created along the way.  This
  * means that any legal path that is requested will ultimately find a 
  * new or previously created Tracker.
  */
  case class Bind(path: String) extends TrackerMessage
  case class EnsureBound(path: String) extends TrackerMessage

 /**
  * Find requests are issued by applications that are looking for a 
  * particular tracker, but not for the purposes of "binding" to it.  Rather, 
  * these requests are made by applications would like to gain access
  * to trackers for the purpose of getting some informaiton or invoking a 
  * command on the Tracker.
  */
  case class Find(path: String) extends TrackerMessage


 /**
  * When a Tracker is found as the result of a Find(path) request, the 
  * ActorRef that was found is sent back as a Found() object.
  */
  case class Found(ref: ActorRef) extends TrackerMessage

  case class NotFound(path: String) extends TrackerMessage
 
 /**
  * List messages are used to obtain a list of children of this Tracker.
  */
  case class List() extends TrackerMessage

  case class Status() extends TrackerMessage
 /**
  * Execute messages are sent to Trackers in order to kick off some 
  * application defined action in the system.
  */
  case class Execute(action: String) extends TrackerMessage

  case class Response(json: String) extends TrackerMessage

  case class Detach() extends TrackerMessage
  case class Shutdown() extends TrackerMessage

  val factories = Stack[PartialFunction[String, Props]]()

  def props(name: String): Props = {
    factories
      .find { pf => pf.isDefinedAt(name) }
      .map { pf => pf.apply(name) }
      .getOrElse { Props[Tracker] }
  }
}

class Tracker extends Actor {
  private val mapper = new ObjectMapper
  private var tracked: Option[ActorRef] = None

  override def preStart = self ! Tracker.Initialize()

  def initialize = { }

  override def receive = {
    case Tracker.Initialize() => initialize
    case Tracker.Bind(path) => 
      println(s"In tracker tree, received bind request for ${path}")
      bindAndForward(path)
    case Tracker.Find(path) => findAndForward(path)
    case Tracker.Found(t) => 
      println(s"In tracker tree, found actor ${t.path}, returning to binder")
      tracked = Some(context.sender)
      context.watch(context.sender)
      context.sender ! Tracker.Found(self)
    case Tracker.Status() => 
      context.sender ! Tracker.Response(
        mapper.writeValueAsString(status)
      )
    case Tracker.List() =>
      context.sender ! Tracker.Response(
        mapper.writeValueAsString(
          context.children.map { child => child.path.name }.asJava 
        )
      )
    case Tracker.Shutdown() =>
      context.stop(self)
    case t @ Terminated(tracked) =>
      if(t.addressTerminated) {
        println(s"shutting down tracker ${self.path}")
        self ! Tracker.Shutdown()
      }
    case exec: Tracker.Execute => 
      // If the Tracker has received an object it doesn't know about, 
      // then just send it on to the tracked actor.
      tracked
        .map { t => {
          implicit val timeout = Timeout(5.seconds)
          val futureResp = (t ? exec).mapTo[Tracker.Response]
          context.sender ! Await.result(futureResp, 10.seconds)
        }}
        .getOrElse {
          println(s"Tracker ${self.path} has no tracked actor to send to.")
        }
  }

  def status: Any = "ok"

 /**
  * The job of this method is to receive a String that represents a
  * starting from this actor's position in the tree, to split that path
  * on the "/" delimiter, and to then resolve the path to 2 components:
  * an Optional actor ref pointing to the next child in the tree and the
  * remainder of the path after that.
  */
  protected def parseParts( path: String
                          , findOrBind: (String) => Option[ActorRef]
                          ) : Tuple2[Option[ActorRef], String] = {
    (findOrBind(path.split("/").head), path.split("/").tail.mkString("/"))
  }

 /**
  * Looks in the list of this actors children for a child that has the
  * given name.
  */
  protected def findChild(thisElem: String): Option[ActorRef] = {
    Option(thisElem)
      .filter { s => s.size > 0 }
      .flatMap { thisPathElem => 
        context.children
          .filter { child => child.path.name == thisElem }
          .headOption
      }
  }

  protected def bindChild(thisElem: String): Option[ActorRef] = {
    findChild(thisElem)
      .orElse {
        Some(context.actorOf(Tracker.props(thisElem), name = thisElem))
      }
  }

  def findAndForward(path: String) = {
    val (thisElem, remainder) = parseParts(path, findChild _)

    thisElem
      .map { childRef => {
        remainder match {
          // If there is no remainder, then we are at the path that was 
          // requested - send self back to the original caller.
          case "" => 
            context.sender ! Tracker.Found(childRef)
          // If there are additional path elements to walk, then forward
          // the request on down the line.
          case path: String => childRef forward Tracker.Find(path)
        }
      }}
      .getOrElse {
        context.sender ! Tracker.NotFound(path)
      }
  }

  def bindAndForward(path: String) = {
    val (thisElem, remainder) = parseParts(path, bindChild _)

    thisElem
      .map { childRef => {
        remainder match {
          // If there is no remainder, then we are at the path that was 
          // requested - forward on to the found tracker to save the
          // requester.
          case "" => 
            childRef forward Tracker.Found(childRef)
            println(s"bound tracker ${childRef.path} to ${context.sender.path}")
          // If there are additional path elements to walk, then forward
          // the request on down the line.
          case path: String => childRef forward Tracker.Bind(path)
        }
      }}
      .orElse {
        throw new RuntimeException(s"Something went wrong binding to ${path}")
      }
  }
}
