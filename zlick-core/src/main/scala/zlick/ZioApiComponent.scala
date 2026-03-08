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

      /** Runs a [[DBIOAction]] as a [[Task]], exposing database exceptions as ZIO failures. */
      def attempt[R, S <: NoStream, E <: Effect](f: ExecutionContext ?=> DBIOAction[R, S, E]): Task[R] =
        ZIO.fromFuture { implicit ec => db.run(f) }

      /** Runs a [[DBIOAction]] that is expected to succeed. Database exceptions become ZIO defects. */
      def succeed[R, S <: NoStream, E <: Effect](f: ExecutionContext ?=> DBIOAction[R, S, E]): ZIO[Any, Nothing, R] =
        attempt(f).orDie

      /** Runs a transactional [[DBIOAction]] returning `Either[A, B]`, mapping `Left` to ZIO failure and `Right` to success. */
      def fromEither[A, B, S <: NoStream, E <: Effect](
        f: ExecutionContext ?=> DBIOAction[Either[A, B], S, E]
      )(using
        @implicitNotFound("DBIOAction[Either[${A}, ${B}], ${S}, ${E}] needs to have a Transactional effect")
        ev1: E <:< Effect.Transactional
      ): ZIO[Any, A, B] =
        succeed(f).absolve
    }

    extension [E <: Throwable, A](zio: ZIO[Any, E, A]) {

      /** Embeds a ZIO effect into a [[DBIOAction]] by running it on the default runtime. ZIO failures become DBIO failures. */
      def unsafeToDBIO: DBIOAction[A, NoStream, Effect] =
        DBIO.from(Unsafe.unsafe { implicit unsafe =>
          Runtime.default.unsafe.runToFuture(zio)
        })
    }
  }
}
