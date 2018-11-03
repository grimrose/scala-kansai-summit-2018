package ninja.grimrose.sandbox.message.http

import akka.event.Logging
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server.{ Directives, Route }
import io.opencensus.scala.akka.http.TracingDirective.traceRequestNoSpan
import ninja.grimrose.BuildInfo
import ninja.grimrose.sandbox.message.http.controller.MessageController
import wvlet.airframe._

trait Routes extends Directives with SprayJsonSupport {

  import spray.json._

  private val messageController = bind[MessageController]

  def routes: Route = logRequestResult("message", Logging.InfoLevel) {
    pathEndOrSingleSlash {
      traceRequestNoSpan {
        complete(BuildInfo.toJson.parseJson)
      }

    } ~ messageController.messageRoutes
  }

}
