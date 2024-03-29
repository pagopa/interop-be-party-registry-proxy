import PagopaVersions.commonsVersion
import Versions._
import sbt._

object Dependencies {

  private[this] object akka {
    lazy val namespace           = "com.typesafe.akka"
    lazy val actorTyped          = namespace                       %% "akka-actor-typed"     % akkaVersion
    lazy val actor               = namespace                       %% "akka-actor"           % akkaVersion
    lazy val stream              = namespace                       %% "akka-stream"          % akkaVersion
    lazy val http                = namespace                       %% "akka-http"            % akkaHttpVersion
    lazy val httpJson            = namespace                       %% "akka-http-spray-json" % akkaHttpVersion
    lazy val httpJson4s          = "de.heikoseeberger"             %% "akka-http-json4s"     % "1.39.2"
    lazy val management          = "com.lightbend.akka.management" %% "akka-management"      % akkaManagementVersion
    lazy val managementLogLevels =
      "com.lightbend.akka.management" %% "akka-management-loglevels-logback" % akkaManagementVersion
    lazy val slf4j   = namespace %% "akka-slf4j"               % akkaVersion
    lazy val testkit = namespace %% "akka-actor-testkit-typed" % akkaVersion
  }

  private[this] object lucene {
    lazy val namespace       = "org.apache.lucene"
    lazy val core            = namespace % "lucene-core"            % luceneVersion
    lazy val analyzersCommon = namespace % "lucene-analysis-common" % luceneVersion
    lazy val queryParser     = namespace % "lucene-queryparser"     % luceneVersion
    lazy val luceneSuggest   = namespace % "lucene-suggest"         % luceneVersion
  }

  private[this] object json4s {
    lazy val namespace = "org.json4s"
    lazy val jackson   = namespace %% "json4s-jackson" % json4sVersion
    lazy val ext       = namespace %% "json4s-ext"     % json4sVersion
  }

  private[this] object logback {
    lazy val namespace = "ch.qos.logback"
    lazy val classic   = namespace % "logback-classic" % logbackVersion
  }

  private[this] object pagopa {
    lazy val namespace = "it.pagopa"

    lazy val commonsUtils = namespace %% "interop-commons-utils" % commonsVersion
    lazy val commonsJwt   = namespace %% "interop-commons-jwt"   % commonsVersion
  }

  private[this] object scalatest {
    lazy val namespace = "org.scalatest"
    lazy val core      = namespace %% "scalatest" % scalatestVersion
  }

  private[this] object scalamock {
    lazy val namespace = "org.scalamock"
    lazy val core      = namespace %% "scalamock" % scalaMockVersion
  }

  private[this] object mustache {
    lazy val mustache = "com.github.spullara.mustache.java" % "compiler" % mustacheVersion
  }

  private[this] object shapeless {
    lazy val shapeless = "com.chuusai" %% "shapeless" % "2.3.10"
  }

  object Jars {
    lazy val `server`: Seq[ModuleID] = Seq(
      // For making Java 12 happy
      "javax.annotation"       % "javax.annotation-api" % "1.3.2" % "compile",
      //
      akka.actor               % Compile,
      akka.actorTyped          % Compile,
      akka.http                % Compile,
      akka.httpJson            % Compile,
      akka.management          % Compile,
      akka.managementLogLevels % Compile,
      akka.slf4j               % Compile,
      akka.stream              % Compile,
      logback.classic          % Compile,
      lucene.analyzersCommon   % Compile,
      lucene.core              % Compile,
      lucene.luceneSuggest     % Compile,
      lucene.queryParser       % Compile,
      mustache.mustache        % Compile,
      pagopa.commonsUtils      % Compile,
      pagopa.commonsJwt        % Compile,
      shapeless.shapeless      % Compile,
      scalatest.core           % Test,
      scalamock.core           % Test,
      akka.testkit             % Test
    )
    lazy val client: Seq[ModuleID]   =
      Seq(akka.stream, akka.http, akka.httpJson4s, akka.slf4j, json4s.jackson, json4s.ext, pagopa.commonsUtils).map(
        _ % Compile
      )
  }
}
