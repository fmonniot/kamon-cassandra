val kamonCassandraVersion = "1.0.0"

val kamonCore = "io.kamon" %% "kamon-core" % "1.0.0"
val core = "com.datastax.cassandra" % "cassandra-driver-core" % "3.4.0"
val kamonTestKit = "io.kamon" %% "kamon-testkit" % "1.0.0"


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
  .settings(aspectJSettings: _*)
  .settings(
    libraryDependencies ++=
      compileScope(kamonCore) ++
        providedScope(aspectJ, core) ++
        testScope(scalatest, kamonTestKit, logbackClassic)
  )

import com.typesafe.sbt.packager.docker.{Cmd, ExecCmd}
lazy val playground = (project in file("playground"))
  .settings(Seq(
    name := "playground",
    crossScalaVersions := Seq.empty,
    scalaVersion := "2.12.4",

    connectInput in run := true,
    cancelable in Global := true
  ))
  .settings(aspectJSettings: _*)
  .dependsOn(agent)
  .settings(noPublishing)
  .settings(libraryDependencies ++= Seq(
    core, // Cassandra driver

    "com.typesafe.akka" %% "akka-http"            % "10.0.11",
    "com.typesafe.akka" %% "akka-stream"          % "2.5.8",

    "de.heikoseeberger" %% "akka-http-circe"      % "1.20.0-RC2",

    "io.circe"          %% "circe-core"           % "0.9.1",
    "io.circe"          %% "circe-generic"        % "0.9.1",
    "io.circe"          %% "circe-parser"         % "0.9.1",

    "io.kamon"          %% "kamon-core"           % "1.0.0",
    "io.kamon"          %% "kamon-system-metrics" % "1.0.0",
    "io.kamon"          %% "kamon-prometheus"     % "1.0.0",
    "io.kamon"          %% "kamon-zipkin"         % "1.0.0",
    "io.kamon"          %% "kamon-jaeger"         % "1.0.0",
    "io.kamon"          %% "kamon-akka-http-2.5"  % "1.0.1"
  ))
  .enablePlugins(AshScriptPlugin)
  .enablePlugins(AspectJWeaver)
  .settings(
    defaultLinuxInstallLocation in Docker := "/opt/playground",
    version in Docker := "latest",
    dockerCommands := Seq(
      Cmd("FROM", "alpine:3.3"),
      Cmd("RUN apk upgrade --update && apk add --update openjdk8-jre"),
      Cmd("ADD", "opt /opt"),
      Cmd("EXPOSE", "42042 42043"),
      Cmd("RUN", "mkdir", "-p", "/var/log/playground"),
      ExecCmd("ENTRYPOINT", "/opt/playground/bin/playground")
    )
  )
