package ninja.grimrose.sandbox.message.cli.task

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Flow
import akka.testkit.TestKit
import ninja.grimrose.sandbox.AkkaSpecHelper
import ninja.grimrose.sandbox.message.MessageId
import ninja.grimrose.sandbox.message.usecase.DeleteMessageUseCase
import org.scalatest.{ DiagrammedAssertions, FlatSpecLike, Matchers }
import wvlet.airframe._

class DeleteTaskSpec
    extends TestKit(ActorSystem("DeleteTaskSpec"))
    with FlatSpecLike
    with Matchers
    with DiagrammedAssertions
    with AkkaSpecHelper {

  import DeleteMessageUseCase._

  behavior of "DeleteTask"

  private val mat = ActorMaterializer()

  private val testDesign = newDesign
    .bind[ActorSystem].toInstance(system)
    .bind[ActorMaterializer].toInstance(mat)
    .bind[DeleteTask].toSingleton

  it should "be success" in {
    testDesign
      .bind[DeleteMessageUseCase].toInstance {
        new DeleteMessageUseCase {
          override def toFlow: Flow[DeleteMessage, DeleteResult, NotUsed] = Flow.fromFunction { message =>
            val id = message.messageId
            assert(id == MessageId(3))

            MessageDeleted(id)
          }
        }
      }
      .build[DeleteTask] { task =>
        val option = TaskOption(messageIds = Seq(MessageId(3)))

        task.run(option)
      }
  }

  it should "be failure" in {
    testDesign
      .bind[DeleteMessageUseCase].toInstance {
        new DeleteMessageUseCase {
          override def toFlow: Flow[DeleteMessage, DeleteResult, NotUsed] = Flow.fromFunction(_ => fail())
        }
      }
      .build[DeleteTask] { task =>
        val option = TaskOption()

        the[IllegalArgumentException] thrownBy {
          task.run(option)
        }
      }
  }

}
