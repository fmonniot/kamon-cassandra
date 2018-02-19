
val kamonCassandraVersion = "1.0.0-SNAPSHOT"

val kamonCore = "io.kamon" %% "kamon-core" % "1.0.0"
val kamonTestKit = "io.kamon" %% "kamon-testkit" % "1.0.0"

val core = "com.datastax.cassandra" % "cassandra-driver-core" % "3.4.0"
val cassandraUnit = "org.cassandraunit" % "cassandra-unit" % "2.1.9.2"

lazy val root = (project in file("."))
  .aggregate(agent, playground)
  .settings(noPublishing: _*)
  .settings(Seq(crossScalaVersions := Seq("2.11.11", "2.12.4")))

lazy val agent = (project in file("agent"))
  .settings(Seq(
    organization := "eu.monniot.kamon",
    name := "kamon-cassandra",
    scalaVersion := "2.12.4",
    crossScalaVersions := Seq("2.11.11", "2.12.4"),
    version in ThisBuild := kamonCassandraVersion,
    bintrayOrganization := None

  ))
  .settings(resolvers += Resolver.bintrayRepo("fmonniot", "snapshots"))
  .settings(resolvers += Resolver.bintrayRepo("fmonniot", "maven"))
  .settings(aspectJSettings: _*)
  .settings(
    libraryDependencies ++=
      compileScope(kamonCore) ++
        providedScope(aspectJ, core) ++
        testScope(scalatest, kamonTestKit, logbackClassic, cassandraUnit)
  )

lazy val playground = (project in file("playground"))
  .settings(Seq(
    name := "playground",
    crossScalaVersions := Seq.empty,

    connectInput in run := true,
    cancelable in Global := true
  ))
  .settings(noPublishing)