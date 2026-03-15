package zlick

import com.typesafe.config.ConfigFactory
import slick.basic.DatabaseConfig
import slick.jdbc.H2Profile
import zio.Scope
import zio.ZIO
import zio.test.*

object SlickEnvironmentSpec extends ZIOSpecDefault {

  override def spec: Spec[TestEnvironment & Scope, Any] = suiteAll("SlickEnvironment") {
    test("creates DatabaseConfig from typesafe config") {
      for {
        dbConfig <- ZIO.service[DatabaseConfig[H2Profile]]
      } yield assertTrue(
        dbConfig.profile == H2Profile
      )
    }.provideLayer(
      SlickEnvironment.forConfig[H2Profile](
        config = ConfigFactory.parseString(
          """
            |profile = "slick.jdbc.H2Profile$"
            |db {
            |  url = "jdbc:h2:mem:slick_env_test;DB_CLOSE_DELAY=-1"
            |  driver = "org.h2.Driver"
            |  connectionPool = disabled
            |}
            |""".stripMargin
        ),
        path = "",
      )
    )

    test("creates DatabaseConfig from URI") {
      for {
        dbConfig <- ZIO.service[DatabaseConfig[H2Profile]]
      } yield assertTrue(
        dbConfig.profile == H2Profile
      )
    }.provideLayer(
      SlickEnvironment.forURI[H2Profile](new java.net.URI("jdbc:h2:mem:slick_env_test;DB_CLOSE_DELAY=-1"))
    )
  } @@ TestAspect.sequential
}
