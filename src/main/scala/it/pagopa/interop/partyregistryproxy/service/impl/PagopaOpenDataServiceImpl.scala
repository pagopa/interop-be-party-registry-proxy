package it.pagopa.interop.partyregistryproxy.service.impl

import it.pagopa.interop.partyregistryproxy.common.util.InstitutionDetails
import it.pagopa.interop.partyregistryproxy.model.{Category, Classification, Institution}
import it.pagopa.interop.partyregistryproxy.service.{InstitutionKind, OpenDataService}

import scala.concurrent.Future

object PagopaOpenDataServiceImpl extends OpenDataService {

  final lazy val INTEROP_ORIGIN = "Interop"

  override def getAllInstitutions(
    institutionsDetails: Map[String, InstitutionDetails],
    institutionKind: InstitutionKind
  ): Future[List[Institution]] =
    Future.successful(
      List(
        Institution(
          id = "PagoPA",
          originId = "PagoPA",
          taxCode = "99999999",
          category = "PGPA",
          description = "PagoPA",
          digitalAddress = "apogap@pagopa.it",
          address = "Piazza Colonna 370, Roma",
          zipCode = "00187",
          origin = INTEROP_ORIGIN,
          kind = "Privato",
          classification = Classification.AGENCY
        ),
        Institution(
          id = "TestANPR",
          originId = "TestANPR",
          taxCode = "99999988",
          category = "ANPR",
          description = "ANPR - Test",
          digitalAddress = "testanprtest@test.it",
          address = "Piazza del Viminale 1, Roma",
          zipCode = "00184",
          origin = INTEROP_ORIGIN,
          kind = "Pubbliche Amministrazioni",
          classification = Classification.AGENCY
        )
      )
    )

  override def getAllCategories: Future[List[Category]] =
    Future.successful(
      List(
        Category(code = "PGPA", name = "PagoPA", kind = "PGP", origin = INTEROP_ORIGIN),
        Category(code = "ANPR", name = "ANPR", kind = "ANPR", origin = INTEROP_ORIGIN)
      )
    )
}
