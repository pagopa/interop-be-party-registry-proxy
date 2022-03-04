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
          o = Some("PAGOPA"),
          ou = None,
          aoo = None,
          taxCode = "99999999",
          category = "PGPA",
          description = "PagoPA",
          digitalAddress = "apogap@pagopa.it",
          address = "Piazza Colonna 370, Roma",
          zipCode = "00187",
          origin = INTEROP_ORIGIN
        )
      )
    )

  override def getAllCategories: Future[List[Category]] =
    Future.successful(List(Category(code = "PGPA", name = "PagoPA", kind = "PGP", origin = INTEROP_ORIGIN)))
}
