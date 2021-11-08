package it.pagopa.pdnd.interop.uservice.partyregistryproxy.service.impl

import it.pagopa.pdnd.interop.uservice.partyregistryproxy.model.Category
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.service.SearchService
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.service.impl.util.DocumentConverter
import org.apache.lucene.analysis.it.ItalianAnalyzer
import org.apache.lucene.index.{DirectoryReader, IndexWriter, IndexWriterConfig, Term}
import org.apache.lucene.search._
import org.apache.lucene.store.ByteBuffersDirectory

import scala.util.Try

case object CategorySearchServiceImpl extends SearchService[Category] {

//  private val dir: FSDirectory                   = FSDirectory.open(Paths.get(ApplicationConfiguration.categoriesIndexDir))
  private val dir: ByteBuffersDirectory          = new ByteBuffersDirectory()
  implicit private val analyzer: ItalianAnalyzer = new ItalianAnalyzer()

  private val config: IndexWriterConfig = new IndexWriterConfig(analyzer)
  private val writer: IndexWriter       = new IndexWriter(dir, config)

  override def adds(items: List[Category]): Try[Unit] = Try {
    items.foreach { item =>
      writer.updateDocument(new Term(CategoryFields.CODE, item.code), item.toDocument)
    }
    val _ = writer.commit()
  }

  override def searchById(id: String): Try[Option[Category]] = Try {
    val reader: DirectoryReader = DirectoryReader.open(writer)
    val searcher: IndexSearcher = new IndexSearcher(reader)
    val query: TermQuery        = new TermQuery(new Term(CategoryFields.CODE, id))
    val hits: TopDocs           = searcher.search(query, 1)

    val results: Option[Category] =
      hits.scoreDocs.map(sc => DocumentConverter.to[Category](searcher.doc(sc.doc))).find(_.code == id)
    results
  }

  override def searchByText(
    searchText: String,
    searchingField: String,
    page: Int,
    limit: Int
  ): Try[(List[Category], Long)] = {
    val reader: DirectoryReader = DirectoryReader.open(writer)
    val searcher: IndexSearcher = new IndexSearcher(reader)
    val results: Try[(List[ScoreDoc], Long)] =
      search(searchTxt = searchText, searchingField = searchingField, page = page, limit = limit)(reader, searcher)

    results.map { case (scores, count) =>
      scores.map(sc => DocumentConverter.to[Category](searcher.doc(sc.doc))) -> count
    }
  }

  override def getAllItems: Try[List[Category]] = Try {
    val reader: DirectoryReader  = DirectoryReader.open(writer)
    val searcher: IndexSearcher  = new IndexSearcher(reader)
    val query: MatchAllDocsQuery = new MatchAllDocsQuery
    val hits: TopDocs            = searcher.search(query, reader.numDocs)

    val results: List[Category] = hits.scoreDocs.map(sc => DocumentConverter.to[Category](searcher.doc(sc.doc))).toList

    results
  }

  def deleteAll(): Try[Long] = Try(writer.deleteAll())

  def commit(): Long = writer.commit()

}
