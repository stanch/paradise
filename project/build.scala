import sbt._
import Keys._

object build extends Build {
  lazy val sharedSettings = Defaults.defaultSettings ++ Seq(
    scalaVersion := "2.10.2-RC1",
    crossVersion := CrossVersion.full,
    version := "2.0.0-SNAPSHOT",
    scalaOrganization := "org.scala-lang.virtualized",
    organization := "org.scala-lang.virtualized.plugins",
    resolvers += Resolver.sonatypeRepo("snapshots"),
    resolvers += Resolver.sonatypeRepo("releases"),
    publishMavenStyle := true,
    publishArtifact in Test := false,
    scalacOptions ++= Seq("-deprecation", "-feature"),
    parallelExecution in Test := false, // hello, reflection sync!!
    logBuffered := false,
    scalaHome := {
      val scalaHome = System.getProperty("paradise.scala.home")
      if (scalaHome != null) {
        println(s"Going for custom scala home at $scalaHome")
        Some(file(scalaHome))
      } else None
    }
  )

  def loadCredentials(): List[Credentials] = {
    val mavenSettingsFile = System.getProperty("maven.settings.file")
    if (mavenSettingsFile != null) {
      println("Loading Sonatype credentials from " + mavenSettingsFile)
      try {
        import scala.xml._
        val settings = XML.loadFile(mavenSettingsFile)
        def readServerConfig(key: String) = (settings \\ "settings" \\ "servers" \\ "server" \\ key).head.text
        List(Credentials(
          "Sonatype Nexus Repository Manager",
          "oss.sonatype.org",
          readServerConfig("username"),
          readServerConfig("password")
        ))
      } catch {
        case ex: Exception =>
          println("Failed to load Maven settings from " + mavenSettingsFile + ": " + ex)
          Nil
      }
    } else {
      // println("Sonatype credentials cannot be loaded: -Dmaven.settings.file is not specified.")
      Nil
    }
  }

  lazy val plugin = Project(
    id   = "macro-paradise",
    base = file("plugin")
  ) settings (
    sharedSettings : _*
  ) settings (
    resourceDirectory in Compile <<= baseDirectory(_ / "src" / "main" / "scala" / "org" / "scalalang" / "macroparadise" / "embedded"),
    libraryDependencies <+= (scalaVersion)("org.scala-lang.virtualized" % "scala-library" % _),
    libraryDependencies <+= (scalaVersion)("org.scala-lang.virtualized" % "scala-reflect" % _),
    libraryDependencies <+= (scalaVersion)("org.scala-lang.virtualized" % "scala-compiler" % _),
    // TODO: how to I make this recursion work?
    // run <<= run in Compile in sandbox,
    // test <<= test in Test in tests
    publishMavenStyle := true,
    publishArtifact in Test := false,
    publishTo <<= version { v: String =>
      val nexus = "https://oss.sonatype.org/"
      if (v.trim.endsWith("SNAPSHOT"))
        Some("snapshots" at nexus + "content/repositories/snapshots")
      else
        Some("releases" at nexus + "service/local/staging/deploy/maven2")
    },
    pomIncludeRepository := { x => false },
    pomExtra := (
      <description>Empowers production Scala compiler with latest macro developments</description>
      <url>https://github.com/scalamacros/paradise</url>
      <inceptionYear>2012</inceptionYear>
      <organization>
        <name>LAMP/EPFL</name>
        <url>http://lamp.epfl.ch/</url>
      </organization>
      <licenses>
        <license>
          <name>BSD-like</name>
          <url>http://www.scala-lang.org/downloads/license.html
          </url>
          <distribution>repo</distribution>
        </license>
      </licenses>
      <scm>
        <url>git://github.com/scalamacros/paradise.git</url>
        <connection>scm:git:git://github.com/scalamacros/paradise.git</connection>
      </scm>
      <issueManagement>
        <system>GitHub</system>
        <url>https://github.com/scalamacros/paradise/issues</url>
      </issueManagement>
      <developers>
        <developer>
          <id>lamp</id>
          <name>EPFL LAMP</name>
        </developer>
      </developers>
    ),
    credentials ++= loadCredentials()
  )

  lazy val usePluginSettings = Seq(
    scalacOptions in Compile <++= (Keys.`package` in (plugin, Compile)) map { (jar: File) =>
      System.setProperty("macroparadise.plugin.jar", jar.getAbsolutePath)
      val addPlugin = "-Xplugin:" + jar.getAbsolutePath
      // Thanks Jason for this cool idea (taken from https://github.com/retronym/boxer)
      // add plugin timestamp to compiler options to trigger recompile of
      // main after editing the plugin. (Otherwise a 'clean' is needed.)
      val dummy = "-Jdummy=" + jar.lastModified
      Seq(addPlugin, dummy)
    }
  )

  lazy val sandbox = Project(
    id   = "sandbox",
    base = file("sandbox")
  ) settings (
    sharedSettings ++ usePluginSettings: _*
  ) settings (
    libraryDependencies <+= (scalaVersion)("org.scala-lang.virtualized" % "scala-reflect" % _),
    publishArtifact in Compile := false
  )

  lazy val tests = Project(
    id   = "tests",
    base = file("tests")
  ) settings (
    sharedSettings ++ usePluginSettings: _*
  ) settings (
    libraryDependencies <+= (scalaVersion)("org.scala-lang.virtualized" % "scala-reflect" % _),
    libraryDependencies <+= (scalaVersion)("org.scala-lang.virtualized" % "scala-compiler" % _),
    libraryDependencies += "org.scalatest" %% "scalatest" % "1.9.1" % "test",
    libraryDependencies += "org.scalacheck" %% "scalacheck" % "1.10.1" % "test",
    publishArtifact in Compile := false,
    unmanagedSourceDirectories in Test <<= (scalaSource in Test) { (root: File) =>
      // TODO: I haven't yet ported negative tests to SBT, so for now I'm excluding them
      val (anns :: Nil, others) = root.listFiles.toList.partition(_.getName == "annotations")
      val (negAnns, otherAnns) = anns.listFiles.toList.partition(_.getName == "neg")
      otherAnns ++ others
    },
    scalacOptions ++= Seq()
    // scalacOptions ++= Seq("-Xprint:typer")
    // scalacOptions ++= Seq("-Xlog-implicits")
  )
}
