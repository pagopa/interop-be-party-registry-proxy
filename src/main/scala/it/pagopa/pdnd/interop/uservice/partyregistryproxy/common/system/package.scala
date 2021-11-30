package it.pagopa.pdnd.interop.uservice.partyregistryproxy.common

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.actor.typed.{ActorSystem, Scheduler}
import akka.http.scaladsl.server.Directives.Authenticator
import akka.http.scaladsl.server.directives.Credentials
import akka.util.Timeout
import akka.{actor => classic}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

package object system {

  val projectName = "pdnd-interop-uservice-party-registry-proxy"

  val actorSystem: ActorSystem[Nothing] =
    ActorSystem[Nothing](Behaviors.empty, projectName)

  implicit val executionContext: ExecutionContext = actorSystem.executionContext

  implicit val classicActorSystem: classic.ActorSystem = actorSystem.toClassic

  implicit val timeout: Timeout = 3.seconds

  implicit val scheduler: Scheduler = actorSystem.scheduler

  object Authenticator extends Authenticator[Unit] {
    override def apply(credentials: Credentials): Option[Unit] = Some(())
  }

}
