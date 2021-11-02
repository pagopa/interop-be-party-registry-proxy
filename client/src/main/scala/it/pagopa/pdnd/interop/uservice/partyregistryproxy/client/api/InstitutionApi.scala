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

import it.pagopa.pdnd.interop.uservice.partyregistryproxy.client.model.Categories
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.client.model.Institution
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.client.model.Institutions
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.client.model.Problem
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.client.invoker._
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.client.invoker.CollectionFormats._
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.client.invoker.ApiKeyLocations._

object InstitutionApi {

  def apply(baseUrl: String = "https://gateway.interop.pdnd.dev/pdnd-interop-uservice-party-registry-proxy/}") = new InstitutionApi(baseUrl)
}

class InstitutionApi(baseUrl: String) {
  
  /**
   * Returns the ipa categories list
   * 
   * Expected answers:
   *   code 200 : Categories (successful operation)
   */
  def getCategories(): ApiRequest[Categories] =
    ApiRequest[Categories](ApiMethods.GET, baseUrl, "/catergories", "application/json")
      .withSuccessResponse[Categories](200)
      

  /**
   * Returns a single institution
   * 
   * Expected answers:
   *   code 200 : Institution (successful operation)
   *   code 400 : Problem (Invalid ID supplied)
   *   code 404 : Problem (Institution not found)
   * 
   * @param institutionId ID of institution to return
   */
  def getInstitutionById(institutionId: String): ApiRequest[Institution] =
    ApiRequest[Institution](ApiMethods.GET, baseUrl, "/institutions/{institutionId}", "application/json")
      .withPathParam("institutionId", institutionId)
      .withSuccessResponse[Institution](200)
      .withErrorResponse[Problem](400)
      .withErrorResponse[Problem](404)
      

  /**
   * Returns a single institution
   * 
   * Expected answers:
   *   code 200 : Institutions (successful operation)
   *   code 400 : Problem (Invalid ID supplied)
   *   code 404 : Problem (Institution not found)
   * 
   * @param search 
   * @param page 
   * @param limit 
   */
  def searchInstitution(search: String, page: Int, limit: Int): ApiRequest[Institutions] =
    ApiRequest[Institutions](ApiMethods.GET, baseUrl, "/institutions", "application/json")
      .withQueryParam("search", search)
      .withQueryParam("page", page)
      .withQueryParam("limit", limit)
      .withSuccessResponse[Institutions](200)
      .withErrorResponse[Problem](400)
      .withErrorResponse[Problem](404)
      



}
