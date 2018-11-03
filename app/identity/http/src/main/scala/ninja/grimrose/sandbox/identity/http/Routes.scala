package ninja.grimrose.sandbox.identity.http

import akka.event.Logging
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server.{ Directives, Route }
import io.opencensus.scala.akka.http.TracingDirective.traceRequestNoSpan
import ninja.grimrose.BuildInfo
import wvlet.airframe.bind

trait Routes extends Directives with SprayJsonSupport {

  import spray.json._

  private val controller: IdentityController = bind[IdentityController]

  def routes: Route = logRequestResult("identity", Logging.InfoLevel) {
    pathEndOrSingleSlash {
      traceRequestNoSpan {
        complete(BuildInfo.toJson.parseJson)
      }
    } ~ controller.routes
  }

}
