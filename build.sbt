import ProjectSettings.ProjectFrom
import com.typesafe.sbt.packager.docker.Cmd

ThisBuild / scalaVersion        := "2.13.10"
ThisBuild / organization        := "it.pagopa"
ThisBuild / organizationName    := "Pagopa S.p.A."
ThisBuild / libraryDependencies := Dependencies.Jars.`server`
Global / onChangedBuildSource   := ReloadOnSourceChanges

ThisBuild / version := ComputeVersion.version

ThisBuild / githubOwner      := "pagopa"
ThisBuild / githubRepository := "interop-be-party-registry-proxy"
ThisBuild / resolvers += Resolver.githubPackages("pagopa")

lazy val generateCode = taskKey[Unit]("A task for generating the code starting from the swagger definition")

val packagePrefix = settingKey[String]("The package prefix derived from the uservice name")

packagePrefix := name.value
  .replaceFirst("interop-", "interop.")
  .replaceFirst("be-", "")
  .replaceAll("-", "")

val projectName = settingKey[String]("The project name prefix derived from the uservice name")

projectName := name.value
  .replaceFirst("interop-", "")
  .replaceFirst("be-", "")

generateCode := {
  import sys.process._

  val openApiCommand: String = {
    if (System.getProperty("os.name").toLowerCase.contains("win")) {
      "openapi-generator-cli-win.bat"
    } else {
      "openapi-generator-cli"
    }
  }

  Process(s"""${openApiCommand} generate -t template/scala-akka-http-server
             |                               -i src/main/resources/interface-specification.yml
             |                               -g scala-akka-http-server
             |                               -p projectName=${projectName.value}
             |                               -p invokerPackage=it.pagopa.${packagePrefix.value}.server
             |                               -p modelPackage=it.pagopa.${packagePrefix.value}.model
             |                               -p apiPackage=it.pagopa.${packagePrefix.value}.api
             |                               -p dateLibrary=java8
             |                               -p entityStrictnessTimeout=15
             |                               -o generated""".stripMargin).!!

  Process(s"""${openApiCommand} generate -t template/scala-akka-http-client
             |                               -i src/main/resources/interface-specification.yml
             |                               -g scala-akka
             |                               -p projectName=${projectName.value}
             |                               -p invokerPackage=it.pagopa.${packagePrefix.value}.client.invoker
             |                               -p modelPackage=it.pagopa.${packagePrefix.value}.client.model
             |                               -p apiPackage=it.pagopa.${packagePrefix.value}.client.api
             |                               -p dateLibrary=java8
             |                               -o client""".stripMargin).!!

}

val runStandalone = inputKey[Unit]("Run the app using standalone configuration")

runStandalone := {
  task(System.setProperty("config.file", "src/main/resources/application-standalone.conf")).value
  (Compile / run).evaluated
}

(Compile / compile) := ((Compile / compile) dependsOn generateCode).value
(Test / test)       := ((Test / test) dependsOn generateCode).value

cleanFiles += baseDirectory.value / "generated" / "src"

cleanFiles += baseDirectory.value / "generated" / "target"

cleanFiles += baseDirectory.value / "client" / "src"

cleanFiles += baseDirectory.value / "client" / "target"

lazy val generated = project
  .in(file("generated"))
  .settings(
    scalacOptions     := Seq(),
    scalafmtOnCompile := true,
    publish / skip    := true,
    publish           := (()),
    publishLocal      := (()),
    publishTo         := None
  )
  .setupBuildInfo

lazy val client = project
  .in(file("client"))
  .settings(
    name                := "interop-be-party-registry-proxy-client",
    scalacOptions       := Seq(),
    scalafmtOnCompile   := true,
    version             := (ThisBuild / version).value,
    libraryDependencies := Dependencies.Jars.client,
    updateOptions       := updateOptions.value.withGigahorse(false),
    Docker / publish    := {}
  )

lazy val root = (project in file("."))
  .settings(
    name                        := "interop-be-party-registry-proxy",
    Test / parallelExecution    := false,
    scalafmtOnCompile           := true,
    dockerBuildOptions ++= Seq("--network=host"),
    dockerRepository            := Some(System.getenv("ECR_REGISTRY")),
    dockerBaseImage             := "adoptopenjdk:11-jdk-hotspot",
    daemonUser                  := "daemon",
    Docker / version            := (ThisBuild / version).value.replaceAll("-SNAPSHOT", "-latest").toLowerCase,
    Docker / packageName        := s"${name.value}",
    Docker / dockerExposedPorts := Seq(8080),
    Docker / maintainer         := "https://pagopa.it",
    dockerCommands += Cmd("LABEL", s"org.opencontainers.image.source https://github.com/pagopa/${name.value}")
  )
  .aggregate(client)
  .dependsOn(generated)
  .enablePlugins(JavaAppPackaging, JavaAgent)
  .enablePlugins(DockerPlugin)
  .enablePlugins(NoPublishPlugin)
  .setupBuildInfo

javaAgents += "io.kamon" % "kanela-agent" % "1.0.16"

Test / fork := true
Test / javaOptions += "-Dconfig.file=src/test/resources/application-test.conf"
