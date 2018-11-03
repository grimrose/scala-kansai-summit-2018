package ninja.grimrose.sandbox

import akka.actor.ActorSystem
import akka.testkit.TestKit
import org.scalatest.{ BeforeAndAfterAll, Suite }

trait AkkaSpecHelper extends BeforeAndAfterAll { self: Suite =>
  def system: ActorSystem

  override protected def afterAll(): Unit = {
    super.afterAll()

    TestKit.shutdownActorSystem(system)
  }
}
