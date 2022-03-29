package it.pagopa.interop.partyregistryproxy.common

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.actor.typed.{ActorSystem, Scheduler}
import akka.util.Timeout
import akka.{actor => classic}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

package object system {

  val actorSystem: ActorSystem[Nothing] =
    ActorSystem[Nothing](Behaviors.empty, "interop-be-party-registry-proxy")

  implicit val executionContext: ExecutionContext = actorSystem.executionContext

  implicit val classicActorSystem: classic.ActorSystem = actorSystem.toClassic

  implicit val timeout: Timeout = 3.seconds

  implicit val scheduler: Scheduler = actorSystem.scheduler

}
