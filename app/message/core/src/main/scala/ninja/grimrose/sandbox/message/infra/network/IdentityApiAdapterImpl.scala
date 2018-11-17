package ninja.grimrose.sandbox.message.infra.network

import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{ HttpRequest, HttpResponse, StatusCodes, Uri }
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.Materializer
import cats.data.ReaderT
import io.opencensus.scala.akka.http.TracingClient
import ninja.grimrose.sandbox.message.gateway.{ IdentityApiAdapter, IdentityApiResponse, SimpleAkkaHttpClient }
import skinny.SkinnyConfig
import skinny.logging.Logging
import spray.json.RootJsonFormat

import scala.concurrent.{ ExecutionContext, Future }

class IdentityApiAdapterImpl(
    config: SkinnyConfig,
    actorSystem: ActorSystem,
    materializer: Materializer,
    httpClient: SimpleAkkaHttpClient
) extends IdentityApiAdapter
    with Logging
    with SprayJsonSupport {

  private val requestHost = config.stringConfigValue("identity.host").getOrElse("localhost")
  private val requestPort = config.intConfigValue("identity.port").getOrElse(80)
  private val uri         = Uri(s"http://$requestHost:$requestPort/identity")

  private implicit val _system: ActorSystem = actorSystem
  private implicit val _mat: Materializer   = materializer

  private implicit val _ec: ExecutionContext = _system.dispatchers.lookup("identity-dispatcher")

  import spray.json.DefaultJsonProtocol._

  private implicit val apiResponseJsonFormat: RootJsonFormat[IdentityApiResponse] =
    jsonFormat(IdentityApiResponse.apply, "id")

  override def generate(): ReaderFS[IdentityApiResponse] = ReaderT { parentSpan =>
    TracingClient.traceRequest(httpClient.doRequest(), parentSpan)(HttpRequest(uri = uri)).flatMap {
      case HttpResponse(StatusCodes.OK, _, entity, _) =>
        // ex.: {"id":"503272088132419584"}
        Unmarshal(entity).to[IdentityApiResponse]
      case _ @HttpResponse(code, _, _, _) =>
        Future.failed(new IllegalStateException(s"Request failed, response code: $code"))
    }
  }
}
