package ninja.grimrose.sandbox.message.usecase

import akka.actor.ActorSystem
import akka.stream.scaladsl.Keep
import akka.stream.testkit.scaladsl.{ TestSink, TestSource }
import akka.stream.{ ActorMaterializer, Materializer }
import akka.testkit.TestKit
import cats.data.ReaderT
import io.opencensus.scala.Tracing
import ninja.grimrose.sandbox.message._
import ninja.grimrose.sandbox.message.gateway.{ IdentityApiAdapter, IdentityApiResponse }
import ninja.grimrose.sandbox.message.infra.database.{ DBConnectionPoolName, TestDBSettings }
import ninja.grimrose.sandbox.{ AkkaSpecHelper, PersistentContext }
import org.scalatest._

import scala.concurrent.{ ExecutionContext, Future }

class CreateMessageUseCaseOfJdbcSpec
    extends TestKit(ActorSystem("CreateMessageUseCaseOfJdbcSpec"))
    with FlatSpecLike
    with Matchers
    with DiagrammedAssertions
    with OptionValues
    with TestDBSettings
    with AkkaSpecHelper {

  import CreateMessageUseCase._
  import ninja.grimrose.sandbox.message.design

  private implicit val mat: Materializer = ActorMaterializer()

  private val baseDesign = design
    .bind[DBConnectionPoolName].toInstance(DBConnectionPoolName.of('dummy))
    .bind[ExecutionContext].toInstance(system.dispatcher)
    .bind[IdentityApiAdapter].toInstance(() => ReaderT(_ => Future.successful(IdentityApiResponse("123"))))

  behavior of "CreateMessageUseCaseOfJdbc"

  it should "be success" in {
    baseDesign
      .bind[MessageRepository].toInstance(new MessageRepository {
        override def store(entity: Message): ReaderTF[PersistentContext, Unit] = ReaderT { _ =>
          Future.successful(())
        }
        override def remove(messageId: MessageId): ReaderTF[PersistentContext, Unit]          = ReaderT(_ => fail())
        override def find(messageId: MessageId): ReaderTF[PersistentContext, Option[Message]] = ReaderT(_ => fail())
        override def findAll(): ReaderTF[PersistentContext, Messages]                         = ReaderT(_ => fail())
      })
      .build[CreateMessageUseCase] { useCase =>
        val (pub, sub) =
          TestSource
            .probe[CreateMessage]
            .via(useCase.toFlow)
            .toMat(TestSink.probe[MessageCreated])(Keep.both)
            .run()

        // exercise
        val span = Tracing.startSpan("test")

        sub.request(1)
        pub.sendNext(CreateMessage(Contents("sample"), span))

        val result = sub.expectNext()
        // verify
        span.end()
        assert(result == MessageCreated(MessageId(123)))

        pub.sendComplete()
        sub.expectComplete()
      }
  }
}
