package ninja.grimrose.sandbox.message.usecase

import akka.NotUsed
import akka.event.Logging
import akka.stream.scaladsl.Flow
import akka.stream.{ ActorAttributes, Attributes }
import ninja.grimrose.sandbox.message.infra.database.{ DBConnectionPoolName, JdbcContextFeature }
import ninja.grimrose.sandbox.message.usecase.FindMessagesUseCase.FindMessages
import ninja.grimrose.sandbox.message.{ MessageId, MessageRepository, Messages }

import scala.concurrent.ExecutionContext

trait FindMessagesUseCase {

  def toFlow: Flow[FindMessages, Messages, NotUsed]

}

object FindMessagesUseCase {
  // TODO parentSpan
  case class FindMessages(ids: Seq[MessageId] = Nil)
}

class FindMessagesUseCaseOfJdbc(repository: MessageRepository, poolName: DBConnectionPoolName, ec: ExecutionContext)
    extends FindMessagesUseCase
    with JdbcContextFeature {

  override def toFlow: Flow[FindMessages, Messages, NotUsed] =
    Flow[FindMessages]
      .mapAsyncUnordered(1) { _ =>
        runReadOnly(ec, poolName)(repository.findAll())
      }
      .withAttributes(ActorAttributes.dispatcher("scalikejdbc-dispatcher"))
      .log("findMessages")
      .withAttributes(Attributes.logLevels(onElement = Logging.InfoLevel))

}
