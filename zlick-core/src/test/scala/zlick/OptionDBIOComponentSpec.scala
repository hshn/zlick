package zlick

import zio.Scope
import zio.test.*

object OptionDBIOComponentSpec extends ZIOSpecDefault {

  object TestProfile extends slick.jdbc.H2Profile with OptionDBIOComponent with ZioApiComponent {
    object TestApi extends JdbcAPI with OptionDBIOApi with ZioApi
  }
  import TestProfile.TestApi.*

  private val db = Database.forURL("jdbc:h2:mem:option_dbio_test;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")

  override def spec: Spec[TestEnvironment & Scope, Any] = suiteAll("OptionDBIOComponent") {

    suiteAll("semiFlatMap") {
      test("applies f on Some") {
        for {
          result <- db.runZIO.succeed {
            DBIO.successful(Some(1): Option[Int]).semiFlatMap(r => DBIO.successful(r + 10))
          }
        } yield assertTrue(result == Some(11))
      }
      test("short-circuits on None") {
        var called = false
        for {
          result <- db.runZIO.succeed {
            DBIO.successful(None: Option[Int]).semiFlatMap { r =>
              called = true
              DBIO.successful(r + 10)
            }
          }
        } yield assertTrue(result == None, !called)
      }
    }

    suiteAll("orElseF") {
      test("returns original on Some") {
        for {
          result <- db.runZIO.succeed {
            DBIO.successful(Some(1): Option[Int]).orElseF(DBIO.successful(Some(99)))
          }
        } yield assertTrue(result == Some(1))
      }
      test("returns fallback on None") {
        for {
          result <- db.runZIO.succeed {
            DBIO.successful(None: Option[Int]).orElseF(DBIO.successful(Some(99)))
          }
        } yield assertTrue(result == Some(99))
      }
    }

    suiteAll("someOrLeft") {
      test("returns Right on Some") {
        for {
          result <- db.runZIO.succeed {
            DBIO.successful(Some(42): Option[Int]).someOrLeft("missing")
          }
        } yield assertTrue(result == Right(42))
      }
      test("returns Left on None") {
        for {
          result <- db.runZIO.succeed {
            DBIO.successful(None: Option[Int]).someOrLeft("missing")
          }
        } yield assertTrue(result == Left("missing"))
      }
    }

    suiteAll("someOrFail") {
      test("returns value on Some") {
        for {
          result <- db.runZIO.succeed {
            DBIO.successful(Some(42): Option[Int]).someOrFail(new NoSuchElementException("missing"))
          }
        } yield assertTrue(result == 42)
      }
      test("fails on None") {
        for {
          result <- db.runZIO.attempt {
            DBIO.successful(None: Option[Int]).someOrFail(new NoSuchElementException("missing"))
          }.exit
        } yield assertTrue(result.isFailure)
      }
    }
  } @@ TestAspect.sequential
}
