package ninja.grimrose.sandbox.message.gateway

import akka.http.scaladsl.model.{ HttpRequest, HttpResponse }
import cats.data.ReaderT
import io.opencensus.trace.Span

import scala.concurrent.Future

trait IdentityApiAdapter {

  type ReaderFS[A] = ReaderT[Future, Span, A]

  def generate(): ReaderFS[IdentityApiResponse]

}

trait SimpleAkkaHttpClient {
  def doRequest(): HttpRequest => Future[HttpResponse]
}

case class IdentityApiResponse(id: String)
