package pirc.kpi

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
    path.split("/").head match {
      case "" => super.parseParts(path.split("/").tail.mkString("/"))
      case _ => throw new RuntimeException(s"path ${path} must start with /")
    }
  }
}

class Tracker extends Actor {
 /**
  * Find messages are sent to Trackers when an application is trying to 
  * obtain an instance of a Tracker to send information to.  The 
  * path should be a slash-delimited String that indicates a path to follow
  * from the root to get down to a particular tracker.
  */
  case class Find(path: String)
 
 /**
  * List messages are used to obtain a list of children of this Tracker.
  */
  case class List()

 /**
  * Execute messages are sent to Trackers in order to kick off some application
  * defined action in the system.
  */
  case class Execute(action: String)

  override def receive = {
    case Find(path) => findAndForward(path)
  }

  protected def parseParts(path: String) = {
    (path.split("/").head, path.split("/").tail.mkString("/"))
  }

  private def findChild(thisElem: String): Option[ActorRef] = {
    context.children
      .filter { child => child.path.name == thisElem }
      .headOption
      .orElse {
        Some(createChild(thisElem))
      }
  }

  def findAndForward(path: String) = {
    val (thisElem, remainder) = parseParts(path)

    findChild(thisElem)
      .map { childRef => {
        remainder match {
          case "" => context.sender ! context.self
          case path: String => childRef forward Find(path)
        }
      }}
  }

  def createChild(path: String): ActorRef = {
    context.actorOf(Props[Tracker], name = path)
  }
}

class CounterTracker(val hvc: HistogramValueCalculator) extends Actor {
  case class Bump(amount: Int = 1)

  val counter = new Counter(hvc)

  override def receive = {
    case Bump(amount) => counter.bump(amount)
  }
}
