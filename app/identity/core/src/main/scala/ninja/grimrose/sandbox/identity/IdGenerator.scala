package ninja.grimrose.sandbox.identity

import cats.data.ReaderT
import com.github.j5ik2o.reactive.memcached.command.ValueDesc
import com.github.j5ik2o.reactive.memcached.{ MemcachedClient, MemcachedConnection, ReaderTTaskMemcachedConnection }
import monix.eval.Task

import wvlet.airframe._

trait IdGenerator {

  val client: MemcachedClient = bind[MemcachedClient]

  def execute(): ReaderTTaskMemcachedConnection[ValueDesc] =
    ReaderT[Task, MemcachedConnection, ValueDesc] { conn =>
      client.get("id").run(conn).map {
        case Some(value) => value
        case None        => throw new IllegalArgumentException("not found.")
      }
    }

}
