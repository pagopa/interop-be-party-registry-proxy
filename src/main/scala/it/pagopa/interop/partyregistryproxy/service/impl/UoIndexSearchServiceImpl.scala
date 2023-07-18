package it.pagopa.interop.partyregistryproxy.service.impl

import it.pagopa.interop.partyregistryproxy.common.system.ApplicationConfiguration
import it.pagopa.interop.partyregistryproxy.common.util.SearchField
import it.pagopa.interop.partyregistryproxy.model.Institution
import it.pagopa.interop.partyregistryproxy.service.IndexSearchService
import org.apache.lucene.index.DirectoryReader
import org.apache.lucene.store.FSDirectory

import java.nio.file.Paths
case object UoIndexSearchServiceImpl extends IndexSearchService[Institution] {

  private val dir: FSDirectory            = FSDirectory.open(Paths.get(ApplicationConfiguration.uooIndexDir))
  private val mainReader: DirectoryReader = DirectoryReader.open(dir)

  override def searchById(id: String): Either[Throwable, Option[Institution]] =
    InstitutionUtils.searchById(id)(mainReader)

  override def searchByExternalId(origin: String, originId: String): Either[Throwable, Option[Institution]] =
    InstitutionUtils.searchByExternalId(origin, originId)(mainReader)

  override def searchByText(
    searchingField: String,
    searchText: String,
    page: Int,
    limit: Int
  ): Either[Throwable, (List[Institution], Long)] =
    InstitutionUtils.searchByText(searchingField, searchText, page, limit)(mainReader)
  override def getAllItems(
    filters: Map[SearchField, String],
    page: Int,
    limit: Int
  ): Either[Throwable, (List[Institution], Long)] = InstitutionUtils.getAllItems(filters, page, limit)(mainReader)

}
