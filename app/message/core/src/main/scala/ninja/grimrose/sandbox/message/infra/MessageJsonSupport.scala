package ninja.grimrose.sandbox.message.infra

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import ninja.grimrose.sandbox.message.{ Contents, Message, MessageId, Messages }

trait MessageJsonSupport extends SprayJsonSupport {
  import spray.json._
  import DefaultJsonProtocol._

  implicit val zonedDateTimeJsonFormat = new RootJsonFormat[ZonedDateTime] {
    private lazy val formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME

    override def write(dateTime: ZonedDateTime): JsValue = JsString(dateTime.format(formatter))
    override def read(json: JsValue): ZonedDateTime = json match {
      case JsString(value) => ZonedDateTime.parse(value, formatter)
      case other           => deserializationError(s"cannot read value $other")
    }
  }

  implicit val messageJsonFormat = new RootJsonFormat[Message] {
    private lazy val formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
    override def write(message: Message): JsValue = {
      JsObject(
        "id"        -> JsNumber(message.id.value),
        "contents"  -> JsString(message.contents.value),
        "createdAt" -> JsString(message.createdAt.format(formatter))
      )
    }
    override def read(json: JsValue): Message = {
      json.asJsObject.getFields("id", "contents", "createdAt") match {
        case Seq(JsNumber(id), JsString(contents), JsString(createdAt)) =>
          Message(
            MessageId(id.toLong),
            Contents(contents),
            ZonedDateTime.parse(createdAt, formatter)
          )
        case other => deserializationError(s"cannot read value $other")
      }
    }
  }

  implicit val contentsJsonFormat = new RootJsonFormat[Contents] {
    override def write(contents: Contents): JsValue = {
      JsObject("contents" -> JsString(contents.value))
    }
    override def read(json: JsValue): Contents = {
      json.asJsObject.getFields("contents") match {
        case Seq(JsString(value)) => Contents(value)
        case other                => deserializationError(s"cannot read value $other")
      }
    }
  }

  implicit val messageIdJsonFormat = new RootJsonFormat[MessageId] {
    override def write(id: MessageId): JsValue = {
      JsObject("id" -> JsNumber(id.value))
    }
    override def read(json: JsValue): MessageId = {
      json.asJsObject.getFields("id") match {
        case Seq(JsNumber(value)) => MessageId(value.longValue())
        case other                => deserializationError(s"cannot read value $other")
      }
    }
  }

  implicit val messagesJsonFormat = jsonFormat(Messages.apply _, "messages")

}
