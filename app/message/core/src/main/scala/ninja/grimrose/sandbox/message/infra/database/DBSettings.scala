package ninja.grimrose.sandbox.message.infra.database

import scalikejdbc.ConnectionPoolFactoryRepository

object DBSettings extends DBSettings

trait DBSettings {

  def initialize(force: Boolean = false): Unit = {
    ConnectionPoolFactoryRepository.add("hikari", HikariConnectionPoolFactory)

    skinny.DBSettings.initialize(force)
  }

  def destroy(): Unit = {
    skinny.DBSettings.destroy()
  }

}
