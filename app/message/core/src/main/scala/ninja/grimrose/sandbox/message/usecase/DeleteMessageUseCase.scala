package ninja.grimrose.sandbox.message.usecase

import akka.NotUsed
import akka.event.Logging
import akka.stream.scaladsl.Flow
import akka.stream.{ ActorAttributes, Attributes }
import cats.data._
import cats.instances.future._
import ninja.grimrose.sandbox.PersistentContext
import ninja.grimrose.sandbox.message.infra.database.{ DBConnectionPoolName, JdbcContextFeature }
import ninja.grimrose.sandbox.message.usecase.DeleteMessageUseCase.{ DeleteMessage, DeleteResult }
import ninja.grimrose.sandbox.message.{ MessageId, MessageRepository }

import scala.concurrent.{ ExecutionContext, Future }

trait DeleteMessageUseCase {

  def toFlow: Flow[DeleteMessage, DeleteResult, NotUsed]
}

object DeleteMessageUseCase {

  case class DeleteMessage(messageId: MessageId)

  sealed abstract class DeleteResult(val messageId: MessageId) extends Serializable

  case class NotFound(override val messageId: MessageId) extends DeleteResult(messageId)

  case class MessageDeleted(override val messageId: MessageId) extends DeleteResult(messageId)

}

class DeleteMessageUseCaseOfJdbc(repository: MessageRepository,
                                 poolName: DBConnectionPoolName,
                                 executionContext: ExecutionContext)
    extends DeleteMessageUseCase
    with JdbcContextFeature {
  import DeleteMessageUseCase._

  implicit val _ec: ExecutionContext = executionContext

  override def toFlow: Flow[DeleteMessage, DeleteResult, NotUsed] =
    Flow[DeleteMessage]
      .map(_.messageId)
      .mapAsyncUnordered(1) { id =>
        val reader: ReaderT[Future, PersistentContext, DeleteResult] = repository.find(id).flatMap {
          case Some(_) => repository.remove(id).map(_ => MessageDeleted(id))
          case None =>
            ReaderT[Future, PersistentContext, DeleteResult] { _ =>
              Future.successful(NotFound(id))
            }
        }
        runFutureLocalTx(executionContext, poolName)(reader)
      }
      .withAttributes(ActorAttributes.dispatcher("scalikejdbc-dispatcher"))
      .log("deleteMessage")
      .withAttributes(Attributes.logLevels(onElement = Logging.InfoLevel))

}
