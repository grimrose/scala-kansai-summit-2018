package ninja.grimrose.sandbox.identity

import akka.actor.ActorSystem
import akka.testkit.TestKit
import com.github.j5ik2o.reactive.memcached.MemcachedConnection
import monix.execution.Scheduler
import ninja.grimrose.sandbox.{ katsubushi, AkkaSpecHelper }
import org.scalatest._
import wvlet.airframe._

class IdGeneratorSpec
    extends TestKit(ActorSystem("IdGeneratorSpec"))
    with FlatSpecLike
    with Matchers
    with DiagrammedAssertions
    with AkkaSpecHelper {

  import akka.pattern._

  behavior of "IdGenerator"

  private implicit val scheduler: Scheduler = Scheduler(system.dispatcher)

  it should "be success" in {
    val design = newDesign
      .add(katsubushi.design)
      .bind[IdGenerator].toEagerSingleton
      .bind[ActorSystem].toInstance(system)

    design.withSession { session =>
      val generator  = session.build[IdGenerator]
      val connection = session.build[MemcachedConnection]

      val actual = generator.execute().run(connection).runAsync

      actual.map(_.key).pipeTo(testActor)

      expectMsg("id")
    }
  }

}
