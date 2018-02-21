lazy val root: Project = project.in(file(".")).dependsOn(latestSbtUmbrella)
lazy val latestSbtUmbrella = uri("git://github.com/kamon-io/kamon-sbt-umbrella.git")

addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.3.2")

addSbtPlugin("com.gilt.sbt" % "sbt-aspectjweaver" % "0.1.0")