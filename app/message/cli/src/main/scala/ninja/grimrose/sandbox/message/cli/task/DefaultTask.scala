package ninja.grimrose.sandbox.message.cli.task

import wvlet.log.LogSupport

trait DefaultTask extends Task with AkkaFeature with LogSupport {
  override def run(option: TaskOption): Unit = {}
}
