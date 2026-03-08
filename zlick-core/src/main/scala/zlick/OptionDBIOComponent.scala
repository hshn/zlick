package zlick

import scala.annotation.targetName
import scala.concurrent.ExecutionContext
import slick.jdbc.JdbcProfile

trait OptionDBIOComponent { self: JdbcProfile =>
  trait OptionDBIOApi { api: JdbcAPI =>
    extension [R, S <: NoStream, E <: Effect](action: DBIOAction[Option[R], S, E]) {
      @targetName("semiFlatMapOption")
      def semiFlatMap[R1, E1 <: Effect](
        f: R => DBIOAction[R1, NoStream, E1]
      )(using ExecutionContext): DBIOAction[Option[R1], NoStream, E & E1] =
        action.flatMap {
          case Some(value) => f(value).map(Some(_))
          case None        => DBIO.successful(None)
        }

      def orElseF[E1 <: Effect](
        other: => DBIOAction[Option[R], NoStream, E1]
      )(using ExecutionContext): DBIOAction[Option[R], NoStream, E & E1] =
        action.flatMap {
          case None           => other
          case some @ Some(_) => DBIO.successful(some)
        }

      def someOrLeft[L](error: => L)(using ExecutionContext): DBIOAction[Either[L, R], NoStream, E] =
        action.map(_.toRight(error))

      def someOrFail[L <: Throwable](error: => L)(using ExecutionContext): DBIOAction[R, NoStream, E] =
        action.flatMap {
          case Some(value) => DBIO.successful(value)
          case None        => DBIO.failed(error)
        }
    }
  }
}
