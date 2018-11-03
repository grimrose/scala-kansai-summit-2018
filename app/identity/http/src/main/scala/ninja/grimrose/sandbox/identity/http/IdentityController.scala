package ninja.grimrose.sandbox.identity.http

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{ Directives, Route }
import com.github.j5ik2o.reactive.memcached.MemcachedConnection
import io.opencensus.scala.akka.http.TracingDirective._
import io.opencensus.trace.AttributeValue
import monix.execution.Scheduler
import ninja.grimrose.sandbox.identity.IdGenerator
import wvlet.airframe._

import scala.util.{ Failure, Success }

trait IdentityController extends Directives with SprayJsonSupport {

  private val generator: IdGenerator = bind[IdGenerator]

  private val connection: MemcachedConnection = bind[MemcachedConnection]

  import spray.json.DefaultJsonProtocol._

  def routes: Route = path("identity") {
    optionalHeaderValueByName("X-B3-TraceId") { traceId =>
      traceRequest { span =>
        span.putAttribute("X-B3-TraceId", AttributeValue.stringAttributeValue(traceId.getOrElse("")))

        extractActorSystem { implicit system =>
          implicit val scheduler: Scheduler = Scheduler(system.dispatchers.lookup("katsubushi-dispatcher"))

          val future = generator.execute().map(_.value).run(connection).runAsync

          onComplete(future) {
            case Success(id) => complete(StatusCodes.OK -> Map("id" -> id))
            case Failure(_) =>
              complete(StatusCodes.NotFound -> Map("rejection" -> s"${StatusCodes.NotFound.defaultMessage}"))
          }
        }
      }
    }
  }

}
