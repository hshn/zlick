package zlick

import scala.annotation.implicitNotFound
import scala.concurrent.ExecutionContext
import slick.jdbc.JdbcProfile
import zio.Runtime
import zio.Task
import zio.Unsafe
import zio.ZIO

trait ZioApiComponent { self: JdbcProfile =>
  trait ZioApi { api: JdbcAPI =>
    extension (db: Database) {
      def runZIO: RunZIOPartiallyApplied = new RunZIOPartiallyApplied(db)
    }

    final class RunZIOPartiallyApplied(private val db: Database) {
      def attempt[R, S <: NoStream, E <: Effect](f: ExecutionContext ?=> DBIOAction[R, S, E]): Task[R] =
        ZIO.fromFuture { implicit ec => db.run(f) }

      def succeed[R, S <: NoStream, E <: Effect](f: ExecutionContext ?=> DBIOAction[R, S, E]): ZIO[Any, Nothing, R] =
        attempt(f).orDie

      def fromEither[A, B, S <: NoStream, E <: Effect](
        f: ExecutionContext ?=> DBIOAction[Either[A, B], S, E]
      )(using
        @implicitNotFound("DBIOAction[Either[${A}, ${B}], ${S}, ${E}]] needs to have a Transactional effect")
        ev1: E <:< Effect.Transactional
      ): ZIO[Any, A, B] =
        succeed(f).absolve
    }

    extension [E <: Throwable, A](zio: ZIO[Any, E, A]) {
      def unsafeToDBIO: DBIOAction[A, NoStream, Effect] = {
        val runtime = Runtime.default
        val future  = Unsafe.unsafe { implicit unsafe =>
          runtime.unsafe.runToFuture(zio)
        }
        DBIO.from(future)
      }
    }
  }
}
