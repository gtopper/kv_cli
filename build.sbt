name := "kv_cli"

organization := "io.iguaz"

version := "0.1"

scalaVersion := "2.11.12"

scalacOptions += "-target:jvm-1.8"

libraryDependencies += "org.json4s" %% "json4s-jackson" % "3.5.4"

assemblyOption in assembly := (assemblyOption in assembly).value.copy(includeScala = false)

assemblyExcludedJars in assembly := {
  val cp = (fullClasspath in assembly).value
  cp filter {
    _.data.getName.contains("v3io")
  }
}
