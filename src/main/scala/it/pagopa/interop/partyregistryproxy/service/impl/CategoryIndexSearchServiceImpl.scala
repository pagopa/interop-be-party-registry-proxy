package it.pagopa.interop.partyregistryproxy.service.impl

import it.pagopa.interop.partyregistryproxy.common.system.ApplicationConfiguration
import it.pagopa.interop.partyregistryproxy.common.util.CategoryField.{CODE, ID, ORIGIN}
import it.pagopa.interop.partyregistryproxy.common.util.SearchField
import it.pagopa.interop.partyregistryproxy.model.Category
import it.pagopa.interop.partyregistryproxy.service.IndexSearchService
import it.pagopa.interop.partyregistryproxy.service.impl.analizer.CategoryTokenAnalyzer
import it.pagopa.interop.partyregistryproxy.service.impl.util.DocumentConverter
import org.apache.lucene.index.{DirectoryReader, Term}
import org.apache.lucene.search.BooleanClause.Occur
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

    val query: TermQuery = new TermQuery(new Term(ID.value, id))
    val hits: TopDocs    = searcher.search(query, 1)

    val results: Option[Category] =
      hits.scoreDocs.map(sc => DocumentConverter.to[Category](searcher.doc(sc.doc))).headOption

    results
  }

  override def searchByExternalId(origin: String, originId: String): Try[Option[Category]] = Try {

    val reader: DirectoryReader = getDirectoryReader(mainReader)
    val searcher: IndexSearcher = new IndexSearcher(reader)

    val queryBuilder: BooleanQuery.Builder = new BooleanQuery.Builder
    queryBuilder.add(new TermQuery(new Term(ORIGIN.value, origin)), Occur.MUST)
    queryBuilder.add(new TermQuery(new Term(CODE.value, originId)), Occur.MUST)

    val hits: TopDocs = searcher.search(queryBuilder.build(), 1)

    val results: Option[Category] =
      hits.scoreDocs
        .map(sc => DocumentConverter.to[Category](searcher.doc(sc.doc)))
        .find(ist => ist.origin == origin && ist.code == originId)

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
      val search = searchFunc(reader, searcher, CategoryTokenAnalyzer)
      search(searchingField, searchText, page, limit)
    }

    val results: Try[(List[Category], Long)] = documents.map { case (scores, count) =>
      scores.map(sc => DocumentConverter.to[Category](searcher.doc(sc.doc))) -> count
    }

    results
  }

  override def getAllItems(filters: Map[SearchField, String], page: Int, limit: Int): Try[(List[Category], Long)] =
    Try(getDirectoryReader(mainReader)).flatMap(getItems[Category](filters, page, limit))

}
