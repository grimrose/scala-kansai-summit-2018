package ninja.grimrose.sandbox.message.cli.task

import akka.stream.scaladsl.Source
import io.opencensus.scala.Tracing
import ninja.grimrose.sandbox.message.usecase.CreateMessageUseCase
import wvlet.airframe._

import scala.concurrent.Await
import scala.concurrent.duration._

trait PostTask extends DefaultTask {

  private val createMessageUseCase = bind[CreateMessageUseCase]

  override def run(option: TaskOption): Unit = {
    import CreateMessageUseCase._

    val span = Tracing.startSpan("PostTask")

    val future = Source
      .single(option.contents).flatMapConcat {
        case Some(contents) => Source.single(CreateMessage(contents, span))
        case None           => Source.failed(new IllegalArgumentException("contents not found."))
      }
      .via(createMessageUseCase.toFlow)
      .runForeach { result =>
        info(s"${result.id}")
      }

    Await.result(future, 10.minutes)

    span.end()
  }

}
