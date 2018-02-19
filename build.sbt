import scala.util.Try
import sbt.ProcessLogger

val kamonCassandraVersion = "1.0.1"

val kamonCore = "io.kamon" %% "kamon-core" % "1.0.0"
val kamonTestKit = "io.kamon" %% "kamon-testkit" % "1.0.0"

val core = "com.datastax.cassandra" % "cassandra-driver-core" % "3.4.0"
val cassandraUnit = "org.cassandraunit" % "cassandra-unit" % "2.1.9.2"

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


def boolEnv(name: String) = sys.env.get(name).flatMap(s => Try(s.toBoolean).toOption).getOrElse(false)

val noOpProcessLogger = new ProcessLogger {
  override def error(s: => String): Unit = ()

  override def buffer[T](f: => T): T = f

  override def info(s: => String): Unit = ()
}

def publishOnlyWithTravis = Def.taskDyn[Unit] {
  val log = streams.value.log
  val isTravis = boolEnv("CI") && boolEnv("TRAVIS")
  val branch = Process("git rev-parse --abbrev-ref HEAD").lines.head

  log.debug(s"is running on travis: $isTravis")
  log.debug(s"is running on branch: $branch")

  if (isTravis && branch == "master") (publish in ThisBuild).toTask
  else Def.task(log.warn("Won't publish unless built by Travis CI on master"))
}