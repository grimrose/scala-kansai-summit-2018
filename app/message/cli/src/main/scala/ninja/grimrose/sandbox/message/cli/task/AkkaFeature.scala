package ninja.grimrose.sandbox.message.cli.task

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import wvlet.airframe._
import wvlet.log.LogSupport

import scala.concurrent.Await
import scala.concurrent.duration._

trait AkkaFeature extends LogSupport {
  protected implicit val _system: ActorSystem = bind[ActorSystem]
    .onShutdown { system =>
      debug("actor system terminating.")

      _mat.shutdown()

      Await.result(system.terminate(), 1.minutes)

      debug("actor system terminated.")
    }

  protected implicit lazy val _mat: ActorMaterializer = bind[ActorMaterializer]

}
