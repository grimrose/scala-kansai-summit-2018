package ninja.grimrose.sandbox.message.usecase

import java.time.ZonedDateTime

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Keep
import akka.stream.testkit.scaladsl.{ TestSink, TestSource }
import akka.testkit.TestKit
import cats.data.ReaderT
import ninja.grimrose.sandbox.message._
import ninja.grimrose.sandbox.message.infra.database.{ DBConnectionPoolName, TestDBSettings }
import ninja.grimrose.sandbox.{ AkkaSpecHelper, PersistentContext }
import org.scalatest._
import wvlet.airframe._

import scala.concurrent.{ ExecutionContext, Future }

class FindMessagesUseCaseOfJdbcSpec
    extends TestKit(ActorSystem("FindMessagesUseCaseOfJdbcSpec"))
    with FlatSpecLike
    with Matchers
    with DiagrammedAssertions
    with OptionValues
    with TestDBSettings
    with AkkaSpecHelper {

  import FindMessagesUseCase._

  private implicit val mat: ActorMaterializer = ActorMaterializer()

  private val baseDesign = newDesign
    .bind[FindMessagesUseCase].toInstanceProvider {
      (repository: MessageRepository, poolName: DBConnectionPoolName, executionContext: ExecutionContext) =>
        new FindMessagesUseCaseOfJdbc(repository, poolName, executionContext)
    }
    .bind[DBConnectionPoolName].toInstance(DBConnectionPoolName.of('dummy))
    .bind[ExecutionContext].toInstance(system.dispatcher)

  behavior of "FindMessagesUseCaseOfJdbc"

  it should "be found" in {
    // setup
    val id1 = MessageId(123)
    val id2 = MessageId(456)

    baseDesign
      .bind[MessageRepository]
      .toInstance(new MessageRepository {
        override def store(entity: Message): ReaderTF[PersistentContext, Unit]                = fail()
        override def remove(messageId: MessageId): ReaderTF[PersistentContext, Unit]          = fail()
        override def find(messageId: MessageId): ReaderTF[PersistentContext, Option[Message]] = fail()
        override def findAll(): ReaderTF[PersistentContext, Messages] = ReaderT { _ =>
          Future.successful(
            Messages(
              Seq(
                Message(id1, Contents("found-123"), ZonedDateTime.now()),
                Message(id2, Contents("found-456"), ZonedDateTime.now())
              )
            )
          )
        }
      })
      .withSession { session =>
        val useCase = session.build[FindMessagesUseCase]

        val (pub, sub) =
          TestSource
            .probe[FindMessages]
            .via(useCase.toFlow)
            .toMat(TestSink.probe[Messages])(Keep.both)
            .run()

        // exercise
        sub.request(1)
        pub.sendNext(FindMessages())

        val result = sub.expectNext()
        // verify
        val actual = result.messages
        assert(actual.length == 2)
        assert(actual.map(_.id) == Seq(id1, id2))

        pub.sendComplete()
        sub.expectComplete()
      }
  }

}
