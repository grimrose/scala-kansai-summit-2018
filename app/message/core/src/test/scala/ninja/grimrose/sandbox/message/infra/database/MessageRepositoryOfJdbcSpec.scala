package ninja.grimrose.sandbox.message.infra.database

import java.time.ZonedDateTime

import cats.instances.future._
import ninja.grimrose.sandbox.message.{ Contents, Message, MessageId, MessageRepository }
import org.scalatest.{ fixture, DiagrammedAssertions, OptionValues }
import scalikejdbc.{ DB, NamedDB }
import scalikejdbc.scalatest.AsyncAutoRollback

class MessageRepositoryOfJdbcSpec
    extends fixture.AsyncFlatSpec
    with TestDBSettings
    with AsyncAutoRollback
    with OptionValues
    with DiagrammedAssertions {

  behavior of "MessageRepositoryOfJdbc"

  override def db(): DB = NamedDB('default).toDB()

  private val repository: MessageRepository = new MessageRepositoryOfJdbc

  it should "be stored" in { implicit session =>
    val context = JdbcContext(session, executionContext)

    val id      = MessageId(1)
    val message = Message(id, Contents("sample"), ZonedDateTime.now())

    val future = (for {
      _      <- repository.store(message)
      entity <- repository.find(id)
    } yield entity).run(context)

    future.map { actual =>
      assert(actual.isDefined)

      assert(actual.value.id == id)
    }
  }

  // TODO delete

  // TODO findAll

}
