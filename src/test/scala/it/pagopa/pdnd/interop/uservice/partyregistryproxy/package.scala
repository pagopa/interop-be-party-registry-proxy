package it.pagopa.pdnd.interop.uservice

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, ResponseEntity}
import akka.http.scaladsl.unmarshalling.{Unmarshal, Unmarshaller}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext}

package object partyregistryproxy {

  val url: String =
    s"http://localhost:8088/pdnd-interop-uservice-party-registry-proxy/${buildinfo.BuildInfo.interfaceVersion}"

  def makeRequest[A](path: String)(implicit
    actorSystem: ActorSystem,
    Unmarshaller: Unmarshaller[ResponseEntity, A],
    ec: ExecutionContext
  ): SpecResult[A] = {
    Await.result(
      Http().singleRequest(HttpRequest(uri = s"$url/$path", method = HttpMethods.GET)).flatMap { response =>
        Unmarshal(response.entity).to[A].map(SpecResult(response.status, _))
      },
      Duration.Inf
    )
  }
}
