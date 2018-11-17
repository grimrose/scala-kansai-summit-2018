package ninja.grimrose.sandbox.message.http.controller

import java.time.{ LocalDateTime, ZoneId, ZonedDateTime }

import akka.actor.ActorSystem
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{ ContentTypes, HttpRequest, MessageEntity, StatusCodes }
import akka.http.scaladsl.testkit.{ RouteTestTimeout, ScalatestRouteTest }
import akka.stream.Materializer
import cats.data._
import ninja.grimrose.sandbox._
import ninja.grimrose.sandbox.message._
import ninja.grimrose.sandbox.message.gateway.{ IdentityApiAdapter, IdentityApiResponse }
import ninja.grimrose.sandbox.message.infra.MessageJsonSupport
import ninja.grimrose.sandbox.message.infra.database.{ DBConnectionPoolName, TestDBSettings }
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{ Matchers, WordSpec }

import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContext, Future }

class MessageControllerSpec
    extends WordSpec
    with Matchers
    with ScalaFutures
    with ScalatestRouteTest
    with TestDBSettings
    with MessageJsonSupport {

  implicit val timeout: RouteTestTimeout = RouteTestTimeout(10.seconds)

  private val tracingHeaders = List(
    RawHeader("X-B3-TraceId", "12345678901234567890123456789012"),
    RawHeader("X-B3-SpanId", "1234567890123456")
  )

  private val baseDesign = design
    .bind[DBConnectionPoolName].toInstance(DBConnectionPoolName.of('dummy))
    .bind[ActorSystem].toInstance(system)
    .bind[ExecutionContext].toInstance(executor)
    .bind[Materializer].toInstance(materializer)
    .bind[MessageController].toSingleton

  "MessageController" should {
    "return no messages if no present (GET /messages)" in {
      baseDesign
        .bind[MessageRepository]
        .toInstance(new MessageRepository {
          override def findAll(): ReaderTF[PersistentContext, Messages] =
            ReaderT(_ => Future.successful(Messages(Seq.empty[Message])))
          override def remove(messageId: MessageId): ReaderTF[PersistentContext, Unit]          = ReaderT(_ => fail())
          override def store(entity: Message): ReaderTF[PersistentContext, Unit]                = ReaderT(_ => fail())
          override def find(messageId: MessageId): ReaderTF[PersistentContext, Option[Message]] = ReaderT(_ => fail())
        })
        .build[MessageController] { controller =>
          val routes = controller.messageRoutes

          val request = HttpRequest(uri = "/messages", headers = tracingHeaders)

          request ~> routes ~> check {
            status should ===(StatusCodes.OK)

            contentType should ===(ContentTypes.`application/json`)

            entityAs[String] should ===("""{"messages":[]}""")
          }
        }
    }

    "return message by id (GET /messages)" in {
      baseDesign
        .bind[MessageRepository]
        .toInstance(any = new MessageRepository {
          override def findAll(): ReaderTF[PersistentContext, Messages]                = ReaderT(_ => fail())
          override def remove(messageId: MessageId): ReaderTF[PersistentContext, Unit] = ReaderT(_ => fail())
          override def store(entity: Message): ReaderTF[PersistentContext, Unit]       = ReaderT(_ => fail())
          override def find(messageId: MessageId): ReaderTF[PersistentContext, Option[Message]] = ReaderT { _ =>
            val dateTime = LocalDateTime.of(2018, 11, 22, 3, 4, 5)
            val zdt      = dateTime.atZone(ZoneId.of("Asia/Tokyo"))

            Future.successful(Option(Message(messageId, Contents("sample"), zdt)))
          }
        })
        .build[MessageController] { controller =>
          val routes = controller.messageRoutes

          val request = HttpRequest(uri = "/messages/17", headers = tracingHeaders)

          request ~> routes ~> check {
            status should ===(StatusCodes.OK)

            contentType should ===(ContentTypes.`application/json`)

            entityAs[String] should ===(
              """{"id":17,"contents":"sample","createdAt":"2018-11-22T03:04:05+09:00"}"""
            )
          }
        }
    }

    "be able to add messages (POST /messages)" in {
      baseDesign
        .bind[MessageRepository]
        .toInstance(new MessageRepository {
          override def findAll(): ReaderTF[PersistentContext, Messages] =
            ReaderT { _ =>
              Future.successful(Messages(Seq.empty[Message]))
            }
          override def remove(messageId: MessageId): ReaderTF[PersistentContext, Unit] = ReaderT(_ => fail())
          override def store(entity: Message): ReaderTF[PersistentContext, Unit] =
            ReaderT { _ =>
              Future.successful(())
            }
          override def find(messageId: MessageId): ReaderTF[PersistentContext, Option[Message]] = ReaderT(_ => fail())
        })
        .bind[IdentityApiAdapter].toInstance(() => ReaderT(_ => Future.successful(IdentityApiResponse("42"))))
        .build[MessageController] { controller =>
          val routes = controller.messageRoutes

          val message       = Message(MessageId(42), Contents("jp"), ZonedDateTime.now())
          val messageEntity = Marshal(message).to[MessageEntity].futureValue // futureValue is from ScalaFutures

          val request = Post("/messages").withEntity(messageEntity).withHeaders(tracingHeaders)

          request ~> routes ~> check {
            status should ===(StatusCodes.OK)

            contentType should ===(ContentTypes.`application/json`)

            entityAs[String] should ===("""{"id":42}""")
          }
        }
    }

    "be able to delete messages (DELETE /messages)" in {
      baseDesign
        .bind[MessageRepository]
        .toInstance(new MessageRepository {
          override def findAll(): ReaderTF[PersistentContext, Messages] =
            ReaderT { _ =>
              Future.successful(Messages(Seq.empty[Message]))
            }
          override def remove(messageId: MessageId): ReaderTF[PersistentContext, Unit] =
            ReaderT { _ =>
              Future.successful(())
            }
          override def store(entity: Message): ReaderTF[PersistentContext, Unit] = ReaderT { _ =>
            Future.successful(())
          }
          override def find(messageId: MessageId): ReaderTF[PersistentContext, Option[Message]] =
            ReaderT { _ =>
              Future.successful(Option(Message(messageId, Contents("sample"), ZonedDateTime.now())))
            }
        })
        .build[MessageController] { controller =>
          val routes = controller.messageRoutes

          val request = Delete(uri = "/messages/13").withHeaders(tracingHeaders)

          request ~> routes ~> check {
            status should ===(StatusCodes.OK)

            contentType should ===(ContentTypes.`text/plain(UTF-8)`)

            entityAs[String] should ===("OK")
          }
        }
    }
  }
}
