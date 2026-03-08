package zlick

import com.typesafe.config.Config
import scala.reflect.ClassTag
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile
import zio.Scope
import zio.ZIO

object SlickEnvironment {

  /** Creates a scoped [[DatabaseConfig]] that automatically closes the database when the scope ends. */
  def config[P <: JdbcProfile: ClassTag](config: Config, path: String): ZIO[Scope, Throwable, DatabaseConfig[P]] =
    ZIO
      .attempt(DatabaseConfig.forConfig[P](path = path, config = config))
      .withFinalizer { databaseConfig =>
        ZIO.attemptBlocking(databaseConfig.db.close()).orDie
      }
}
