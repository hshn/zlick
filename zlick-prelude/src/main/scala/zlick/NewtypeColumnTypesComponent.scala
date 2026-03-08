package zlick

import scala.reflect.ClassTag
import slick.jdbc.GetResult
import slick.jdbc.JdbcProfile
import zio.Chunk
import zio.prelude.Newtype

trait NewtypeColumnTypesComponent { self: JdbcProfile =>
  trait NewtypeColumnTypesApi { api: JdbcAPI =>

    extension (factory: JdbcProfile#MappedColumnTypeFactory) {

      /** Maps a column to a [[Newtype]] using `wrap`/`unwrap`. No validation on read. */
      def newtypeWrap[U: BaseColumnType](newtype: Newtype[U])(using ClassTag[newtype.Type]): BaseColumnType[newtype.Type] =
        MappedColumnType.base[newtype.Type, U](newtype.unwrap, newtype.wrap)

      /** Maps a column to a [[Newtype]] using `make` on read, which validates the newtype's assertions. */
      def newtypeMake[U: BaseColumnType](newtype: Newtype[U])(using ClassTag[newtype.Type]): BaseColumnType[newtype.Type] =
        MappedColumnType.base[newtype.Type, U](
          newtype.unwrap,
          v =>
            newtype
              .make(v)
              .mapError(e => new IllegalStateException(s"$v is not a valid $newtype: $e"))
              .toTry
              .get,
        )
    }

    given BaseColumnType[Chunk[Byte]] = MappedColumnType.base[Chunk[Byte], Array[Byte]](
      _.toArray,
      Chunk.fromArray,
    )

    given GetResult[Chunk[Byte]] = GetResult(using
      { pr =>
        val blob = pr.nextBlob()
        Chunk.fromArray(blob.getBytes(1, blob.length().toInt))
      }
    )
  }
}
