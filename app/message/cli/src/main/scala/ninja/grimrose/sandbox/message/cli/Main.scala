package ninja.grimrose.sandbox.message.cli

import akka.actor.{ ActorSystem, CoordinatedShutdown }
import akka.stream.{ ActorMaterializer, Materializer }
import ninja.grimrose.sandbox.message
import ninja.grimrose.sandbox.message.cli.task._
import ninja.grimrose.sandbox.message.infra.database.DBSettings
import ninja.grimrose.sandbox.message.{ Contents, MessageId }
import wvlet.airframe._
import wvlet.airframe.opts._
import wvlet.log.{ LogFormatter, LogLevel, LogSupport, Logger }

case class GlobalOption(
    @option(prefix = "-h,--help", description = "display help messages", isHelp = true)
    help: Boolean = false,
    @option(prefix = "-l,--loglevel", description = "log level")
    logLevel: Option[LogLevel] = None
)

class Main(globalOption: GlobalOption) extends DefaultCommand with LogSupport with MainFeature {
  Logger.setDefaultLogLevel(globalOption.logLevel.getOrElse(LogLevel.INFO))

  Logger.setDefaultFormatter(LogFormatter.TSVLogFormatter)

  Logger.scheduleLogLevelScan

  override def default: Unit = {
    println("Type --help to display the list of commands")
  }

  @command(description = "show messages")
  def messages(
      @argument(description = "id") ids: Seq[Long] = Nil
  ): Unit = {
    val option = TaskOption(ids.map(MessageId))

    run("messages", option)
  }

  @command(description = "post message")
  def post(@option(prefix = "-m,--message") message: String): Unit = {
    val option = TaskOption(contents = Some(Contents(message)))

    run("post", option)
  }

  @command(description = "delete message")
  def delete(@option(prefix = "--id") id: Option[Long]): Unit = {
    val option = TaskOption(id.map(MessageId).toSeq)

    run("delete", option)
  }

}

trait MainFeature extends LogSupport {
  def run(name: String, option: TaskOption): Unit = {
    DBSettings.initialize()

    val actorSystem       = ActorSystem("message-cli")
    val actorMaterializer = ActorMaterializer()(actorSystem)

    val design = newDesign.noLifeCycleLogging
      .add(message.design)
      .bind[ActorSystem].toInstance(actorSystem)
      .bind[ActorMaterializer].toInstance(actorMaterializer)
      .bind[Materializer].toInstance(actorMaterializer)
      .bind[Tasks].toSingleton

    try {
      design.withSession { _.build[Tasks].dispatch(name, option) }
    } finally {
      debug("actor system terminating.")

      actorMaterializer.shutdown()

      CoordinatedShutdown(actorSystem).run(CoordinatedShutdown.jvmExitReason)

      debug("actor system terminated.")

      DBSettings.destroy()
    }
  }
}

object Main extends App {
  Launcher.of[Main].execute(args)
}
