package it.pagopa.pdnd.interop.uservice.partyregistryproxy.service.impl

import it.pagopa.pdnd.interop.uservice.partyregistryproxy.common.system.ApplicationConfiguration
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.model.Institution
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.service.IndexSearchService
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.service.impl.util.DocumentConverter
import org.apache.lucene.index.{DirectoryReader, Term}
import org.apache.lucene.search._
import org.apache.lucene.store.FSDirectory

import java.nio.file.Paths
import scala.util.{Failure, Try}

case object InstitutionIndexSearchServiceImpl extends IndexSearchService[Institution] {

  private val dir: FSDirectory            = FSDirectory.open(Paths.get(ApplicationConfiguration.institutionsIndexDir))
  private val mainReader: DirectoryReader = DirectoryReader.open(dir)

  override def searchById(id: String): Try[Option[Institution]] = Try {
    val reader: DirectoryReader = DirectoryReader.openIfChanged(mainReader)
    val searcher: IndexSearcher = new IndexSearcher(reader)

    val query         = new TermQuery(new Term(InstitutionFields.ID, id))
    val hits: TopDocs = searcher.search(query, 1)

    val results = hits.scoreDocs.map(sc => DocumentConverter.to[Institution](searcher.doc(sc.doc))).find(_.id == id)

    reader.close()

    results
  }

  override def searchByText(
    searchingField: String,
    searchText: String,
    page: Int,
    limit: Int
  ): Try[(List[Institution], Long)] = {
    val reader: DirectoryReader = DirectoryReader.openIfChanged(mainReader)
    val searcher: IndexSearcher = new IndexSearcher(reader)

    val documents: Try[(List[ScoreDoc], Long)] = {
      val search = searchFunc(reader, searcher)
      search(searchingField, searchText, page, limit)
    }

    val results: Try[(List[Institution], Long)] = documents.map { case (scores, count) =>
      scores.map(sc => DocumentConverter.to[Institution](searcher.doc(sc.doc))) -> count
    }

    reader.close()

    results
  }

  //TODO add pagination, low priority
  override def getAllItems: Try[List[Institution]] = Failure(new RuntimeException("Not implemented"))

}