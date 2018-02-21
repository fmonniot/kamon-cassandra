/*
 * Copyright © 2018 François Monniot <https://github.com/fmonniot/kamon-cassandra>
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 *  except in compliance with the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the
 *  License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 *  either express or implied. See the License for the specific language governing permissions
 *  and limitations under the License.
 */

import io.kamon.sbt.umbrella.KamonSbtUmbrella
import sbt.Keys._
import sbt.{AutoPlugin, Def, Level, Plugins, Process, ProcessLogger, Resolver, SettingKey, ThisBuild}

import scala.util.Try


object TravisCI extends AutoPlugin {

  // To be able to overrides their `publish` task
  override def requires: Plugins = KamonSbtUmbrella

  override def trigger = allRequirements

  object autoImport {
    val gitAwareVersion = SettingKey[String]("git-aware-version", "A version which can be changed depending on the current git tag")
    val travisCI = SettingKey[Boolean]("travis-ci", "Indicates whether is build is done by Travis or not")
  }

  import autoImport._

  override def projectSettings: Seq[Def.Setting[_]] = Seq(
    version in ThisBuild := versionSetting.value,

    // We are overriding the Kamon publish task here, but as they just check for a dirty repo
    // and we are restricting the build to Travis CI it's not really a problem.
    travisCI := boolEnv("TRAVIS") && boolEnv("CI"),

    // Let us overrides the log level directly from an environment variable
    logLevel := sys.env.get("LOG_LEVEL").flatMap(Level(_)).getOrElse(Level.Info),

    resolvers ++= Seq(snapshotsResolver, releaseResolver),

    // Make SBT happy (fix RuntimeException: Repository for publishing is not specified.)
    publishTo := {
      if (isSnapshot.value) Option(snapshotsResolver)
      else Option(releaseResolver)
    }
  )

  private val snapshotsResolver = Resolver.bintrayRepo("fmonniot", "snapshots")
  private val releaseResolver = Resolver.bintrayRepo("fmonniot", "maven")

  private def boolEnv(name: String) = sys.env.get(name).flatMap(s => Try(s.toBoolean).toOption).getOrElse(false)

  private val noOpProcessLogger = new ProcessLogger {
    override def error(s: => String): Unit = ()

    override def buffer[T](f: => T): T = f

    override def info(s: => String): Unit = ()
  }

  def versionSetting = Def.settingDyn[String] {
    val definedVersion = gitAwareVersion.value
    val commit = Process("git rev-parse HEAD").lines.head
    val tag = Try(Process(s"git describe --exact-match --tags $commit").lines(noOpProcessLogger).head).toOption

    val v = tag match {
      case Some(tagVersion) if tagVersion == definedVersion.stripSuffix("-SNAPSHOT") =>
        // Release version
        tagVersion

      case _ =>
        // SNAPSHOT
        if (definedVersion.endsWith("-SNAPSHOT")) definedVersion
        else s"$definedVersion-SNAPSHOT"
    }

    Def.setting(v)
  }
}
