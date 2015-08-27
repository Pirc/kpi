package pirc.kpi

import scala.collection.JavaConverters._
import scala.collection.mutable.Stack

import com.fasterxml.jackson.databind.ObjectMapper

import akka.actor.{Actor, ActorRef, Props}

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
  protected override def parseParts(path: String) = {
    path.split("/").headOption match {
      case  None => (Some(self), "")
      case Some("") => super.parseParts(path.split("/").tail.mkString("/"))
      case _ => throw new RuntimeException(s"path ${path} must start with /")
    }
  }
}

object Tracker {
  case class Initialize()
 /**
  * Find messages are sent to Trackers when an application is trying to 
  * obtain an instance of a Tracker to send information to.  The 
  * path should be a slash-delimited String that indicates a path to follow
  * from the root to get down to a particular tracker.
  */
  case class Find(path: String)

 /**
  * When a Tracker is found as the result of a Find(path) request, the 
  * ActorRef that was found is sent back as a Found() object.
  */
  case class Found(ref: ActorRef)
 
 /**
  * List messages are used to obtain a list of children of this Tracker.
  */
  case class List()

  case class Status()
 /**
  * Execute messages are sent to Trackers in order to kick off some 
  * application defined action in the system.
  */
  case class Execute(action: String)

  case class Response(json: String)

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
    case Tracker.Initialize => initialize
    case Tracker.Find(path) => findAndForward(path)
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
    // If the Tracker has received an object it doesn't know about, 
    // then just send it on to the 
    case a: Any => tracked.map { t => t ! a }
  }

  def status: Any = "ok"

 /**
  * The job of this method is to receive a String that represents a
  * starting from this actor's position in the tree, to split that path
  * on the "/" delimiter, and to then resolve the path to 2 components:
  * an Optional actor ref pointing to the next child in the tree and the
  * remainder of the path after that.
  */
  protected def parseParts(path: String): Tuple2[Option[ActorRef], String] = {
    (findChild(path.split("/").head), path.split("/").tail.mkString("/"))
  }

 /**
  * Looks in the list of this actors children for a child that has the
  * given name.
  */
  private def findChild(thisElem: String): Option[ActorRef] = {
    Option(thisElem)
      .filter { s => s.size > 0 }
      .flatMap { thisPathElem => 
        context.children
          .filter { child => child.path.name == thisElem }
          .headOption
          .orElse {
            Some(createChild(thisElem))
          }
      }
  }

  def findAndForward(path: String) = {
    val (thisElem, remainder) = parseParts(path)

    thisElem
      .map { childRef => {
        remainder match {
          // If there is no remainder, then we are at the path that was 
          // requested - send self back to the original caller.
          case "" => 
            tracked = Some(context.sender)
            context.sender ! Tracker.Found(childRef)
          // If there are additional path elements to walk, then forward
          // the request on down the line.
          case path: String => childRef forward Tracker.Find(path)
        }
      }}
      .orElse {
        throw new RuntimeException(s"something went wrong finding path <${path}>")
      }
  }

  def createChild(name: String): ActorRef = {
    context.actorOf(Tracker.props(name), name = name)
  }
}
