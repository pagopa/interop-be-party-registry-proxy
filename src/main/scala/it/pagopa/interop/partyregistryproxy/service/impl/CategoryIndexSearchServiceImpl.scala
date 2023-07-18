package it.pagopa.interop.partyregistryproxy.service.impl

import cats.syntax.all._
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
import scala.util.{Either, Try}
case object CategoryIndexSearchServiceImpl extends IndexSearchService[Category] {

  private val dir: FSDirectory            = FSDirectory.open(Paths.get(ApplicationConfiguration.categoriesIndexDir))
  private val mainReader: DirectoryReader = DirectoryReader.open(dir)

  override def searchById(id: String): Either[Throwable, Option[Category]] = {
    val unsafeDeps = Try {
      val reader: DirectoryReader = getDirectoryReader(mainReader)
      val searcher: IndexSearcher = new IndexSearcher(reader)

      val query: TermQuery = new TermQuery(new Term(ID.value, id))
      val hits: TopDocs    = searcher.search(query, 1)

      (hits, searcher)
    }.toEither

    unsafeDeps.flatMap { case (hits, searcher) =>
      hits.scoreDocs.toList
        .traverse(sc => DocumentConverter.to[Category](searcher.doc(sc.doc)))
        .map(_.headOption)

    }
  }

  override def searchByExternalId(origin: String, originId: String): Either[Throwable, Option[Category]] = {
    val unsafeDeps = Try {

      val reader: DirectoryReader = getDirectoryReader(mainReader)
      val searcher: IndexSearcher = new IndexSearcher(reader)

      val queryBuilder: BooleanQuery.Builder = new BooleanQuery.Builder
      queryBuilder.add(new TermQuery(new Term(ORIGIN.value, origin)), Occur.MUST)
      queryBuilder.add(new TermQuery(new Term(CODE.value, originId)), Occur.MUST)

      (searcher.search(queryBuilder.build(), 1), searcher)
    }.toEither

    unsafeDeps.flatMap { case (hits, searcher) =>
      hits.scoreDocs.toList
        .traverse(sc => DocumentConverter.to[Category](searcher.doc(sc.doc)))
        .map(_.find(ist => ist.origin == origin && ist.code == originId))

    }
  }

  override def searchByText(
    searchingField: String,
    searchText: String,
    page: Int,
    limit: Int
  ): Either[Throwable, (List[Category], Long)] = {
    val reader: DirectoryReader = getDirectoryReader(mainReader)
    val searcher: IndexSearcher = new IndexSearcher(reader)

    val documents: Either[Throwable, (List[ScoreDoc], Long)] = {
      val search = searchFunc(reader, searcher, CategoryTokenAnalyzer)
      search(searchingField, searchText, page, limit)
    }.toEither

    documents.flatMap { case (scores, count) =>
      scores
        .traverse(sc => DocumentConverter.to[Category](searcher.doc(sc.doc)))
        .map(categories => categories -> count)
    }

  }

  override def getAllItems(
    filters: Map[SearchField, String],
    page: Int,
    limit: Int
  ): Either[Throwable, (List[Category], Long)] =
    Try(getDirectoryReader(mainReader)).toEither.flatMap(getItems[Category](filters, page, limit))

}
