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
    path.split("/").headOption match {
      case None => (Some(self), "")
      case Some("") => super.parseParts(path.split("/").tail.mkString("/"))
      case _ => throw new RuntimeException(s"path ${path} must start with /")
    }
  }
}

object Tracker {
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
}

class Tracker extends Actor {
  override def receive = {
    case Tracker.Find(path) => 
      println(s"processing find request for ${path}")
      findAndForward(path)
    case Tracker.Status() => println(s"received Status() request in ${self.path.name}")
  }

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
          case "" => context.sender ! Tracker.Found(childRef)
          // If there are additional path elements to walk, then forward
          // the request on down the line.
          case path: String => childRef forward Tracker.Find(path)
        }
      }}
      .orElse {
        throw new RuntimeException(s"something went wrong finding path <${path}>")
      }
  }

  def createChild(path: String): ActorRef = {
    context.actorOf(Props[Tracker], name = path)
  }
}

object CounterTracker {
  case class Bump(amount: Int = 1)
}

/**
 * The CounterTracker is a tracker that counts "bumps" that it receives over
 * a period of time, and creates histograms based on the number of bumps
 * received.  The contructor receives a HistogramValueCalculator instance
 * so that it knows how to initialize the histogram on instantiation.
 */
class CounterTracker(val hvc: HistogramValueCalculator) extends Tracker {
  val counter = new Counter(hvc)

  override def receive = counterReceive orElse super.receive

  def counterReceive: Actor.Receive = {
    case CounterTracker.Bump(amount) => counter.bump(amount)
  }
}
