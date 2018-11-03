package ninja.grimrose.sandbox.message.http.controller

import akka.event.Logging
import akka.http.scaladsl.common.{ EntityStreamingSupport, JsonEntityStreamingSupport }
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{ Directives, Route }
import akka.stream.scaladsl.{ Sink, Source }
import io.opencensus.scala.akka.http.TracingDirective.traceRequest
import io.opencensus.trace.AttributeValue
import ninja.grimrose.sandbox.message.infra.MessageJsonSupport
import ninja.grimrose.sandbox.message.{ Contents, MessageId }
import ninja.grimrose.sandbox.message.usecase.{
  CreateMessageUseCase,
  DeleteMessageUseCase,
  FindByIdMessageUseCase,
  FindMessagesUseCase
}
import wvlet.airframe._

trait MessageController extends Directives with MessageJsonSupport {

  private val findMessagesUseCase: FindMessagesUseCase = bind[FindMessagesUseCase]

  private val createMessageUseCase: CreateMessageUseCase = bind[CreateMessageUseCase]

  private val findByIdMessageUseCase: FindByIdMessageUseCase = bind[FindByIdMessageUseCase]

  private val deleteMessageUseCase: DeleteMessageUseCase = bind[DeleteMessageUseCase]

  implicit val jsonStreamingSupport: JsonEntityStreamingSupport = EntityStreamingSupport.json()

  def messageRoutes: Route = logRequestResult("messages", Logging.InfoLevel) {
    pathPrefix("messages") {
      optionalHeaderValueByName("X-B3-TraceId") { traceId =>
        traceRequest { span =>
          span.putAttribute("X-B3-TraceId", AttributeValue.stringAttributeValue(traceId.getOrElse("")))

          concat(
            pathEnd {
              concat(
                get {
                  extractMaterializer { implicit mat =>
                    import FindMessagesUseCase._

                    val future = Source
                      .single(FindMessages())
                      .via(findMessagesUseCase.toFlow)
                      .runWith(Sink.head)

                    onSuccess(future) { messages =>
                      complete(messages)
                    }
                  }
                },
                post {
                  entity(as[Contents]) { contents =>
                    extractMaterializer { implicit mat =>
                      import CreateMessageUseCase._

                      val future = Source
                        .single(CreateMessage(contents))
                        .via(createMessageUseCase.toFlow)
                        .runWith(Sink.head)

                      onSuccess(future) { result =>
                        complete(StatusCodes.OK -> result.id)
                      }
                    }
                  }
                }
              )
            },
            path(Segment) { id =>
              concat(
                get {
                  extractMaterializer { implicit mat =>
                    import FindByIdMessageUseCase._

                    rejectEmptyResponse {
                      val future = Source
                        .single(FindByIdMessage(MessageId(id.toLong)))
                        .via(findByIdMessageUseCase.toFlow)
                        .runWith(Sink.head)

                      onSuccess(future) { message =>
                        complete(message)
                      }
                    }
                  }
                },
                delete {
                  extractMaterializer { implicit mat =>
                    import DeleteMessageUseCase._

                    val future = Source
                      .single(DeleteMessage(MessageId(id.toLong)))
                      .via(deleteMessageUseCase.toFlow)
                      .runWith(Sink.head)

                    onSuccess(future) {
                      case NotFound(_)       => complete(StatusCodes.NotFound)
                      case MessageDeleted(_) => complete(StatusCodes.OK)
                    }
                  }
                }
              )
            }
          )
        }
      }
    }
  }
}
