package ninja.grimrose.sandbox.message.infra.network

import akka.actor.{ ActorSystem, Status }
import akka.http.scaladsl.model._
import akka.stream.{ ActorMaterializer, Materializer }
import akka.testkit.TestKit
import cats.instances.future._
import io.opencensus.scala.Tracing
import ninja.grimrose.sandbox.AkkaSpecHelper
import ninja.grimrose.sandbox.message.gateway.{ IdentityApiAdapter, SimpleAkkaHttpClient }
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.{ ExecutionContext, Future }

class IdentityApiAdapterImplSpec
    extends TestKit(ActorSystem("IdentityApiAdapterImplSpec"))
    with FlatSpecLike
    with Matchers
    with DiagrammedAssertions
    with OptionValues
    with ScalaFutures
    with AkkaSpecHelper {
  import akka.pattern._
  import ninja.grimrose.sandbox.message.design

  private implicit val ctx: ExecutionContext = system.dispatcher

  private implicit val mat: Materializer = ActorMaterializer()

  behavior of "IdentityApiAdapterImpl"

  private val baseDesign = design
    .bind[ActorSystem].toInstance(system)
    .bind[Materializer].toInstance(mat)

  it should "be success" in {
    baseDesign
      .bind[SimpleAkkaHttpClient].toInstance(new SimpleAkkaHttpClient {
        override def doRequest(): HttpRequest => Future[HttpResponse] = { _ =>
          Future.successful(
            HttpResponse(entity = HttpEntity(ContentTypes.`application/json`, """{"id":"123456789012345678"}"""))
          )
        }
      })
      .build[IdentityApiAdapter] { adapter =>
        val future = Tracing.trace("test")(span => adapter.generate().map(_.id).run(span))
        future pipeTo testActor

        expectMsg("123456789012345678")
      }
  }

  it should "be failure" in {
    baseDesign
      .bind[SimpleAkkaHttpClient].toInstance(new SimpleAkkaHttpClient {
        override def doRequest(): HttpRequest => Future[HttpResponse] = { _ =>
          Future.successful(
            HttpResponse(status = StatusCodes.InternalServerError)
          )
        }
      })
      .build[IdentityApiAdapter] { adapter =>
        val future = Tracing.trace("test")(span => adapter.generate().run(span))
        future pipeTo testActor

        expectMsgType[Status.Failure]
      }
  }

}
