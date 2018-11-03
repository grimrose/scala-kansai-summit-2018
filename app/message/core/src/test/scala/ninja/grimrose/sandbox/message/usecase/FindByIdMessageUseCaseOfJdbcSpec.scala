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

class FindByIdMessageUseCaseOfJdbcSpec
    extends TestKit(ActorSystem("FindByIdMessageUseCaseOfJdbcSpec"))
    with FlatSpecLike
    with Matchers
    with DiagrammedAssertions
    with OptionValues
    with TestDBSettings
    with AkkaSpecHelper {

  import FindByIdMessageUseCase._

  private implicit val mat: ActorMaterializer = ActorMaterializer()

  private val baseDesign = newDesign
    .bind[FindByIdMessageUseCase].toInstanceProvider {
      (repository: MessageRepository, poolName: DBConnectionPoolName, executionContext: ExecutionContext) =>
        new FindByIdMessageUseCaseOfJdbc(repository, poolName, executionContext)
    }
    .bind[DBConnectionPoolName].toInstance(DBConnectionPoolName.of('dummy))
    .bind[ExecutionContext].toInstance(system.dispatcher)

  behavior of "FindByIdMessageUseCaseOfJdbc"

  it should "be found" in {
    // setup
    val id = MessageId(1)

    baseDesign
      .bind[MessageRepository]
      .toInstance(new MessageRepository {
        override def store(entity: Message): ReaderTF[PersistentContext, Unit]       = fail()
        override def remove(messageId: MessageId): ReaderTF[PersistentContext, Unit] = fail()
        override def find(messageId: MessageId): ReaderTF[PersistentContext, Option[Message]] =
          ReaderT { _ =>
            Future.successful(Option(Message(messageId, Contents("found"), ZonedDateTime.now())))
          }
        override def findAll(): ReaderTF[PersistentContext, Messages] = fail()
      })
      .withSession { session =>
        val useCase = session.build[FindByIdMessageUseCase]

        val (pub, sub) =
          TestSource
            .probe[FindByIdMessage]
            .via(useCase.toFlow)
            .toMat(TestSink.probe[Option[Message]])(Keep.both)
            .run()

        // exercise
        sub.request(1)
        pub.sendNext(FindByIdMessage(id))

        val result = sub.expectNext()
        // verify
        assert(result.value.id == id)

        pub.sendComplete()
        sub.expectComplete()
      }
  }
}
