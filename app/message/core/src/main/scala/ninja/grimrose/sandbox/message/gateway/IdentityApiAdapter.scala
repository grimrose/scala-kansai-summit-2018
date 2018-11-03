package ninja.grimrose.sandbox.message.gateway

import akka.http.scaladsl.model.HttpResponse
import javax.annotation.PreDestroy

import scala.concurrent.{ Future, Promise }

trait IdentityApiAdapter {

  type PromiseResponse = Promise[HttpResponse]

  // TODO parentSpan
  def generate(): Future[IdentityApiResponse]

  @PreDestroy
  def shutdown(): Unit

}

case class IdentityApiResponse(id: String)
