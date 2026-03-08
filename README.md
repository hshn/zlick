# zlick

Slick ZIO bindings for Scala 3.

## Getting Started

### zlick-core

Core module providing ZIO integration for Slick's `DBIO` actions.

```scala
libraryDependencies += "dev.hshn" %% "zlick-core" % "<version>"
```

### zlick-prelude

Optional module adding ZIO Prelude `Newtype` column type support.

```scala
libraryDependencies += "dev.hshn" %% "zlick-prelude" % "<version>"
```

## Setup

Mix zlick traits into your Slick profile:

```scala
import slick.jdbc.PostgresProfile
import zlick.*

object MyProfile extends PostgresProfile
  with ZioApiComponent
  with OptionDBIOComponent
  with EitherDBIOComponent {

  object MyApi extends JdbcAPI
    with ZioApi
    with OptionDBIOApi
    with EitherDBIOApi
}

import MyProfile.MyApi.*
```

## Usage

### Running DBIO as ZIO

```scala
// Expose database exceptions as ZIO failures
val result: Task[Int] =
  db.runZIO.attempt(DBIO.successful(42))

// Database exceptions become ZIO defects (for infallible queries)
val result: ZIO[Any, Nothing, Int] =
  db.runZIO.succeed(DBIO.successful(42))

// Map Either: Left becomes ZIO failure, Right becomes success
// Requires a transactional DBIO
val result: ZIO[Any, String, Int] =
  db.runZIO.fromEither {
    myTransactionalQuery.transactionally
  }
```

### Embedding ZIO into DBIO

```scala
val dbio: DBIO[Int] = ZIO.succeed(42).unsafeToDBIO
```

### Option Combinators

Extension methods on `DBIO[Option[A]]`:

```scala
// Apply f on Some, short-circuit on None
findById(id).semiFlatMap(user => updateLastLogin(user))

// Fallback if None
findById(id).orElseF(findByEmail(email))

// Convert to Either: Some → Right, None → Left
findById(id).someOrLeft(UserNotFound(id))

// Extract Some or fail with exception
findById(id).someOrFail(new NoSuchElementException("not found"))
```

### Either Combinators

Extension methods on `DBIO[Either[L, R]]`:

```scala
// Apply f on Right, short-circuit on Left
validate(input).semiflatMap(data => save(data))

// Apply pure function returning Either
validate(input).subflatMap(data => refine(data))

// Chain with another DBIO[Either[...]]
validate(input).flatMapF(data => checkUniqueness(data))

// Extract Right when Left is Nothing
DBIO.successful(Right(42): Either[Nothing, Int]).right
```

### Transactional Rollback on Left

Tag an `Either` action with `.rollbackOnLeft` to roll back the transaction when the result is `Left`, while still returning the `Left` value (instead of throwing):

```scala
val result: DBIO[Either[String, Int]] =
  insertRecord(data)
    .flatMap(_ => validate(data))
    .rollbackOnLeft
    .transactionally
// Right → committed, Left → rolled back (Left value preserved)
```

### Newtype Column Types (zlick-prelude)

Map ZIO Prelude `Newtype` values to Slick columns:

```scala
import zlick.NewtypeColumnTypesComponent

object MyProfile extends PostgresProfile with NewtypeColumnTypesComponent {
  object MyApi extends JdbcAPI with NewtypeColumnTypesApi
}
import MyProfile.MyApi.*

object UserId extends Newtype[String]
type UserId = UserId.Type

// No validation on read
given BaseColumnType[UserId] = MappedColumnType.newtypeWrap(UserId)

// Validates assertions on read
given BaseColumnType[UserId] = MappedColumnType.newtypeMake(UserId)
```

## Requirements

- Scala 3
- Slick 3.6.x
- ZIO 2.x

## License

Apache License 2.0
