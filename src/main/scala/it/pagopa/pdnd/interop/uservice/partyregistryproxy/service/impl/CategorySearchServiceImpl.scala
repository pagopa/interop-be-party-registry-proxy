package it.pagopa.pdnd.interop.uservice.partyregistryproxy.service.impl

import it.pagopa.pdnd.interop.uservice.partyregistryproxy.common.system.ApplicationConfiguration
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.model.Category
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.service.SearchService
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.service.impl.util.DocumentConverter
import org.apache.lucene.index.{DirectoryReader, IndexWriter, Term}
import org.apache.lucene.search._
import org.apache.lucene.store.FSDirectory

import java.nio.file.Paths
import scala.util.Try

case object CategorySearchServiceImpl extends SearchService[Category] {

  private val dir                 = FSDirectory.open(Paths.get(ApplicationConfiguration.categoriesIndexDir))
  private val writer: IndexWriter = new IndexWriter(dir, config)

  implicit private val reader: DirectoryReader = DirectoryReader.open(writer)
  implicit private val searcher: IndexSearcher = new IndexSearcher(reader)

  override def adds(items: List[Category]): Try[Unit] = Try {
    items.foreach { item =>
      writer.updateDocument(new Term(CategoryFields.CODE, item.code), item.toDocument)
    }
  }

  override def searchById(id: String): Try[Option[Category]] = Try {
    val query         = new TermQuery(new Term(CategoryFields.CODE, id))
    val hits: TopDocs = searcher.search(query, 1)

    val results = hits.scoreDocs.map(sc => DocumentConverter.to[Category](searcher.doc(sc.doc))).find(_.code == id)
    reader.close()
    results
  }

  override def searchByText(
    searchText: String,
    searchingField: String,
    page: Int,
    limit: Int
  ): Try[(List[Category], Long)] = {
    val results: Try[(List[ScoreDoc], Long)] =
      search(searchTxt = searchText, searchingField = searchingField, page = page, limit = limit)

    results.map { case (scores, count) =>
      scores.map(sc => DocumentConverter.to[Category](searcher.doc(sc.doc))) -> count
    }
  }

  override def getAllItems: Try[List[Category]] = ???

  def deleteAll(): Try[Long] = Try(writer.deleteAll())

  def commit(): Long = writer.commit()

}
