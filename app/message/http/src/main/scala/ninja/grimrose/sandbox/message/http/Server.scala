package ninja.grimrose.sandbox.message.http

import akka.Done
import akka.actor.ActorSystem
import akka.http.scaladsl.server.{ HttpApp, Route }
import akka.stream.ActorMaterializer
import ninja.grimrose.sandbox.message.infra.database.DBSettings
import wvlet.airframe._

import scala.util.Try

trait Server extends HttpApp {
  implicit val system: ActorSystem             = bind[ActorSystem]
  implicit val materializer: ActorMaterializer = bind[ActorMaterializer]

  DBSettings.initialize()

  def routes: Route = bind[Routes].routes

  override protected def postServerShutdown(attempt: Try[Done], system: ActorSystem): Unit = {

    DBSettings.destroy()

    materializer.shutdown()

    super.postServerShutdown(attempt, system)
  }

}
