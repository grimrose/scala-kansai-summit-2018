package ninja.grimrose.sandbox.identity.http

import java.util.UUID

import akka.actor.ActorSystem
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{ ContentTypes, HttpRequest, StatusCodes }
import akka.http.scaladsl.testkit.{ RouteTestTimeout, ScalatestRouteTest }
import com.github.j5ik2o.reactive.memcached.command.{ CommandRequestBase, ValueDesc }
import com.github.j5ik2o.reactive.memcached.{
  MemcachedConnection,
  PeerConfig,
  ReaderTTask,
  ReaderTTaskMemcachedConnection
}
import monix.eval.Task
import ninja.grimrose.sandbox.identity.IdGenerator
import ninja.grimrose.sandbox.katsubushi
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{ Matchers, WordSpec }
import wvlet.airframe._

import scala.concurrent.duration._

class IdentityControllerSpec extends WordSpec with Matchers with ScalaFutures with ScalatestRouteTest {
  implicit val timeout: RouteTestTimeout = RouteTestTimeout(10.seconds)

  private val baseDesign = newDesign
    .add(katsubushi.design)
    .bind[ActorSystem].toInstance(system)
    .bind[MemcachedConnection].toInstance(new MemcachedConnection {
      // mocking
      override def id: UUID                                                  = fail()
      override def shutdown(): Unit                                          = fail()
      override def peerConfig: Option[PeerConfig]                            = fail()
      override def send[C <: CommandRequestBase](cmd: C): Task[cmd.Response] = fail()
    })
    .bind[IdentityController].toSingleton

  private val tracingHeaders = List(
    RawHeader("X-B3-TraceId", "12345678901234567890123456789012"),
    RawHeader("X-B3-SpanId", "1234567890123456")
  )

  "IdentityController" should {
    "return identity (GET /identity)" in {
      val design = baseDesign
        .bind[IdGenerator].to[SuccessIdGenerator]

      design.withSession { session =>
        val routes = session.build[IdentityController].routes

        val request = HttpRequest(
          uri = "/identity",
          headers = tracingHeaders
        )
        request ~> routes ~> check {
          status should ===(StatusCodes.OK)

          contentType should ===(ContentTypes.`application/json`)

          entityAs[String] should ===("""{"id":"502423337796915200"}""")
        }
      }
    }

    "catch an exception in the failure case (GET /identity)" in {
      val design = baseDesign
        .bind[IdGenerator].to[FailureIdGenerator]

      design.withSession { session =>
        val routes = session.build[IdentityController].routes

        val request = HttpRequest(
          uri = "/identity",
          headers = tracingHeaders
        )
        request ~> routes ~> check {
          status should ===(StatusCodes.NotFound)

          contentType should ===(ContentTypes.`application/json`)

          entityAs[String] should ===(
            """{"rejection":"The requested resource could not be found but may be available again in the future."}"""
          )
        }
      }
    }
  }

  trait SuccessIdGenerator extends IdGenerator {
    override def execute(): ReaderTTaskMemcachedConnection[ValueDesc] =
      ReaderTTask.pure(ValueDesc(key = "id", value = "502423337796915200", expire = 18, flags = 0))
  }

  trait FailureIdGenerator extends IdGenerator {
    override def execute(): ReaderTTaskMemcachedConnection[ValueDesc] =
      ReaderTTask.raiseError(new IllegalArgumentException("not found."))
  }
}
