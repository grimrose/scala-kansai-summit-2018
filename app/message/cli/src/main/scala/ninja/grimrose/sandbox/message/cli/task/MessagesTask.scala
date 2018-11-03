package ninja.grimrose.sandbox.message.cli.task

import java.time.format.DateTimeFormatter

import akka.{ Done, NotUsed }
import akka.event.Logging
import akka.stream.{ Attributes, FlowShape }
import akka.stream.scaladsl.{ Flow, GraphDSL, Merge, Partition, Sink, Source }
import ninja.grimrose.sandbox.message.Messages
import ninja.grimrose.sandbox.message.usecase.{ FindByIdMessageUseCase, FindMessagesUseCase }
import wvlet.airframe._

import scala.concurrent.{ Await, Future }
import scala.concurrent.duration._

trait MessagesTask extends DefaultTask {
  import FindByIdMessageUseCase._
  import FindMessagesUseCase._

  private val findMessagesUseCase = bind[FindMessagesUseCase]

  private val findByIdMessageUseCase = bind[FindByIdMessageUseCase]

  override def run(option: TaskOption): Unit = {
    val future = Source
      .single(option)
      .via(messageFlow)
      .runWith(loggingSink)

    Await.result(future, 10.minutes)
  }

  private def messageFlow: Flow[TaskOption, Messages, NotUsed] =
    Flow
      .fromGraph(GraphDSL.create() { implicit builder =>
        import GraphDSL.Implicits._

        val function = (option: TaskOption) => {
          option.messageIds match {
            case _ :: Nil => 0
            case _        => 1
          }
        }
        val partition = builder.add(Partition[TaskOption](2, function))

        val mapOne = builder.add(mapToFindByIdMessageFlow)

        val mapMany = builder.add(mapToFindMessagesFlow)

        val findById = builder.add(findByIdFlow)

        val findMessages = builder.add(findMessagesUseCase.toFlow)

        val merge = builder.add(Merge[Messages](2))

        // format: off

        partition ~> mapOne  ~> findById     ~> merge
        partition ~> mapMany ~> findMessages ~> merge

        // format: on

        FlowShape(partition.in, merge.out)
      })

  private def mapToFindByIdMessageFlow: Flow[TaskOption, FindByIdMessage, NotUsed] =
    Flow[TaskOption]
      .mapConcat { option: TaskOption =>
        option.messageIds match {
          case id :: Nil => FindByIdMessage(id) :: Nil
          case _         => Nil // partitionでここには来ない
        }
      }
      .log("FindByIdMessage")
      .withAttributes(Attributes.logLevels(onElement = Logging.DebugLevel))

  private def mapToFindMessagesFlow: Flow[TaskOption, FindMessages, NotUsed] =
    Flow[TaskOption]
      .mapConcat { option: TaskOption =>
        option.messageIds match {
          case _ :: Nil => Nil
          case ids      => FindMessages(ids) :: Nil
        }
      }.log("FindMessages")
      .withAttributes(Attributes.logLevels(onElement = Logging.DebugLevel))

  private def findByIdFlow =
    findByIdMessageUseCase.toFlow
      .map {
        case Some(message) => Messages(Seq(message))
        case None          => Messages(Nil)
      }

  private def loggingSink: Sink[Messages, Future[Done]] = Sink.foreach[Messages] { messages =>
    messages.messages.foreach { msg =>
      val formatted = Map(
        'id         -> msg.id,
        'contents   -> msg.contents.value,
        'created_at -> msg.createdAt.format(DateTimeFormatter.ISO_ZONED_DATE_TIME)
      ).map { case (k, v) => s"$k:$v" }.toSeq.mkString("\t")

      info(formatted)
    }
  }

}
