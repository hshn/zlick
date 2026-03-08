package zlick

import com.typesafe.config.Config
import scala.reflect.ClassTag
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile
import zio.Scope
import zio.ZIO

object SlickEnvironment {
  def config[P <: JdbcProfile: ClassTag](config: Config, path: String): ZIO[Scope, Throwable, DatabaseConfig[P]] =
    ZIO
      .attempt(DatabaseConfig.forConfig[P](path = path, config = config))
      .withFinalizer { databaseConfig =>
        ZIO.attemptBlocking(databaseConfig.db.close()).orDie
      }
}
