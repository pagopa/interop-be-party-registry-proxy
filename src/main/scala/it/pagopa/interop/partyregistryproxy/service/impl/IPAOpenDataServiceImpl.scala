package it.pagopa.interop.partyregistryproxy.service.impl

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.HttpExt
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.unmarshalling.Unmarshal
import it.pagopa.interop.partyregistryproxy.common.system.ApplicationConfiguration
import it.pagopa.interop.partyregistryproxy.model.{Category, Institution}
import it.pagopa.interop.partyregistryproxy.service.impl.util.{
  OpenDataResponse,
  OpenDataResponseField,
  OpenDataResponseMarshaller
}
import it.pagopa.interop.partyregistryproxy.service.{InstitutionKind, OpenDataService}

import scala.concurrent.{ExecutionContext, Future}

final case class IPAOpenDataServiceImpl(http: HttpExt)(implicit system: ActorSystem[Nothing], ec: ExecutionContext)
    extends OpenDataService
    with OpenDataResponseMarshaller {

  def getAllInstitutions(
    categorySource: Map[String, String],
    institutionKind: InstitutionKind
  ): Future[List[Institution]] = {
    val url = institutionKind match {
      case InstitutionKind.Agency => ApplicationConfiguration.institutionsIpaOpenDataUrl
      case InstitutionKind.AOO    => ApplicationConfiguration.AOOIpaOpenDataUrl
      case InstitutionKind.UO     => ApplicationConfiguration.UOIpaOpenDataUrl
    }

    retrieveOpenData(url).map(IPAOpenDataServiceImpl.extractInstitutions(categorySource, institutionKind))
  }

  override def getAllCategories: Future[List[Category]] = {
    retrieveOpenData(ApplicationConfiguration.categoriesIpaOpenDataUrl).map(IPAOpenDataServiceImpl.extractCategories)
  }

  private def retrieveOpenData(uri: String): Future[OpenDataResponse] = for {
    response         <- http.singleRequest(HttpRequest(uri = uri))
    openDataResponse <- Unmarshal(response).to[OpenDataResponse]
  } yield openDataResponse

}

object IPAOpenDataServiceImpl {

  private object InstitutionsFields {
    final val originId       = "Codice_IPA"
    final val description    = "Denominazione_ente"
    final val taxCode        = "Codice_fiscale_ente"
    final val category       = "Codice_Categoria"
    final val digitalAddress = "Mail1"
    final val address        = "Indirizzo"
    final val zipCode        = "CAP"
    final val kind           = "Tipologia"
    final val aooId          = "Codice_uni_aoo"
    final val uoId           = "Codice_uni_uo"

    final val fields: Set[String] =
      Set(originId, description, taxCode, category, digitalAddress, address, zipCode, kind, aooId, uoId)
  }

  private object CategoriesFields {
    final val code = "Codice_categoria"
    final val name = "Nome_categoria"
    final val kind = "Tipologia_categoria"

    final val fields: Set[String] = Set(code, name, kind)
  }

  def extractInstitutions(categorySource: Map[String, String], institutionKind: InstitutionKind)(
    response: OpenDataResponse
  ): List[Institution] = {

    val indexed: List[(OpenDataResponseField, Int)]  = response.fields.zipWithIndex
    val filtered: List[(OpenDataResponseField, Int)] = indexed.filter { case (field, _) =>
      InstitutionsFields.fields.contains(field.id)
    }
    val mapped: Map[String, Int]                     = filtered.map { case (k, v) => k.id -> v }.toMap

    response.records.flatMap { record =>
      for {
        id             <- record(mapped(InstitutionsFields.taxCode)).select[String]
        originId       <- institutionKind match {
          case InstitutionKind.Agency => record(mapped(InstitutionsFields.originId)).select[String]
          case InstitutionKind.AOO    => record(mapped(InstitutionsFields.aooId)).select[String]
          case InstitutionKind.UO     => record(mapped(InstitutionsFields.uoId)).select[String]
        }
        taxCode        <- mapped.get(InstitutionsFields.taxCode).flatMap(idx => record(idx).select[String])
        category       <- institutionKind match {
          case InstitutionKind.Agency =>
            mapped.get(InstitutionsFields.category).flatMap(idx => record(idx).select[String])
          case _                      =>
            val institutionOriginId = record(mapped(InstitutionsFields.originId)).select[String]
            institutionOriginId.flatMap(categorySource.get)
        }
        description    <- record(mapped(InstitutionsFields.description)).select[String]
        digitalAddress <- mapped.get(InstitutionsFields.digitalAddress).flatMap(idx => record(idx).select[String])
        address        <- mapped.get(InstitutionsFields.address).flatMap(idx => record(idx).select[String])
        zipCode        <- mapped.get(InstitutionsFields.zipCode).flatMap(idx => record(idx).select[String])
        kind           <- mapped.get(InstitutionsFields.kind).flatMap(idx => record(idx).select[String])
      } yield Institution(
        id = id,
        originId = originId,
        o = Some(id),
        ou = None,  // TODO remember to fix id build when considering OU (see SELC-853 for the expression to use)
        aoo = None, // TODO remember to fix id build when considering AOO (see SELC-853 for the expression to use)
        taxCode = taxCode,
        category = category,
        description = description,
        digitalAddress = digitalAddress,
        address = address,
        zipCode = zipCode,
        origin = ApplicationConfiguration.ipaOrigin,
        kind = kind
      )
    }
  }

  def extractCategories(response: OpenDataResponse): List[Category] = {

    val indexed: List[(OpenDataResponseField, Int)]  = response.fields.zipWithIndex
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
