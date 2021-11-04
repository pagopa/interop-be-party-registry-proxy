package it.pagopa.pdnd.interop.uservice.partyregistryproxy.service.impl

import it.pagopa.pdnd.interop.uservice.partyregistryproxy.common.system.ApplicationConfiguration
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.model.Institution
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.service.SearchService
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.service.impl.util.DocumentConverter
import org.apache.lucene.index.{DirectoryReader, IndexWriter, Term}
import org.apache.lucene.search._
import org.apache.lucene.store.FSDirectory

import java.nio.file.Paths
import scala.util.Try

case object InstitutionSearchServiceImpl extends SearchService[Institution] {

  private val dir                 = FSDirectory.open(Paths.get(ApplicationConfiguration.institutionsIndexDir))
  private val writer: IndexWriter = new IndexWriter(dir, config)

  implicit private val reader: DirectoryReader = DirectoryReader.open(writer)
  implicit private val searcher: IndexSearcher = new IndexSearcher(reader)

  override def adds(items: List[Institution]): Try[Unit] = Try {
    items.foreach { item =>
      writer.updateDocument(new Term(InstitutionFields.ID, item.id), item.toDocument)
    }
  }

  override def searchById(id: String): Try[Option[Institution]] = Try {
    val query         = new TermQuery(new Term(InstitutionFields.ID, id))
    val hits: TopDocs = searcher.search(query, 1)

    val results = hits.scoreDocs.map(sc => DocumentConverter.to[Institution](searcher.doc(sc.doc))).find(_.id == id)
    reader.close()
    results
  }

  override def searchByText(
    searchText: String,
    searchingField: String,
    page: Int,
    limit: Int
  ): Try[(List[Institution], Long)] = {
    val results: Try[(List[ScoreDoc], Long)] =
      search(searchTxt = searchText, searchingField = searchingField, page = page, limit = limit)

    results.map { case (scores, count) =>
      scores.map(sc => DocumentConverter.to[Institution](searcher.doc(sc.doc))) -> count
    }
  }

  override def getAllItems: Try[List[Institution]] = ???

  def deleteAll(): Try[Long] = Try(writer.deleteAll())

  def commit(): Long = writer.commit()

}
