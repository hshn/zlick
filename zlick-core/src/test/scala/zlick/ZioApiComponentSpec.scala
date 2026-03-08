package zlick

import zio.Exit
import zio.Scope
import zio.ZIO
import zio.test.*

object ZioApiComponentSpec extends ZIOSpecDefault {

  object TestProfile extends slick.jdbc.H2Profile with ZioApiComponent {
    object TestApi extends JdbcAPI with ZioApi
  }
  import TestProfile.TestApi.*

  private val db = Database.forURL("jdbc:h2:mem:zio_api_test;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")

  override def spec: Spec[TestEnvironment & Scope, Any] = suiteAll("ZioApiComponent") {

    suiteAll("runZIO.attempt") {
      test("succeeds with value") {
        for {
          result <- db.runZIO.attempt(DBIO.successful(42))
        } yield assertTrue(result == 42)
      }
      test("fails with exception") {
        for {
          result <- db.runZIO.attempt(DBIO.failed(new RuntimeException("boom"))).exit
        } yield assertTrue(result.isFailure)
      }
    }

    suiteAll("runZIO.succeed") {
      test("returns value") {
        for {
          result <- db.runZIO.succeed(DBIO.successful(42))
        } yield assertTrue(result == 42)
      }
    }

    suiteAll("runZIO.fromEither") {
      test("succeeds on Right") {
        for {
          result <- db.runZIO.fromEither {
            (DBIO.successful(Right(42)): DBIO[Either[String, Int]]).transactionally
          }
        } yield assertTrue(result == 42)
      }
      test("fails on Left") {
        for {
          result <- db.runZIO.fromEither {
            (DBIO.successful(Left("err")): DBIO[Either[String, Int]]).transactionally
          }.exit
        } yield assertTrue(result == Exit.fail("err"))
      }
    }

    suiteAll("unsafeToDBIO") {
      test("converts ZIO to DBIO") {
        for {
          result <- db.runZIO.attempt(ZIO.succeed(42).unsafeToDBIO)
        } yield assertTrue(result == 42)
      }
    }
  } @@ TestAspect.sequential
}
