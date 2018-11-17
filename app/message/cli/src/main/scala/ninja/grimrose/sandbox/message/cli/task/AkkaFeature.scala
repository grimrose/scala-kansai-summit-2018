package ninja.grimrose.sandbox.message.cli.task

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import wvlet.airframe._

trait AkkaFeature {
  protected implicit val _system: ActorSystem = bind[ActorSystem]

  protected implicit lazy val _mat: ActorMaterializer = bind[ActorMaterializer]

}
