package ninja.grimrose.sandbox.message.infra.database

import java.time.ZonedDateTime

import cats.data._
import ninja.grimrose.sandbox.PersistentContext
import ninja.grimrose.sandbox.message._
import scalikejdbc._

import scala.concurrent.{ ExecutionContext, Future }

class MessageRepositoryOfJdbc extends MessageRepository {

  import JdbcContext._
  import MessageRecord._

  override def store(entity: Message): ReaderTF[PersistentContext, Unit] = ReaderT { ctx =>
    val context: JdbcContext = ctx.ofJdbc

    implicit val session: DBSession   = context.session
    implicit val ec: ExecutionContext = context.executionContext

    Future {
      val m = MessageRecord.column

      MessageRecord.createWithNamedValues(
        m.id        -> entity.id.value,
        m.contents  -> entity.contents.value,
        m.createdAt -> ZonedDateTime.now
      )
    }
  }

  override def remove(messageId: MessageId): ReaderTF[PersistentContext, Unit] = ReaderT { ctx =>
    val context: JdbcContext = ctx.ofJdbc

    implicit val session: DBSession   = context.session
    implicit val ec: ExecutionContext = context.executionContext

    Future {
      val id = messageId.value

      MessageRecord.deleteById(id)
    }
  }

  override def find(messageId: MessageId): ReaderTF[PersistentContext, Option[Message]] = ReaderT { ctx =>
    val context: JdbcContext = ctx.ofJdbc

    implicit val session: DBSession   = context.session
    implicit val ec: ExecutionContext = context.executionContext

    Future {
      val id = messageId.value

      MessageRecord.findById(id).map(_.asEntity)
    }
  }

  override def findAll(): ReaderTF[PersistentContext, Messages] = ReaderT { ctx =>
    val context: JdbcContext = ctx.ofJdbc

    implicit val session: DBSession   = context.session
    implicit val ec: ExecutionContext = context.executionContext

    Future {
      val m = MessageRecord.column
      Messages(MessageRecord.findAll(Seq(m.id)).map(_.asEntity))
    }
  }

}
