package ninja.grimrose.sandbox.message.infra.database

import cats.data.ReaderT
import ninja.grimrose.sandbox.PersistentContext
import scalikejdbc.{ DBSession, NamedDB }

import scala.concurrent.{ ExecutionContext, Future }

case class JdbcContext(session: DBSession, executionContext: ExecutionContext) extends PersistentContext

object JdbcContext {

  implicit class ContextToJdbcContext(ctx: PersistentContext) {

    def ofJdbc: JdbcContext = ctx.asInstanceOf[JdbcContext]
  }

}

trait JdbcContextFeature {

  def runFutureLocalTx[A](
      ec: ExecutionContext,
      poolName: DBConnectionPoolName = DBConnectionPoolName()
  )(reader: ReaderT[Future, PersistentContext, A]): Future[A] = {
    NamedDB(poolName.name).futureLocalTx { implicit session =>
      reader.run(JdbcContext(session, ec))
    }(ec)
  }

  def runReadOnly[A](
      ec: ExecutionContext,
      poolName: DBConnectionPoolName = DBConnectionPoolName()
  )(reader: ReaderT[Future, PersistentContext, A]): Future[A] = {
    NamedDB(poolName.name)
      .autoClose(false)
      .readOnly { implicit session =>
        reader.run(JdbcContext(session, ec))
      }
  }

}
