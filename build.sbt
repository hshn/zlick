ThisBuild / scalaVersion := "3.8.1"
ThisBuild / organization := "dev.hshn"
ThisBuild / homepage     := Some(url("https://github.com/hshn/zlick"))
ThisBuild / licenses     := List("Apache-2.0" -> url("https://www.apache.org/licenses/LICENSE-2.0"))
ThisBuild / developers   := List(
  Developer("hshn", "Shota Hoshino", "sht.hshn@gmail.com", url("https://github.com/hshn"))
)
ThisBuild / description   := "Slick ZIO bindings for Scala 3"
ThisBuild / versionScheme := Some("early-semver")

val slickVersion      = "3.6.1"
val zioVersion        = "2.1.22"
val zioPreludeVersion = "1.0.0-RC46"
val h2Version         = "2.2.224"

lazy val zlick = (project in file(".") withId "zlick")
  .aggregate(zlickCore, zlickPrelude)
  .settings(
    publish / skip := true
  )

lazy val zlickCore = (project in file("zlick-core"))
  .settings(
    name := "zlick-core",
    libraryDependencies ++= Seq(
      "com.typesafe.slick" %% "slick"        % slickVersion,
      "dev.zio"            %% "zio"           % zioVersion,
      "dev.zio"            %% "zio-test"     % zioVersion % Test,
      "dev.zio"            %% "zio-test-sbt" % zioVersion % Test,
      "com.h2database"      % "h2"           % h2Version  % Test,
    ),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
  )

lazy val zlickPrelude = (project in file("zlick-prelude"))
  .settings(
    name := "zlick-prelude",
    libraryDependencies ++= Seq(
      "com.typesafe.slick" %% "slick"        % slickVersion,
      "dev.zio"            %% "zio-prelude"  % zioPreludeVersion,
      "dev.zio"            %% "zio-test"     % zioVersion % Test,
      "dev.zio"            %% "zio-test-sbt" % zioVersion % Test,
      "com.h2database"      % "h2"           % h2Version  % Test,
    ),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
  )
