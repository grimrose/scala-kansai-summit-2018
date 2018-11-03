package ninja.grimrose.sandbox

import java.net.InetSocketAddress

import akka.actor.ActorSystem
import com.github.j5ik2o.reactive.memcached.{ MemcachedClient, MemcachedConnection, PeerConfig }
import com.typesafe.config.{ Config, ConfigFactory }
import wvlet.airframe.{ newDesign, Design }

package object katsubushi {

  def design: Design =
    newDesign
      .bind[MemcachedConnection].toInstanceProvider { actorSystem: ActorSystem =>
        implicit val system: ActorSystem = actorSystem

        val config: Config = ConfigFactory.load()

        MemcachedConnection(
          PeerConfig(
            remoteAddress = new InetSocketAddress(
              config.getString("katsubushi.host"),
              config.getInt("katsubushi.port")
            )
          ),
          Some(MemcachedConnection.DEFAULT_DECIDER)
        )
      }
      .bind[MemcachedClient].toSingletonProvider { actorSystem: ActorSystem =>
        implicit val system: ActorSystem = actorSystem

        MemcachedClient()
      }

}
