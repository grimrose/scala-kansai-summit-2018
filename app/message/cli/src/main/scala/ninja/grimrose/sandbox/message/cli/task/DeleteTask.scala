package ninja.grimrose.sandbox.message.cli.task

import akka.stream.scaladsl.Source
import ninja.grimrose.sandbox.message.usecase.DeleteMessageUseCase
import wvlet.airframe._

import scala.concurrent.Await
import scala.concurrent.duration._

trait DeleteTask extends DefaultTask {

  private val deleteMessageUseCase = bind[DeleteMessageUseCase]

  override def run(option: TaskOption): Unit = {
    import DeleteMessageUseCase._

    val future = Source
      .single(option.messageIds.headOption)
      .flatMapConcat {
        case Some(id) => Source.single(DeleteMessage(id))
        case None     => Source.failed(new IllegalArgumentException("require input id."))
      }
      .via(deleteMessageUseCase.toFlow)
      .runForeach {
        case NotFound(messageId)       => warn(s"already deleted or missing. [id:${messageId.value}].")
        case MessageDeleted(messageId) => info(s"deleted. [id:${messageId.value}]")
      }

    Await.result(future, 10.minutes)
  }
}
