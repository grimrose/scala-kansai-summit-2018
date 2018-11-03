package ninja.grimrose.sandbox.identity.http

import akka.actor.ActorSystem
import com.typesafe.config.{ Config, ConfigFactory }
import ninja.grimrose.sandbox.identity.IdGenerator
import ninja.grimrose.sandbox.katsubushi
import org.slf4j.bridge.SLF4JBridgeHandler
import wvlet.airframe.newDesign

object Main extends App {
  // jul -> slf4j
  SLF4JBridgeHandler.removeHandlersForRootLogger()
  SLF4JBridgeHandler.install()

  implicit val system: ActorSystem = ActorSystem("identity-http")

  val config: Config = ConfigFactory.load()

  val host = config.getString("http.host")
  val port = config.getInt("http.port")

  val design = newDesign.withProductionMode.noLifeCycleLogging
    .add(katsubushi.design)
    .bind[ActorSystem].toInstance(system)
    .bind[Server].toSingleton
    .bind[Routes].toSingleton
    .bind[IdentityController].toSingleton
    .bind[IdGenerator].toSingleton

  design.withSession { session =>
    val system = session.build[ActorSystem]
    session.build[Server].startServer(host, port, system)
  }
}
