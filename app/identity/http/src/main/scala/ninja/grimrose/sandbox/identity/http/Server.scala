package ninja.grimrose.sandbox.identity.http

import akka.Done
import akka.actor.ActorSystem
import akka.http.scaladsl.server.{ HttpApp, Route }
import akka.stream.ActorMaterializer
import com.github.j5ik2o.reactive.memcached.MemcachedConnection
import wvlet.airframe.bind

import scala.util.Try

trait Server extends HttpApp {
  implicit val system: ActorSystem             = bind[ActorSystem]
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  private val connection: MemcachedConnection = bind[MemcachedConnection]

  def routes: Route = bind[Routes].routes

  override protected def postServerShutdown(attempt: Try[Done], system: ActorSystem): Unit = {
    connection.shutdown()

    materializer.shutdown()

    super.postServerShutdown(attempt, system)
  }
}
