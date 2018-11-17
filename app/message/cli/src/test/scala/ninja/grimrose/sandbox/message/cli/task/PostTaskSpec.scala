package ninja.grimrose.sandbox.message.cli.task

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Flow
import akka.testkit.TestKit
import ninja.grimrose.sandbox.AkkaSpecHelper
import ninja.grimrose.sandbox.message.usecase.CreateMessageUseCase
import ninja.grimrose.sandbox.message.{ Contents, MessageId }
import org.scalatest.{ DiagrammedAssertions, FlatSpecLike, Matchers }
import wvlet.airframe._

class PostTaskSpec
    extends TestKit(ActorSystem("PostTaskSpec"))
    with FlatSpecLike
    with Matchers
    with DiagrammedAssertions
    with AkkaSpecHelper {
  import CreateMessageUseCase._

  behavior of "PostTask"

  private val mat = ActorMaterializer()

  private val testDesign = newDesign
    .bind[ActorSystem].toInstance(system)
    .bind[ActorMaterializer].toInstance(mat)
    .bind[PostTask].toSingleton

  it should "be success" in {
    val id = MessageId(1)

    testDesign
      .bind[CreateMessageUseCase].toInstance {
        new CreateMessageUseCase {
          override def toFlow: Flow[CreateMessage, MessageCreated, NotUsed] = Flow.fromFunction { message =>
            assert(message.contents == Contents("success"))

            MessageCreated(id)
          }
        }
      }
      .build[PostTask] { task =>
        val option = TaskOption(contents = Some(Contents("success")))

        task.run(option)
      }
  }

  it should "be failure" in {
    testDesign
      .bind[CreateMessageUseCase].toInstance {
        new CreateMessageUseCase {
          override def toFlow: Flow[CreateMessage, MessageCreated, NotUsed] = Flow.fromFunction { _ =>
            fail()
          }
        }
      }
      .build[PostTask] { task =>
        val option = TaskOption()

        the[IllegalArgumentException] thrownBy {
          task.run(option)
        }
      }
  }
}
