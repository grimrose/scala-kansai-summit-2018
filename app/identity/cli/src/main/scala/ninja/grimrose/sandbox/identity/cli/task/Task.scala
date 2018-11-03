package ninja.grimrose.sandbox.identity.cli.task

import akka.actor.ActorSystem
import com.github.j5ik2o.reactive.memcached.MemcachedConnection
import monix.execution.Scheduler
import ninja.grimrose.sandbox.identity.IdGenerator
import wvlet.airframe._
import wvlet.log.LogSupport

import scala.concurrent.Await
import scala.concurrent.duration._

trait Task extends LogSupport {

  implicit val _system: ActorSystem = bind[ActorSystem]
    .onShutdown { system =>
      debug("actor system terminating.")

      Await.result(system.terminate(), 1.minutes)

      debug("actor system terminated.")
    }

  val generator: IdGenerator = bind[IdGenerator]

  val connection: MemcachedConnection = bind[MemcachedConnection]

  def run(): Unit = {
    implicit val scheduler: Scheduler = Scheduler(_system.dispatchers.lookup("katsubushi-dispatcher"))

    generator.execute().run(connection).runAsync.onComplete {
      case scala.util.Success(value)     => info(value.value)
      case scala.util.Failure(exception) => error(exception.getMessage, exception)
    }
  }

}
