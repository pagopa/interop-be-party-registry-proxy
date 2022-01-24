package it.pagopa.pdnd.interop.uservice.partyregistryproxy.service

import it.pagopa.pdnd.interop.uservice.partyregistryproxy.common.system.ApplicationConfiguration
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.model.{Category, Institution, Manager}
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.service.impl.util.{OpenDataResponse, OpenDataResponseField}

import scala.concurrent.Future

trait OpenDataService {
  def getAllInstitutions: Future[List[Institution]]
  def getAllCategories: Future[List[Category]]
}

object OpenDataService {

  private object InstitutionsFields {
    final val id                = "Codice_IPA"
    final val description       = "Denominazione_ente"
    final val taxCode           = "Codice_fiscale_ente"
    final val category          = "Codice_Categoria"
    final val managerGivenName  = "Nome_responsabile"
    final val managerFamilyName = "Cognome_responsabile"
    final val digitalAddress    = "Mail1"

    final val fields: Set[String] =
      Set(id, description, taxCode, category, managerGivenName, managerFamilyName, digitalAddress)
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
        id                <- record(mapped(InstitutionsFields.id)).select[String]
        managerGivenName  <- record(mapped(InstitutionsFields.managerGivenName)).select[String]
        managerFamilyName <- record(mapped(InstitutionsFields.managerFamilyName)).select[String]
        taxCode           <- mapped.get(InstitutionsFields.taxCode).flatMap(idx => record(idx).select[String])
        category          <- mapped.get(InstitutionsFields.category).flatMap(idx => record(idx).select[String])
        description       <- record(mapped(InstitutionsFields.description)).select[String]
        digitalAddress    <- mapped.get(InstitutionsFields.digitalAddress).flatMap(idx => record(idx).select[String])
      } yield Institution(
        id = id,
        o = Some(id),
        ou = None,
        aoo = None,
        taxCode = taxCode,
        category = category,
        manager = Manager(managerGivenName, managerFamilyName),
        description = description,
        digitalAddress = digitalAddress
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
        origin = ApplicationConfiguration.registryOrigin
      } yield Category(code = code, name = name, kind = kind, origin = origin)
    }

  }
}
