package zlick

import scala.concurrent.ExecutionContext
import slick.jdbc.JdbcProfile

trait EitherDBIOComponent { self: JdbcProfile =>
  trait EitherDBIOApi { api: JdbcAPI =>

    opaque type RollbackOnLeft[L, R, E <: Effect] = DBIOAction[Either[L, R], NoStream, E]

    extension [L, R, E <: Effect](rollback: RollbackOnLeft[L, R, E]) {
      def transactionally(using
        ExecutionContext
      ): DBIOAction[Either[L, R], NoStream, E & Effect.Transactional] = {
        val lifted = rollback.flatMap {
          case Right(r) => DBIO.successful(r)
          case Left(l)  => DBIO.failed(EitherDBIOComponent.EitherLeftWrappingException(l))
        }
        lifted.transactionally.asTry.flatMap {
          case scala.util.Success(r)                                                                => DBIO.successful(Right(r))
          case scala.util.Failure(EitherDBIOComponent.EitherLeftWrappingException(l: L @unchecked)) => DBIO.successful(Left(l))
          case scala.util.Failure(ex)                                                               => DBIO.failed(ex)
        }
      }
    }

    extension [L, R, E <: Effect](action: DBIOAction[Either[L, R], NoStream, E]) {

      def semiflatMap[R1, E1 <: Effect](
        f: R => DBIOAction[R1, NoStream, E1]
      )(using ExecutionContext): DBIOAction[Either[L, R1], NoStream, E & E1] =
        action.flatMap {
          case Right(r) => f(r).map(Right(_))
          case Left(l)  => DBIO.successful(Left(l))
        }

      def subflatMap[L1 >: L, R1](
        f: R => Either[L1, R1]
      )(using ExecutionContext): DBIOAction[Either[L1, R1], NoStream, E] =
        action.map {
          case Right(r) => f(r)
          case Left(l)  => Left(l)
        }

      def flatMapF[L1 >: L, R1, E1 <: Effect](
        f: R => DBIOAction[Either[L1, R1], NoStream, E1]
      )(using ExecutionContext): DBIOAction[Either[L1, R1], NoStream, E & E1] =
        action.flatMap {
          case Right(r) => f(r)
          case Left(l)  => DBIO.successful(Left(l))
        }

      def right(using ec: ExecutionContext, ev: L =:= Nothing): DBIOAction[R, NoStream, E] =
        action.map(_.fold(ev(_), identity))

      def rollbackOnLeft: RollbackOnLeft[L, R, E] = action
    }

  }
}

private[zlick] object EitherDBIOComponent {
  private[zlick] case class EitherLeftWrappingException[+L](left: L) extends RuntimeException("EitherDBIO left value", null, true, false)
}
