package ninja.grimrose.sandbox.message

import java.time.ZonedDateTime

case class Contents(value: String) extends AnyVal

case class Message(id: MessageId, contents: Contents, createdAt: ZonedDateTime)

case class Messages(messages: Seq[Message]) extends AnyVal
