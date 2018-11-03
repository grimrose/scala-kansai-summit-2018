package ninja.grimrose.sandbox

import akka.actor.ActorSystem
import akka.stream.Materializer
import ninja.grimrose.sandbox.message.gateway.IdentityApiAdapter
import ninja.grimrose.sandbox.message.infra.database.{ DBConnectionPoolName, MessageRepositoryOfJdbc }
import ninja.grimrose.sandbox.message.infra.network.IdentityApiAdapterImpl
import ninja.grimrose.sandbox.message.usecase._
import skinny.SkinnyConfig
import wvlet.airframe._

import scala.concurrent.ExecutionContext

package object message {

  def design: Design =
    newDesign
      .bind[IdentityApiAdapter].toSingletonProvider {
        (config: SkinnyConfig, actorSystem: ActorSystem, materializer: Materializer) =>
          new IdentityApiAdapterImpl(config, actorSystem, materializer)
      }
      .bind[MessageRepository].toInstance(new MessageRepositoryOfJdbc)
      .bind[DBConnectionPoolName].toSingleton
      .bind[ExecutionContext].toProvider { actorSystem: ActorSystem =>
        actorSystem.dispatchers.lookup("scalikejdbc-dispatcher")
      }
      .bind[FindMessagesUseCase].toInstanceProvider {
        (repository: MessageRepository, poolName: DBConnectionPoolName, executionContext: ExecutionContext) =>
          new FindMessagesUseCaseOfJdbc(repository, poolName, executionContext)
      }
      .bind[FindByIdMessageUseCase].toInstanceProvider {
        (repository: MessageRepository, poolName: DBConnectionPoolName, executionContext: ExecutionContext) =>
          new FindByIdMessageUseCaseOfJdbc(repository, poolName, executionContext)
      }
      .bind[DeleteMessageUseCase].toInstanceProvider {
        (repository: MessageRepository, poolName: DBConnectionPoolName, executionContext: ExecutionContext) =>
          new DeleteMessageUseCaseOfJdbc(repository, poolName, executionContext)
      }
      .bind[CreateMessageUseCase].toInstanceProvider {
        (repository: MessageRepository,
         poolName: DBConnectionPoolName,
         executionContext: ExecutionContext,
         identityApi: IdentityApiAdapter) =>
          new CreateMessageUseCaseOfJdbc(repository, poolName, executionContext, identityApi)
      }
}
