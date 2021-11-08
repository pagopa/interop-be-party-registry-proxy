package it.pagopa.pdnd.interop.uservice.partyregistryproxy.service.impl

import it.pagopa.pdnd.interop.uservice.partyregistryproxy.model.Institution
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.service.SearchService
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.service.impl.util.DocumentConverter
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.index.{DirectoryReader, IndexWriter, IndexWriterConfig, Term}
import org.apache.lucene.search._
import org.apache.lucene.store.ByteBuffersDirectory

import scala.util.Try

case object InstitutionSearchServiceImpl extends SearchService[Institution] {
  private val dir: ByteBuffersDirectory           = new ByteBuffersDirectory()
  implicit private val analyzer: StandardAnalyzer = new StandardAnalyzer()
//  implicit private val analyzer: ItalianAnalyzer = new ItalianAnalyzer()

  private val config: IndexWriterConfig = new IndexWriterConfig(analyzer)
  private val writer: IndexWriter       = new IndexWriter(dir, config)

  override def adds(items: List[Institution]): Try[Unit] = Try {

    items.foreach { item =>
      writer.updateDocument(new Term(InstitutionFields.ID, item.id), item.toDocument)
    }

  }

  override def searchById(id: String): Try[Option[Institution]] = Try {
    val reader: DirectoryReader = DirectoryReader.open(writer)
    val searcher: IndexSearcher = new IndexSearcher(reader)
    val query                   = new TermQuery(new Term(InstitutionFields.ID, id))
    val hits: TopDocs           = searcher.search(query, 1)

    val results = hits.scoreDocs.map(sc => DocumentConverter.to[Institution](searcher.doc(sc.doc))).find(_.id == id)
    results
  }

  override def searchByText(
    searchText: String,
    searchingField: String,
    page: Int,
    limit: Int
  ): Try[(List[Institution], Long)] = {
    val reader: DirectoryReader = DirectoryReader.open(writer)
    val searcher: IndexSearcher = new IndexSearcher(reader)
    val results: Try[(List[ScoreDoc], Long)] = {

      search(searchTxt = searchText, searchingField = searchingField, page = page, limit = limit)(reader, searcher)
    }

    results.map { case (scores, count) =>
      scores.map(sc => DocumentConverter.to[Institution](searcher.doc(sc.doc))) -> count
    }
  }

  override def getAllItems: Try[List[Institution]] = ???

  def deleteAll(): Try[Long] = Try(writer.deleteAll())

  def commit(): Long = {
    writer.commit()
  }

}
