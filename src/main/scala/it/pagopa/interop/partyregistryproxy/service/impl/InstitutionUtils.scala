package it.pagopa.interop.partyregistryproxy.service.impl

import cats.syntax.all._
import it.pagopa.interop.partyregistryproxy.common.util.CategoryField.ORIGIN
import it.pagopa.interop.partyregistryproxy.common.util.InstitutionField.{ID, ORIGIN_ID}
import it.pagopa.interop.partyregistryproxy.common.util.SearchField
import it.pagopa.interop.partyregistryproxy.model.Institution
import it.pagopa.interop.partyregistryproxy.service.impl.analizer.InstitutionTokenAnalyzer
import it.pagopa.interop.partyregistryproxy.service.impl.util.DocumentConverter
import it.pagopa.interop.partyregistryproxy.service.impl.util.DocumentConverter._
import org.apache.lucene.index.{DirectoryReader, Term}
import org.apache.lucene.search.BooleanClause.Occur
import org.apache.lucene.search._

import scala.util.Try

object InstitutionUtils {

  def searchById(id: String)(mainReader: DirectoryReader): Either[Throwable, Option[Institution]] = {

    val unsafeDeps: Either[Throwable, (TopDocs, IndexSearcher)] = Try {
      val reader: DirectoryReader = getDirectoryReader(mainReader)
      val searcher: IndexSearcher = new IndexSearcher(reader)
      val query: TermQuery        = new TermQuery(new Term(ID.value, id))
      (searcher.search(query, 1), searcher)
    }.toEither

    unsafeDeps.flatMap { case (hits, searcher) =>
      hits.scoreDocs.toList
        .traverse(sc => DocumentConverter.to[Institution](searcher.doc(sc.doc)))
        .map(_.find(_.id == id))

    }
  }

  def searchByExternalId(origin: String, originId: String)(
    mainReader: DirectoryReader
  ): Either[Throwable, Option[Institution]] = {
    val unsafeDeps = Try {
      val reader: DirectoryReader = getDirectoryReader(mainReader)
      val searcher: IndexSearcher = new IndexSearcher(reader)

      val queryBuilder: BooleanQuery.Builder = new BooleanQuery.Builder
      queryBuilder.add(new TermQuery(new Term(ORIGIN.value, origin)), Occur.MUST)
      queryBuilder.add(new TermQuery(new Term(ORIGIN_ID.value, originId)), Occur.MUST)

      (searcher.search(queryBuilder.build(), 1), searcher)
    }.toEither

    unsafeDeps.flatMap { case (hits, searcher) =>
      hits.scoreDocs.toList
        .traverse(sc => DocumentConverter.to[Institution](searcher.doc(sc.doc)))
        .map(_.find(ist => ist.origin == origin && ist.originId == originId))

    }
  }

  def searchByText(searchingField: String, searchText: String, page: Int, limit: Int)(
    mainReader: DirectoryReader
  ): Either[Throwable, (List[Institution], Long)] = {
    val reader: DirectoryReader = getDirectoryReader(mainReader)
    val searcher: IndexSearcher = new IndexSearcher(reader)

    val documents: Either[Throwable, (List[ScoreDoc], Long)] = {
      val search = searchFunc(reader, searcher, InstitutionTokenAnalyzer)
      search(searchingField, searchText, page, limit)
    }.toEither

    documents.flatMap { case (scores, count) =>
      scores
        .traverse(sc => DocumentConverter.to[Institution](searcher.doc(sc.doc)))
        .map(institutions => institutions -> count)
    }

  }

  def getAllItems(filters: Map[SearchField, String], page: Int, limit: Int)(
    mainReader: DirectoryReader
  ): Either[Throwable, (List[Institution], Long)] =
    Try(getDirectoryReader(mainReader)).toEither.flatMap(getItems[Institution](filters, page, limit))
}
