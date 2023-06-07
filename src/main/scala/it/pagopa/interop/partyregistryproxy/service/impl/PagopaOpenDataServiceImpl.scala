package it.pagopa.interop.partyregistryproxy.service.impl

import it.pagopa.interop.partyregistryproxy.model.{Category, Institution}
import it.pagopa.interop.partyregistryproxy.service.OpenDataService

import scala.concurrent.Future

object PagopaOpenDataServiceImpl extends OpenDataService {

  final lazy val INTEROP_ORIGIN = "Interop"

  override def getAllInstitutions: Future[List[Institution]] =
    Future.successful(
      List(
        Institution(
          id = "PagoPA",
          originId = "PagoPA",
          o = Some("PAGOPA"),
          ou = None,
          aoo = None,
          taxCode = "99999999",
          category = "PGPA",
          description = "PagoPA",
          digitalAddress = "apogap@pagopa.it",
          address = "Piazza Colonna 370, Roma",
          zipCode = "00187",
          origin = INTEROP_ORIGIN,
          kind = "Privato"
        ),
        Institution(
          id = "TestANPR",
          originId = "TestANPR",
          o = Some("TestANPR"),
          ou = None,
          aoo = None,
          taxCode = "99999988",
          category = "ANPR",
          description = "ANPR - Test",
          digitalAddress = "testanprtest@test.it",
          address = "Piazza del Viminale 1, Roma",
          zipCode = "00184",
          origin = INTEROP_ORIGIN,
          kind = "Pubbliche Amministrazioni"
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
