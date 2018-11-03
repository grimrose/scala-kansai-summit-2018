package ninja.grimrose.sandbox.message.infra.database

import java.time.ZonedDateTime

import ninja.grimrose.sandbox.message.{ Contents, Message, MessageId }
import scalikejdbc._
import skinny.orm._

case class MessageRecord(id: Long, contents: String, createdAt: ZonedDateTime)

object MessageRecord extends SkinnyCRUDMapperWithId[Long, MessageRecord] {

  override def schemaName: Option[String]         = Some("public")
  override def tableName: String                  = "messages"
  override def defaultAlias: Alias[MessageRecord] = createAlias("m")

  override def idToRawValue(id: Long): Any         = id
  override def rawValueToId(value: Any): Long      = value.toString.toLong
  override def useAutoIncrementPrimaryKey: Boolean = false

  override def extract(rs: WrappedResultSet, n: scalikejdbc.ResultName[MessageRecord]): MessageRecord =
    autoConstruct(rs, n)

  implicit class RecordToEntity(record: MessageRecord) {
    def asEntity: Message = Message(MessageId(record.id), Contents(record.contents), record.createdAt)
  }
}
