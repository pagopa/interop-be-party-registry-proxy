package it.pagopa.interop.partyregistryproxy.service.impl

import it.pagopa.interop.partyregistryproxy.common.system.ApplicationConfiguration
import it.pagopa.interop.partyregistryproxy.common.util.InstitutionField.ID
import it.pagopa.interop.partyregistryproxy.common.util.SearchField
import it.pagopa.interop.partyregistryproxy.model.Institution
import it.pagopa.interop.partyregistryproxy.service.IndexSearchService
import it.pagopa.interop.partyregistryproxy.service.impl.analizer.InstitutionTokenAnalyzer
import it.pagopa.interop.partyregistryproxy.service.impl.util.DocumentConverter
import org.apache.lucene.index.{DirectoryReader, Term}
import org.apache.lucene.search._
import org.apache.lucene.store.FSDirectory

import java.nio.file.Paths
import scala.util.{Failure, Try}

case object InstitutionIndexSearchServiceImpl extends IndexSearchService[Institution] {

  private val dir: FSDirectory            = FSDirectory.open(Paths.get(ApplicationConfiguration.institutionsIndexDir))
  private val mainReader: DirectoryReader = DirectoryReader.open(dir)

  override def searchById(id: String): Try[Option[Institution]] = Try {
    val reader: DirectoryReader = getDirectoryReader(mainReader)
    val searcher: IndexSearcher = new IndexSearcher(reader)

    val query: TermQuery = new TermQuery(new Term(ID.value, id))
    val hits: TopDocs    = searcher.search(query, 1)

    val results: Option[Institution] =
      hits.scoreDocs.map(sc => DocumentConverter.to[Institution](searcher.doc(sc.doc))).find(_.id == id)

    results
  }

  override def searchByText(
    searchingField: String,
    searchText: String,
    page: Int,
    limit: Int
  ): Try[(List[Institution], Long)] = {
    val reader: DirectoryReader = getDirectoryReader(mainReader)
    val searcher: IndexSearcher = new IndexSearcher(reader)

    val documents: Try[(List[ScoreDoc], Long)] = {
      val search = searchFunc(reader, searcher, InstitutionTokenAnalyzer)
      search(searchingField, searchText, page, limit)
    }

    val results: Try[(List[Institution], Long)] = documents.map { case (scores, count) =>
      scores.map(sc => DocumentConverter.to[Institution](searcher.doc(sc.doc))) -> count
    }

    results
  }

  // TODO add pagination, low priority
  override def getAllItems(filters: Map[SearchField, String]): Try[List[Institution]] = Failure(
    new RuntimeException("Not implemented")
  )

}
