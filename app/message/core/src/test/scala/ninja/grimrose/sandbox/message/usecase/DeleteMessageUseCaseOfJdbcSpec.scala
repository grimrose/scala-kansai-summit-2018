package ninja.grimrose.sandbox.message.usecase

import java.time.ZonedDateTime

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Keep
import akka.stream.testkit.scaladsl.{ TestSink, TestSource }
import akka.testkit.TestKit
import cats.data.ReaderT
import ninja.grimrose.sandbox._
import ninja.grimrose.sandbox.message._
import ninja.grimrose.sandbox.message.infra.database.{ DBConnectionPoolName, TestDBSettings }
import org.scalatest._
import wvlet.airframe._

import scala.concurrent.{ ExecutionContext, Future }

class DeleteMessageUseCaseOfJdbcSpec
    extends TestKit(ActorSystem("RemoveMessageUseCaseOfJdbcSpec"))
    with FlatSpecLike
    with Matchers
    with DiagrammedAssertions
    with OptionValues
    with AkkaSpecHelper
    with TestDBSettings {

  import DeleteMessageUseCase._

  private implicit val mat: ActorMaterializer = ActorMaterializer()

  private val baseDesign = newDesign
    .bind[DeleteMessageUseCase].toInstanceProvider {
      (repository: MessageRepository, poolName: DBConnectionPoolName, executionContext: ExecutionContext) =>
        new DeleteMessageUseCaseOfJdbc(repository, poolName, executionContext)
    }
    .bind[DBConnectionPoolName].toInstance(DBConnectionPoolName.of('dummy))
    .bind[ExecutionContext].toInstance(system.dispatcher)

  behavior of "RemoveMessageUseCaseOfJdbc"

  it should "be removed" in {
    // setup
    val id = MessageId(1)

    baseDesign
      .bind[MessageRepository]
      .toInstance(new MessageRepository {
        override def store(entity: Message): ReaderTF[PersistentContext, Unit] = fail()
        override def remove(messageId: MessageId): ReaderTF[PersistentContext, Unit] =
          ReaderT { _ =>
            Future.successful(())
          }
        override def find(messageId: MessageId): ReaderTF[PersistentContext, Option[Message]] =
          ReaderT { _ =>
            Future.successful(Option(Message(messageId, Contents("remove"), ZonedDateTime.now())))
          }
        override def findAll(): ReaderTF[PersistentContext, Messages] = fail()
      })
      .withSession { session =>
        val useCase = session.build[DeleteMessageUseCase]

        val (pub, sub) =
          TestSource
            .probe[DeleteMessage]
            .via(useCase.toFlow)
            .toMat(TestSink.probe[DeleteResult])(Keep.both)
            .run()

        // exercise
        sub.request(1)
        pub.sendNext(DeleteMessage(id))

        val result = sub.expectNext()
        // verify
        assert(result == MessageDeleted(id))

        pub.sendComplete()
        sub.expectComplete()
      }
  }

  it should "be not found" in {
    // setup
    val id = MessageId(1)

    val design = baseDesign
      .bind[MessageRepository]
      .toInstance(new MessageRepository {
        override def store(entity: Message): ReaderTF[PersistentContext, Unit]       = fail()
        override def remove(messageId: MessageId): ReaderTF[PersistentContext, Unit] = fail()
        override def find(messageId: MessageId): ReaderTF[PersistentContext, Option[Message]] =
          ReaderT { _ =>
            Future.successful(Option.empty[Message])
          }
        override def findAll(): ReaderTF[PersistentContext, Messages] = fail()
      })

    design.withSession { session =>
      val useCase = session.build[DeleteMessageUseCase]

      val (pub, sub) =
        TestSource
          .probe[DeleteMessage]
          .via(useCase.toFlow)
          .toMat(TestSink.probe[DeleteResult])(Keep.both)
          .run()

      // exercise
      sub.request(1)
      pub.sendNext(DeleteMessage(id))

      val result = sub.expectNext()
      // verify
      assert(result == NotFound(id))

      pub.sendComplete()
      sub.expectComplete()
    }
  }

}
