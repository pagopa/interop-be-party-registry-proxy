/**
 * Party Registry Proxy Server
 * This service is the proxy to the party registry
 *
 * The version of the OpenAPI document: {{version}}
 * Contact: support@example.com
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */
package it.pagopa.pdnd.interop.uservice.partyregistryproxy.client.api

import it.pagopa.pdnd.interop.uservice.partyregistryproxy.client.model.Problem
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.client.invoker._
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.client.invoker.CollectionFormats._
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.client.invoker.ApiKeyLocations._

object HealthApi {

  def apply(baseUrl: String = "https://gateway.interop.pdnd.dev/pdnd-interop-uservice-party-registry-proxy/}") = new HealthApi(baseUrl)
}

class HealthApi(baseUrl: String) {
  
  /**
   * Return ok
   * 
   * Expected answers:
   *   code 200 : Problem (successful operation)
   */
  def getStatus(): ApiRequest[Problem] =
    ApiRequest[Problem](ApiMethods.GET, baseUrl, "/status", "application/json")
      .withSuccessResponse[Problem](200)
      



}

