package ninja.grimrose.sandbox.message

import cats.data.ReaderT
import ninja.grimrose.sandbox.PersistentContext

import scala.concurrent.Future

trait MessageRepository {

  type ReaderTF[C, A] = ReaderT[Future, C, A]

  def store(entity: Message): ReaderTF[PersistentContext, Unit]

  def remove(messageId: MessageId): ReaderTF[PersistentContext, Unit]

  def find(messageId: MessageId): ReaderTF[PersistentContext, Option[Message]]

  def findAll(): ReaderTF[PersistentContext, Messages]

}
