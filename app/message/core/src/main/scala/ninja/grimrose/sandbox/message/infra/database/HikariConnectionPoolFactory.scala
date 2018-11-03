package ninja.grimrose.sandbox.message.infra.database

import java.sql.Connection
import java.util.Properties

import com.zaxxer.hikari.{ HikariConfig, HikariDataSource }
import scalikejdbc._

object HikariConnectionPoolFactory extends ConnectionPoolFactory {

  override def apply(url: String, user: String, password: String, settings: ConnectionPoolSettings): ConnectionPool = {
    val driverName = settings.driverName

    val dataSource = {
      val props = new Properties()

      val dbUrl = JDBCUrl(url)

      props.setProperty("dataSourceClassName", driverName)
      props.setProperty("dataSource.serverName", dbUrl.host)
      props.setProperty("dataSource.portNumber", dbUrl.port.toString)
      props.setProperty("dataSource.databaseName", dbUrl.database)

      props.setProperty("dataSource.user", user)
      props.setProperty("dataSource.password", password)

      val config = new HikariConfig(props)

      config.setDataSourceClassName(driverName)
      config.setJdbcUrl(url)
      config.setUsername(user)
      config.setPassword(password)

      config.setConnectionTimeout(settings.connectionTimeoutMillis)
      config.setIdleTimeout(settings.warmUpTime)
      if (settings.validationQuery != null) {
        config.setConnectionTestQuery(settings.validationQuery)
      }

      config.setMinimumIdle(settings.initialSize)
      config.setMaximumPoolSize(settings.maxSize)

      new HikariDataSource(config)
    }

    new DataSourceConnectionPool(
      dataSource,
      DataSourceConnectionPoolSettings(driverName = driverName),
      () => dataSource.close()
    ) {
      override def borrow(): Connection = new io.opencensus.ocjdbc.Connection(
        dataSource.getConnection
      )
    }
  }
}
