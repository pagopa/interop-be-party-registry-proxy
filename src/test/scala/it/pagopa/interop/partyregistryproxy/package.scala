package it.pagopa.interop

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers.OAuth2BearerToken
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.{Unmarshal, Unmarshaller}

import java.net.InetAddress
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext}

package object partyregistryproxy {

  val url: String =
    s"http://localhost:8088/party-registry-proxy/${buildinfo.BuildInfo.interfaceVersion}"

  final val requestHeaders: Seq[HttpHeader] =
    Seq(
      headers.Authorization(OAuth2BearerToken("token")),
      headers.RawHeader("X-Correlation-Id", "test-id"),
      headers.`X-Forwarded-For`(RemoteAddress(InetAddress.getByName("127.0.0.1")))
    )

  def makeRequest[A](path: String)(implicit
    actorSystem: ActorSystem,
    Unmarshaller: Unmarshaller[ResponseEntity, A],
    ec: ExecutionContext
  ): SpecResult[A] = {
    Await.result(
      Http()
        .singleRequest(HttpRequest(uri = s"$url/$path", method = HttpMethods.GET, headers = requestHeaders))
        .flatMap { response =>
          Unmarshal(response.entity).to[A].map(SpecResult(response.status, _))
        },
      Duration.Inf
    )
  }
}
