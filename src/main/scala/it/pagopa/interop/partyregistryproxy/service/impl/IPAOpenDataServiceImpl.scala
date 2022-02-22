package it.pagopa.interop.partyregistryproxy.service.impl

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.HttpExt
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.unmarshalling.Unmarshal
import it.pagopa.interop.partyregistryproxy.common.system.ApplicationConfiguration
import it.pagopa.interop.partyregistryproxy.model.{Category, Institution}
import it.pagopa.interop.partyregistryproxy.service.OpenDataService
import it.pagopa.interop.partyregistryproxy.service.impl.util.{
  OpenDataResponse,
  OpenDataResponseField,
  OpenDataResponseMarshaller
}

import scala.concurrent.{ExecutionContext, Future}

final case class IPAOpenDataServiceImpl(http: HttpExt)(implicit system: ActorSystem[Nothing], ec: ExecutionContext)
    extends OpenDataService
    with OpenDataResponseMarshaller {

  def getAllInstitutions: Future[List[Institution]] = {
    retrieveOpenData(ApplicationConfiguration.institutionsIpaOpenDataUrl).map(
      IPAOpenDataServiceImpl.extractInstitutions
    )
  }

  override def getAllCategories: Future[List[Category]] = {
    retrieveOpenData(ApplicationConfiguration.categoriesIpaOpenDataUrl).map(IPAOpenDataServiceImpl.extractCategories)
  }

  private def retrieveOpenData(uri: String): Future[OpenDataResponse] = {
    for {
      response         <- http.singleRequest(HttpRequest(uri = uri))
      openDataResponse <- Unmarshal(response).to[OpenDataResponse]
    } yield openDataResponse
  }
}

object IPAOpenDataServiceImpl {

  private object InstitutionsFields {
    final val id             = "Codice_IPA"
    final val description    = "Denominazione_ente"
    final val taxCode        = "Codice_fiscale_ente"
    final val category       = "Codice_Categoria"
    final val digitalAddress = "Mail1"
    final val address        = "Indirizzo"
    final val zipCode        = "CAP"

    final val fields: Set[String] =
      Set(id, description, taxCode, category, digitalAddress, address, zipCode)
  }

  private object CategoriesFields {
    final val code = "Codice_categoria"
    final val name = "Nome_categoria"
    final val kind = "Tipologia_categoria"

    final val fields: Set[String] = Set(code, name, kind)
  }

  def extractInstitutions(response: OpenDataResponse): List[Institution] = {

    val indexed: List[(OpenDataResponseField, Int)] = response.fields.zipWithIndex
    val filtered: List[(OpenDataResponseField, Int)] = indexed.filter { case (field, _) =>
      InstitutionsFields.fields.contains(field.id)
    }
    val mapped: Map[String, Int] = filtered.map { case (k, v) => k.id -> v }.toMap

    response.records.flatMap { record =>
      for {
        id             <- record(mapped(InstitutionsFields.id)).select[String]
        taxCode        <- mapped.get(InstitutionsFields.taxCode).flatMap(idx => record(idx).select[String])
        category       <- mapped.get(InstitutionsFields.category).flatMap(idx => record(idx).select[String])
        description    <- record(mapped(InstitutionsFields.description)).select[String]
        digitalAddress <- mapped.get(InstitutionsFields.digitalAddress).flatMap(idx => record(idx).select[String])
        address        <- mapped.get(InstitutionsFields.address).flatMap(idx => record(idx).select[String])
        zipCode        <- mapped.get(InstitutionsFields.zipCode).flatMap(idx => record(idx).select[String])
      } yield Institution(
        id = id,
        o = Some(id),
        ou = None,
        aoo = None,
        taxCode = taxCode,
        category = category,
        description = description,
        digitalAddress = digitalAddress,
        address = address,
        zipCode = zipCode,
        origin = ApplicationConfiguration.ipaOrigin
      )
    }
  }

  def extractCategories(response: OpenDataResponse): List[Category] = {

    val indexed: List[(OpenDataResponseField, Int)] = response.fields.zipWithIndex
    val filtered: List[(OpenDataResponseField, Int)] = indexed.filter { case (field, _) =>
      CategoriesFields.fields.contains(field.id)
    }
    val mapped: Map[String, Int] = filtered.map { case (k, v) => k.id -> v }.toMap

    response.records.flatMap { record =>
      for {
        code <- record(mapped(CategoriesFields.code)).select[String]
        name <- record(mapped(CategoriesFields.name)).select[String]
        kind <- record(mapped(CategoriesFields.kind)).select[String]
      } yield Category(code = code, name = name, kind = kind, origin = ApplicationConfiguration.ipaOrigin)
    }

  }
}
