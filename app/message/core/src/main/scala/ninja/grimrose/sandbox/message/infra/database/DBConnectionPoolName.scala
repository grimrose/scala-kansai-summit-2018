package ninja.grimrose.sandbox.message.infra.database

case class DBConnectionPoolName(name: Symbol = 'default) extends AnyVal

object DBConnectionPoolName {
  def of(name: Symbol): DBConnectionPoolName = DBConnectionPoolName(name)
}
