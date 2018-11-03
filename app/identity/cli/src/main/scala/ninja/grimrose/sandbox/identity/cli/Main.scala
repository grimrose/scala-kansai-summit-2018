package ninja.grimrose.sandbox.identity.cli

import akka.actor.ActorSystem
import ninja.grimrose.sandbox.identity.IdGenerator
import ninja.grimrose.sandbox.identity.cli.task.Task
import ninja.grimrose.sandbox.katsubushi
import wvlet.airframe.opts._
import wvlet.log.{ LogFormatter, LogLevel, LogSupport, Logger }
import wvlet.airframe._

case class GlobalOption(
    @option(prefix = "-h,--help", description = "display help messages", isHelp = true)
    help: Boolean = false,
    @option(prefix = "-l,--loglevel", description = "log level")
    logLevel: Option[LogLevel] = None
)

class Main(globalOption: GlobalOption) extends DefaultCommand with LogSupport {
  Logger.setDefaultLogLevel(globalOption.logLevel.getOrElse(LogLevel.INFO))

  Logger.setDefaultFormatter(LogFormatter.TSVLogFormatter)

  Logger.scheduleLogLevelScan

  def default: Unit = {
    newDesign.withProductionMode.noLifeCycleLogging
      .add(katsubushi.design)
      .bind[ActorSystem].toInstance(ActorSystem("identity-cli"))
      .bind[IdGenerator].toSingleton
      .bind[Task].toSingleton
      .withSession { session =>
        session.build[Task].run()
      }
  }

}

object Main extends App {
  Launcher.of[Main].execute(args)
}
