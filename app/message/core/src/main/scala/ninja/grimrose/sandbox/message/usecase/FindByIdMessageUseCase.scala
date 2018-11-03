package ninja.grimrose.sandbox.message.usecase

import akka.NotUsed
import akka.event.Logging
import akka.stream.scaladsl.Flow
import akka.stream.{ ActorAttributes, Attributes }
import ninja.grimrose.sandbox.message.infra.database.{ DBConnectionPoolName, JdbcContextFeature }
import ninja.grimrose.sandbox.message.usecase.FindByIdMessageUseCase.FindByIdMessage
import ninja.grimrose.sandbox.message.{ Message, MessageId, MessageRepository }

import scala.concurrent.ExecutionContext

trait FindByIdMessageUseCase {

  def toFlow: Flow[FindByIdMessage, Option[Message], NotUsed]

}

object FindByIdMessageUseCase {

  case class FindByIdMessage(messageId: MessageId)
}

class FindByIdMessageUseCaseOfJdbc(repository: MessageRepository, poolName: DBConnectionPoolName, ec: ExecutionContext)
    extends FindByIdMessageUseCase
    with JdbcContextFeature {

  override def toFlow: Flow[FindByIdMessage, Option[Message], NotUsed] =
    Flow[FindByIdMessage]
      .map(_.messageId)
      .mapAsyncUnordered(1) { id =>
        runReadOnly(ec, poolName)(repository.find(id))
      }
      .withAttributes(ActorAttributes.dispatcher("scalikejdbc-dispatcher"))
      .log("findMessage")
      .withAttributes(Attributes.logLevels(onElement = Logging.InfoLevel))
}
