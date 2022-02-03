package it.pagopa.pdnd.interop.uservice.partyregistryproxy.service.impl

import it.pagopa.pdnd.interop.uservice.partyregistryproxy.common.system.ApplicationConfiguration
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.model.Category
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.service.IndexSearchService
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.service.impl.util.DocumentConverter
import org.apache.lucene.index.{DirectoryReader, Term}
import org.apache.lucene.search._
import org.apache.lucene.store.FSDirectory

import java.nio.file.Paths
import scala.util.Try

case object CategoryIndexSearchServiceImpl extends IndexSearchService[Category] {

  private val dir: FSDirectory            = FSDirectory.open(Paths.get(ApplicationConfiguration.categoriesIndexDir))
  private val mainReader: DirectoryReader = DirectoryReader.open(dir)

  override def searchById(id: String): Try[Option[Category]] = Try {
    val reader: DirectoryReader = getDirectoryReader(mainReader)
    val searcher: IndexSearcher = new IndexSearcher(reader)

    val query: TermQuery = new TermQuery(new Term(CategoryFields.ID, id))
    val hits: TopDocs    = searcher.search(query, 1)

    val results: Option[Category] =
      hits.scoreDocs.map(sc => DocumentConverter.to[Category](searcher.doc(sc.doc))).headOption

    results
  }

  override def searchByText(
    searchingField: String,
    searchText: String,
    page: Int,
    limit: Int
  ): Try[(List[Category], Long)] = {
    val reader: DirectoryReader = getDirectoryReader(mainReader)
    val searcher: IndexSearcher = new IndexSearcher(reader)

    val documents: Try[(List[ScoreDoc], Long)] = {
      val search = searchFunc(reader, searcher)
      search(searchingField, searchText, page, limit)
    }

    val results: Try[(List[Category], Long)] = documents.map { case (scores, count) =>
      scores.map(sc => DocumentConverter.to[Category](searcher.doc(sc.doc))) -> count
    }

    results
  }

  override def getAllItems: Try[List[Category]] = Try {
    val reader: DirectoryReader = getDirectoryReader(mainReader)
    val searcher: IndexSearcher = new IndexSearcher(reader)

    val query: MatchAllDocsQuery = new MatchAllDocsQuery
    val hits: TopDocs            = searcher.search(query, reader.numDocs)

    val results: List[Category] = hits.scoreDocs.map(sc => DocumentConverter.to[Category](searcher.doc(sc.doc))).toList

    results
  }

}
