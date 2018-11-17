package ninja.grimrose.sandbox.message.cli.task

import java.time.ZonedDateTime

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Flow
import akka.testkit.TestKit
import ninja.grimrose.sandbox.AkkaSpecHelper
import ninja.grimrose.sandbox.message.{ Contents, Message, MessageId, Messages }
import ninja.grimrose.sandbox.message.usecase.{ FindByIdMessageUseCase, FindMessagesUseCase }
import org.scalatest.{ DiagrammedAssertions, FlatSpecLike, Matchers }
import wvlet.airframe.newDesign

class MessagesTaskSpec
    extends TestKit(ActorSystem("MessagesTaskSpec"))
    with FlatSpecLike
    with Matchers
    with DiagrammedAssertions
    with AkkaSpecHelper {

  import FindMessagesUseCase._
  import FindByIdMessageUseCase._

  behavior of "MessagesTask"

  private val mat = ActorMaterializer()

  private val testDesign = newDesign
    .bind[ActorSystem].toInstance(system)
    .bind[ActorMaterializer].toInstance(mat)
    .bind[MessagesTask].toSingleton

  it should "be success by id" in {
    testDesign
      .bind[FindByIdMessageUseCase].toInstance {
        new FindByIdMessageUseCase {
          override def toFlow: Flow[FindByIdMessage, Option[Message], NotUsed] = Flow.fromFunction { message =>
            val actual = message.messageId

            assert(actual == MessageId(5))

            Some(Message(message.messageId, Contents("single"), ZonedDateTime.now()))
          }
        }
      }
      .bind[FindMessagesUseCase].toInstance {
        new FindMessagesUseCase {
          override def toFlow: Flow[FindMessages, Messages, NotUsed] = Flow.fromFunction(_ => fail())
        }
      }
      .build[MessagesTask] { task =>
        val option = TaskOption(Seq(MessageId(5)))

        task.run(option)
      }
  }

  it should "be success by id when entity not found" in {
    testDesign
      .bind[FindByIdMessageUseCase].toInstance {
        new FindByIdMessageUseCase {
          override def toFlow: Flow[FindByIdMessage, Option[Message], NotUsed] = Flow.fromFunction { message =>
            val actual = message.messageId

            assert(actual == MessageId(6))

            None
          }
        }
      }
      .bind[FindMessagesUseCase].toInstance {
        new FindMessagesUseCase {
          override def toFlow: Flow[FindMessages, Messages, NotUsed] = Flow.fromFunction(_ => fail())
        }
      }
      .build[MessagesTask] { task =>
        val option = TaskOption(Seq(MessageId(6)))

        task.run(option)
      }
  }

  it should "be success by ids" in {
    testDesign
      .bind[FindByIdMessageUseCase].toInstance {
        new FindByIdMessageUseCase {
          override def toFlow: Flow[FindByIdMessage, Option[Message], NotUsed] = Flow.fromFunction(_ => fail())
        }
      }
      .bind[FindMessagesUseCase].toInstance {
        new FindMessagesUseCase {
          override def toFlow: Flow[FindMessages, Messages, NotUsed] = Flow.fromFunction { message =>
            assert(message.ids == Seq(MessageId(7), MessageId(8)))

            Messages(Seq.empty[Message])
          }
        }
      }
      .build[MessagesTask] { task =>
        val option = TaskOption(
          Seq(
            MessageId(7),
            MessageId(8)
          )
        )

        task.run(option)
      }
  }

}
