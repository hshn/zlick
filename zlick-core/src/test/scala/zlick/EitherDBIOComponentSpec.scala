package zlick

import zio.Scope
import zio.test.*

object EitherDBIOComponentSpec extends ZIOSpecDefault {

  object TestProfile extends slick.jdbc.H2Profile with EitherDBIOComponent with ZioApiComponent {
    object TestApi extends JdbcAPI with EitherDBIOApi with ZioApi
  }
  import TestProfile.TestApi.*

  private val db = Database.forURL("jdbc:h2:mem:either_dbio_test;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")

  class Counter(tag: Tag) extends Table[Int](tag, "counter") {
    def value = column[Int]("value")
    def *     = value
  }
  private val counter = TableQuery[Counter]

  override def spec: Spec[TestEnvironment & Scope, Any] = suiteAll("EitherDBIOComponent") {

    suiteAll("semiflatMap") {
      test("applies f on Right") {
        for {
          result <- db.runZIO.succeed {
            DBIO.successful(Right(1): Either[String, Int]).semiflatMap(r => DBIO.successful(r + 10))
          }
        } yield assertTrue(result == Right(11))
      }
      test("short-circuits on Left") {
        var called = false
        for {
          result <- db.runZIO.succeed {
            DBIO.successful(Left("err"): Either[String, Int]).semiflatMap { r =>
              called = true
              DBIO.successful(r + 10)
            }
          }
        } yield assertTrue(result == Left("err"), !called)
      }
    }

    suiteAll("subflatMap") {
      test("applies f on Right returning Right") {
        for {
          result <- db.runZIO.succeed {
            DBIO.successful(Right(5): Either[String, Int]).subflatMap(r => Right(r * 2))
          }
        } yield assertTrue(result == Right(10))
      }
      test("applies f on Right returning Left") {
        for {
          result <- db.runZIO.succeed {
            DBIO.successful(Right(5): Either[String, Int]).subflatMap(_ => Left("from f"))
          }
        } yield assertTrue(result == Left("from f"))
      }
      test("short-circuits on Left") {
        for {
          result <- db.runZIO.succeed {
            DBIO.successful(Left("err"): Either[String, Int]).subflatMap(r => Right(r * 2))
          }
        } yield assertTrue(result == Left("err"))
      }
    }

    suiteAll("flatMapF") {
      test("applies f on Right returning Right") {
        for {
          result <- db.runZIO.succeed {
            DBIO.successful(Right(3): Either[String, Int]).flatMapF(r => DBIO.successful(Right(r + 7)))
          }
        } yield assertTrue(result == Right(10))
      }
      test("applies f on Right returning Left") {
        for {
          result <- db.runZIO.succeed {
            DBIO.successful(Right(3): Either[String, Int]).flatMapF(_ => DBIO.successful(Left("from f")))
          }
        } yield assertTrue(result == Left("from f"))
      }
      test("short-circuits on Left") {
        var called = false
        for {
          result <- db.runZIO.succeed {
            DBIO.successful(Left("err"): Either[String, Int]).flatMapF { r =>
              called = true
              DBIO.successful(Right(r))
            }
          }
        } yield assertTrue(result == Left("err"), !called)
      }
    }

    suiteAll("right") {
      test("extracts Right when Left is Nothing") {
        for {
          result <- db.runZIO.succeed {
            DBIO.successful(Right(42): Either[Nothing, Int]).right
          }
        } yield assertTrue(result == 42)
      }
    }

    suiteAll("rollbackOnLeft.transactionally") {
      test("commits on Right") {
        for {
          _      <- db.runZIO.succeed(counter.schema.createIfNotExists)
          _      <- db.runZIO.succeed(counter.delete)
          result <- db.runZIO.succeed {
            (counter += 99).flatMap(_ => DBIO.successful(Right(1)): DBIO[Either[String, Int]]).rollbackOnLeft.transactionally
          }
          rows <- db.runZIO.succeed(counter.result)
        } yield assertTrue(result == Right(1), rows == Seq(99))
      }
      test("rolls back on Left") {
        for {
          _      <- db.runZIO.succeed(counter.schema.createIfNotExists)
          _      <- db.runZIO.succeed(counter.delete)
          result <- db.runZIO.succeed {
            (counter += 99).flatMap(_ => DBIO.successful(Left("domain error")): DBIO[Either[String, Int]]).rollbackOnLeft.transactionally
          }
          rows <- db.runZIO.succeed(counter.result)
        } yield assertTrue(result == Left("domain error"), rows.isEmpty)
      }
      test("propagates non-Left exceptions and rolls back") {
        for {
          _      <- db.runZIO.succeed(counter.schema.createIfNotExists)
          _      <- db.runZIO.succeed(counter.delete)
          result <- db.runZIO.attempt {
            (counter += 99)
              .flatMap(_ => DBIO.failed(new RuntimeException("boom")): DBIO[Either[String, Int]])
              .rollbackOnLeft
              .transactionally
          }.exit
          rows <- db.runZIO.succeed(counter.result)
        } yield assertTrue(result.isFailure, rows.isEmpty)
      }
    }
  } @@ TestAspect.sequential
}
