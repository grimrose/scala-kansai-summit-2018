package ninja.grimrose.sandbox.message.http

import akka.actor.ActorSystem
import akka.stream.{ ActorMaterializer, Materializer }
import com.typesafe.config.{ Config, ConfigFactory }
import io.opencensus.exporter.stats.prometheus.PrometheusStatsCollector
import io.opencensus.exporter.trace.jaeger.JaegerTraceExporter
import io.opencensus.ocjdbc.Observability
import ninja.grimrose.sandbox.message
import ninja.grimrose.sandbox.message.http.controller.MessageController
import org.slf4j.bridge.SLF4JBridgeHandler
import wvlet.airframe._

object Main extends App {
  // jul -> slf4j
  SLF4JBridgeHandler.removeHandlersForRootLogger()
  SLF4JBridgeHandler.install()

  private val config: Config = ConfigFactory.load()

  Observability.registerAllViews()

  JaegerTraceExporter.createAndRegister(
    config.getString("jaeger.thrift-endpoint"),
    config.getString("jaeger.service-name")
  )

  PrometheusStatsCollector.createAndRegister()
  new io.prometheus.client.exporter.HTTPServer(8082)

  implicit val system: ActorSystem = ActorSystem("message-http")

  private val materializer = ActorMaterializer()

  private val host = config.getString("http.host")
  private val port = config.getInt("http.port")

  val design = newDesign.withProductionMode.noLifeCycleLogging
    .add(message.design)
    .bind[ActorSystem].toInstance(system)
    .bind[ActorMaterializer].toInstance(materializer)
    .bind[Materializer].toInstance(materializer)
    .bind[Server].toSingleton
    .bind[Routes].toSingleton
    .bind[MessageController].toSingleton

  design.withSession { session =>
    val system = session.build[ActorSystem]
    session.build[Server].startServer(host, port, system)
  }

}
