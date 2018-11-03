package ninja.grimrose.sandbox.message.infra.database

import org.scalatest.{ BeforeAndAfterAll, Suite }

trait TestDBSettings extends BeforeAndAfterAll { self: Suite =>

  override protected def beforeAll(): Unit = {
    super.beforeAll()

    DBSettings.initialize(true)
  }

}
