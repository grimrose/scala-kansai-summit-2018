package ninja.grimrose.sandbox.message.infra.database

import java.time.ZonedDateTime

import cats.instances.future._
import ninja.grimrose.sandbox.message._
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

  it should "be deleted" in { implicit session =>
    val context = JdbcContext(session, executionContext)

    val id      = MessageId(1)
    val message = Message(id, Contents("sample"), ZonedDateTime.now())

    val future = (for {
      _      <- repository.store(message)
      _      <- repository.find(id)
      _      <- repository.remove(id)
      entity <- repository.find(id)
    } yield entity).run(context)

    future.map { actual =>
      assert(actual.isEmpty)
    }
  }

  it should "be find all" in { implicit session =>
    val context = JdbcContext(session, executionContext)

    val message1 = Message(MessageId(1), Contents("sample1"), ZonedDateTime.now())
    val message2 = Message(MessageId(2), Contents("sample2"), ZonedDateTime.now())

    val future = (for {
      _        <- repository.store(message1)
      _        <- repository.store(message2)
      entities <- repository.findAll()
    } yield entities).run(context)

    future.map { actual =>
      val messages = actual.messages

      assert(messages.size == 2)

      assert(messages.map(_.id.value) == Seq(1, 2))

    }
  }

}
