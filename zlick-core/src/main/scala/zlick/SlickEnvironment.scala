package zlick

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import java.net.URI
import scala.reflect.ClassTag
import slick.basic.BasicProfile
import slick.basic.DatabaseConfig
import zio.Scope
import zio.Tag
import zio.ZIO
import zio.ZLayer

object SlickEnvironment {

  /** Creates a scoped [[DatabaseConfig]] that automatically closes the database when the scope ends. */
  def forConfig[P <: BasicProfile: ClassTag](config: Config, path: String)(using Tag[P]): ZLayer[Scope, Throwable, DatabaseConfig[P]] =
    configure(DatabaseConfig.forConfig[P](path = path, config = config))

  /** Creates a scoped [[DatabaseConfig]] that automatically closes the database when the scope ends. */
  def forConfig[P <: BasicProfile: ClassTag](path: String)(using Tag[P]): ZLayer[Scope, Throwable, DatabaseConfig[P]] =
    configure(DatabaseConfig.forConfig[P](path = path, config = ConfigFactory.load()))

  /** Creates a scoped [[DatabaseConfig]] from a URI that automatically closes the database when the scope ends. */
  def forURI[P <: BasicProfile: ClassTag](uri: URI)(using Tag[P]): ZLayer[Scope, Throwable, DatabaseConfig[P]] =
    configure(DatabaseConfig.forURI[P](uri))

  private def configure[P <: BasicProfile](f: => DatabaseConfig[P])(using Tag[P]) = ZLayer.scoped {
    ZIO
      .attempt(f)
      .withFinalizer { databaseConfig =>
        ZIO.attemptBlocking(databaseConfig.db.close()).orDie
      }
  }
}
