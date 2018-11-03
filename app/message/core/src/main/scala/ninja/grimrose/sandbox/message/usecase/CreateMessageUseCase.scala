package ninja.grimrose.sandbox.message.usecase

import java.time.ZonedDateTime

import akka.NotUsed
import akka.event.Logging
import akka.stream.scaladsl.{ Broadcast, Flow, GraphDSL, ZipWith }
import akka.stream.{ ActorAttributes, Attributes, FlowShape }
import ninja.grimrose.sandbox.message._
import ninja.grimrose.sandbox.message.gateway.IdentityApiAdapter
import ninja.grimrose.sandbox.message.infra.database.{ DBConnectionPoolName, JdbcContextFeature }
import ninja.grimrose.sandbox.message.usecase.CreateMessageUseCase.{ CreateMessage, MessageCreated }

import scala.concurrent.ExecutionContext

trait CreateMessageUseCase {

  def toFlow: Flow[CreateMessage, MessageCreated, NotUsed]

}

object CreateMessageUseCase {
  case class CreateMessage(contents: Contents)
  case class MessageCreated(id: MessageId)
}

class CreateMessageUseCaseOfJdbc(
    repository: MessageRepository,
    poolName: DBConnectionPoolName,
    ec: ExecutionContext,
    identityApi: IdentityApiAdapter
) extends CreateMessageUseCase
    with JdbcContextFeature {

  import CreateMessageUseCase._

  def toFlow: Flow[CreateMessage, MessageCreated, NotUsed] = {
    Flow.fromGraph(GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val contents = builder.add(Flow[CreateMessage].map(_.contents))

      val broadcast = builder.add(Broadcast[Contents](2))

      val identity = builder.add(identityFlow)

      val idBroadcast = builder.add(Broadcast[MessageId](2))

      val zipper = builder.add(zipToEntity)

      val store = builder.add(storeFlow)

      val event = builder.add(zipToEvent)

      // format: off

      contents ~> broadcast                            ~> zipper.in0
                  broadcast ~> identity ~> idBroadcast ~> zipper.in1
                                                          zipper.out ~> store ~> event.in0
                                           idBroadcast                        ~> event.in1

      // format: on

      FlowShape(contents.in, event.out)
    })
  }

  private val identityFlow =
    Flow[Contents]
      .mapAsyncUnordered(1)(_ => identityApi.generate())
      .map(response => MessageId(response.id.toLong))

  private val zipToEntity = ZipWith[Contents, MessageId, Message] { (contents, id) =>
    Message(id, contents, ZonedDateTime.now())
  }

  private val storeFlow = Flow[Message]
    .mapAsyncUnordered(1) { entity =>
      runFutureLocalTx(ec, poolName)(repository.store(entity))
    }
    .withAttributes(ActorAttributes.dispatcher("scalikejdbc-dispatcher"))
    .log("createMessage")
    .withAttributes(Attributes.logLevels(onElement = Logging.InfoLevel))

  private val zipToEvent = ZipWith[Unit, MessageId, MessageCreated] { (_, id) =>
    MessageCreated(id)
  }

}
