package pirc.kpi

import com.typesafe.config.Config
import akka.actor.{ActorSystem, Props}

/**
 * The TrackerTree is hosted on the KPI machine, and it's where all of the
 * Tracker instances will live and also where the admin app that reads those
 * instance will live.  Client machines need an instance of the TrackerApi,
 * but the main KPI machine will also need an instance of this to be created 
 * and also for a Config to be injected.
 */
object TrackerTreeApiImpl {
  var config: Config = _

  lazy val kpiSystem = 
    ActorSystem("Kpi", config.getConfig("Kpi").withFallback(config))

  lazy val root = kpiSystem.actorOf(Props[RootTracker], name = "root")

  CounterTracker
  LogTracker
}

class TrackerTreeApiImpl(val config: Config) extends TrackerTreeApi {
  println("Initialize actor tree")
  val e = new Exception()
  e.printStackTrace()
  TrackerTreeApiImpl.config = config

  def locate(path: String): TrackerReader = {
    new TrackerReaderImpl(TrackerTreeApiImpl.root, path)
  }
}
