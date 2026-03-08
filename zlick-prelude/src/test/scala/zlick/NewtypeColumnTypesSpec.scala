package zlick

import zio.Chunk
import zio.Scope
import zio.Task
import zio.ZIO
import zio.prelude.Assertion
import zio.prelude.Newtype
import zio.test.*

object NewtypeColumnTypesSpec extends ZIOSpecDefault {

  object UserId extends Newtype[Int]
  type UserId = UserId.Type

  object Score extends Newtype[Int] {
    override inline def assertion: Assertion[Int] = Assertion.greaterThanOrEqualTo(0)
  }
  type Score = Score.Type

  object TestProfile extends slick.jdbc.H2Profile with NewtypeColumnTypesComponent {
    object TestApi extends JdbcAPI with NewtypeColumnTypesApi
  }
  import TestProfile.TestApi.{*, given}

  given BaseColumnType[UserId] = MappedColumnType.newtypeWrap(UserId)
  given BaseColumnType[Score]  = MappedColumnType.newtypeMake(Score)

  class Scores(tag: Tag) extends Table[(Int, Score)](tag, "scores") {
    def id    = column[Int]("id")
    def score = column[Score]("score")
    def *     = (id, score)
  }
  private val scores = TableQuery[Scores]

  class Users(tag: Tag) extends Table[(UserId, String)](tag, "users") {
    def id   = column[UserId]("id")
    def name = column[String]("name")
    def *    = (id, name)
  }
  private val users = TableQuery[Users]

  class BlobTable(tag: Tag) extends Table[(Int, Chunk[Byte])](tag, "blob_table") {
    def id   = column[Int]("id")
    def data = column[Chunk[Byte]]("data")
    def *    = (id, data)
  }
  private val blobs = TableQuery[BlobTable]

  private val db = Database.forURL("jdbc:h2:mem:newtype_test;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")

  private def runZIO[R](action: DBIO[R]): Task[R] =
    ZIO.fromFuture(implicit ec => db.run(action))

  override def spec: Spec[TestEnvironment & Scope, Any] = suiteAll("NewtypeColumnTypesComponent") {

    suiteAll("newtypeWrap") {
      test("round-trips through the database") {
        val userId = UserId(42)
        for {
          _      <- runZIO(users.schema.createIfNotExists)
          _      <- runZIO(users.delete)
          _      <- runZIO(users += (userId, "Alice"))
          result <- runZIO(users.filter(_.id === userId).result.head)
        } yield assertTrue(result == (userId, "Alice"))
      }
    }

    suiteAll("newtypeMake") {
      test("round-trips valid value through the database") {
        val score = Score(42)
        for {
          _      <- runZIO(scores.schema.createIfNotExists)
          _      <- runZIO(scores.delete)
          _      <- runZIO(scores += (1, score))
          result <- runZIO(scores.filter(_.id === 1).result.head)
        } yield assertTrue(result == (1, score))
      }
      test("throws on invalid value from database") {
        for {
          _      <- runZIO(scores.schema.createIfNotExists)
          _      <- runZIO(scores.delete)
          _      <- runZIO(sqlu"""INSERT INTO "scores" ("id", "score") VALUES (1, -5)""")
          result <- runZIO(scores.filter(_.id === 1).result.head).exit
        } yield assertTrue(result.isFailure)
      }
    }

    suiteAll("Chunk[Byte] column type") {
      test("round-trips Chunk[Byte] through the database") {
        val data = Chunk[Byte](1, 2, 3, 4, 5)
        for {
          _      <- runZIO(blobs.schema.createIfNotExists)
          _      <- runZIO(blobs.delete)
          _      <- runZIO(blobs += (1, data))
          result <- runZIO(blobs.filter(_.id === 1).result.head)
        } yield assertTrue(result == (1, data))
      }
    }
  } @@ TestAspect.sequential
}
