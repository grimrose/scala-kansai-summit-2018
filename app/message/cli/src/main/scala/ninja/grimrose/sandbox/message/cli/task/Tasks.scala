package ninja.grimrose.sandbox.message.cli.task

import ninja.grimrose.sandbox.message.{ Contents, MessageId }
import wvlet.airframe._

trait Task {

  def run(option: TaskOption): Unit

}

case class TaskOption(
    messageIds: Seq[MessageId] = Nil,
    contents: Option[Contents] = None
)

trait Tasks {

  private val dispatcher: String => Task = {
    case "messages" => bind[MessagesTask]
    case "post"     => bind[PostTask]
    case "delete"   => bind[DeleteTask]
    case _          => bind[DefaultTask]
  }

  def dispatch(name: String, option: TaskOption): Unit = dispatcher(name).run(option)
}
