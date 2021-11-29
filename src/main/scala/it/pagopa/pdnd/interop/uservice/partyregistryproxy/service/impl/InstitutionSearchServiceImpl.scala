package it.pagopa.pdnd.interop.uservice.partyregistryproxy.service.impl

import it.pagopa.pdnd.interop.uservice.partyregistryproxy.model.Institution
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.service.SearchService
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.service.impl.util.DocumentConverter
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.index.{DirectoryReader, IndexWriter, IndexWriterConfig, Term}
import org.apache.lucene.search._
import org.apache.lucene.store.ByteBuffersDirectory

import scala.util.{Failure, Try}

case object InstitutionSearchServiceImpl extends SearchService[Institution] {

  private val dir: ByteBuffersDirectory  = new ByteBuffersDirectory()
  private val analyzer: StandardAnalyzer = new StandardAnalyzer()
  private val config: IndexWriterConfig  = new IndexWriterConfig(analyzer)
  private val writer: IndexWriter        = new IndexWriter(dir, config)

  override def adds(items: List[Institution]): Try[Unit] = Try {

    items.foreach { item =>
      writer.updateDocument(new Term(InstitutionFields.ID, item.id), item.toDocument)
    }

  }

  override def searchById(id: String): Try[Option[Institution]] = Try {
    val reader: DirectoryReader = DirectoryReader.open(writer)
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
    val reader: DirectoryReader = DirectoryReader.open(writer)
    val searcher: IndexSearcher = new IndexSearcher(reader)

    val documents: Try[(List[ScoreDoc], Long)] = {
      val search = searchFunc(reader, analyzer, searcher)
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

  def deleteAll(): Try[Long] = Try(writer.deleteAll())

  def commit(): Long = {
    writer.commit()
  }

}
