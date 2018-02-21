import scala.util.Try
import sbt.ProcessLogger

val kamonCassandraVersion = "1.0.0"

val kamonCore = "io.kamon" %% "kamon-core" % "1.0.0"

val core = "com.datastax.cassandra" % "cassandra-driver-core" % "3.3.2"

val kamonTestKit = "io.kamon" %% "kamon-testkit" % "1.0.0"
val cassandraUnit = "org.cassandraunit" % "cassandra-unit" % "2.1.9.2"

val nettyCommon = "io.netty" % "netty-common" % "4.1.21.Final"



lazy val root = (project in file("."))
  .aggregate(agent, playground)
  .settings(Seq(
    crossScalaVersions := Seq("2.11.11", "2.12.4")
  ))
  .settings(noPublishing)

lazy val agent = (project in file("agent"))
  .settings(Seq(
    organization := "eu.monniot.kamon",
    name := "kamon-cassandra",
    scalaVersion := "2.12.4",
    crossScalaVersions := Seq("2.11.11", "2.12.4"),
    bintrayOrganization := None,
    gitAwareVersion := kamonCassandraVersion
  ))
  .settings(resolvers += Resolver.bintrayRepo("fmonniot", "snapshots"))
  .settings(resolvers += Resolver.bintrayRepo("fmonniot", "maven"))
  .settings(aspectJSettings: _*)
  .settings(
    libraryDependencies ++=
      compileScope(kamonCore) ++
        providedScope(aspectJ, core) ++
        testScope(scalatest, kamonTestKit, logbackClassic, cassandraUnit, nettyCommon)
  )

lazy val playground = (project in file("playground"))
  .settings(Seq(
    name := "playground",
    crossScalaVersions := Seq.empty,

    connectInput in run := true,
    cancelable in Global := true
  ))
  .settings(noPublishing)
