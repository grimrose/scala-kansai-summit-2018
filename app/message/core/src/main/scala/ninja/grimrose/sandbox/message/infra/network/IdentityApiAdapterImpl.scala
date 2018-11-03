package ninja.grimrose.sandbox.message.infra.network

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{ HttpRequest, HttpResponse, StatusCodes, Uri }
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.scaladsl.{ Keep, Sink, Source, SourceQueueWithComplete }
import akka.stream.{ Materializer, OverflowStrategy, QueueOfferResult }
import io.opencensus.scala.akka.http.TracingClient
import ninja.grimrose.sandbox.message.gateway.{ IdentityApiAdapter, IdentityApiResponse }
import skinny.SkinnyConfig
import skinny.logging.Logging
import spray.json.RootJsonFormat

import scala.concurrent.{ ExecutionContext, Future, Promise }
import scala.util.{ Failure, Success }

class IdentityApiAdapterImpl(config: SkinnyConfig, actorSystem: ActorSystem, materializer: Materializer)
    extends IdentityApiAdapter
    with Logging
    with SprayJsonSupport {

  private val requestHost = config.stringConfigValue("identity.host").getOrElse("localhost")
  private val requestPort = config.intConfigValue("identity.port").getOrElse(80)
  private val uri         = Uri(s"http://$requestHost:$requestPort/identity")

  private val QUEUE_SIZE = config.intConfigValue("indentity.queue-size").getOrElse(10)

  private implicit val _system: ActorSystem = actorSystem
  private implicit val _mat: Materializer   = materializer

  private implicit val _ec: ExecutionContext = _system.dispatchers.lookup("identity-dispatcher")

  // TODO https://github.com/census-ecosystem/opencensus-scala/blob/master/akka-http/README.md#request-level-client

  private val poolClientFlow =
    TracingClient.traceRequestForPool(Http().cachedHostConnectionPool[PromiseResponse](requestHost, requestPort))

  private val queue: SourceQueueWithComplete[(HttpRequest, PromiseResponse)] = Source
    .queue[(HttpRequest, PromiseResponse)](QUEUE_SIZE, OverflowStrategy.backpressure)
    .via(poolClientFlow)
    .toMat(Sink.foreach({
      case (Success(res), promise) => promise.success(res)
      case (Failure(e), promise)   => promise.failure(e)
    }))(Keep.left)
    .run()

  override def shutdown(): Unit = queue.complete()

  private def queueRequest(request: HttpRequest): Future[HttpResponse] = {
    logger.debug(request)

    val promise = Promise[HttpResponse]()
    queue
      .offer(request -> promise)
      .map {
        case QueueOfferResult.Enqueued    => promise
        case QueueOfferResult.Dropped     => promise.failure(new RuntimeException("Queue overflowed. Try again later."))
        case QueueOfferResult.Failure(ex) => promise.failure(ex)
        case QueueOfferResult.QueueClosed =>
          promise.failure(
            new RuntimeException("Queue was closed (pool shut down) while running the request. Try again later.")
          )
      }
      .flatMap(_.future)
  }

  import spray.json.DefaultJsonProtocol._

  private implicit val apiResponseJsonFormat: RootJsonFormat[IdentityApiResponse] =
    jsonFormat(IdentityApiResponse.apply, "id")

  def generate(): Future[IdentityApiResponse] = queueRequest(HttpRequest(uri = uri)).flatMap {
    case HttpResponse(StatusCodes.OK, _, entity, _) =>
      // ex.: {"id":"503272088132419584"}
      Unmarshal(entity).to[IdentityApiResponse]
    case _ @HttpResponse(code, _, _, _) =>
      Future.failed(new IllegalArgumentException(s"Request failed, response code: $code"))
  }

}
