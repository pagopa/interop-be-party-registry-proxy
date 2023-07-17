package it.pagopa.interop.partyregistryproxy.service

import cats.syntax.all._
import it.pagopa.interop.partyregistryproxy.common.util.{CategoryField, InstitutionField, SearchField, createCategoryId}
import it.pagopa.interop.partyregistryproxy.model.{Category, Institution}
import it.pagopa.interop.partyregistryproxy.service.impl.util.DocumentConverter
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.document._
import org.apache.lucene.index.{DirectoryReader, Term}
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.search._
import org.apache.lucene.util.BytesRef

import scala.util.Try

package object impl {

  def getItems[A](filters: Map[SearchField, String], page: Int, limit: Int)(
    reader: DirectoryReader
  )(implicit documentConverter: DocumentConverter[A]): Either[Throwable, (List[A], Long)] = {
    val unsafeDeps: Either[Throwable, (TopDocs, IndexSearcher)] = Try {
      val searcher: IndexSearcher = new IndexSearcher(reader)
      val query: Query            = getQuery(filters)

      val collector: TopScoreDocCollector = TopScoreDocCollector.create(reader.numDocs(), reader.numDocs())

      val startIndex = (page - 1) * limit

      searcher.search(query, collector)

      (collector.topDocs(startIndex, limit), searcher)
    }.toEither

    unsafeDeps.flatMap { case (hits, searcher) =>
      hits.scoreDocs.toList
        .traverse(sc => DocumentConverter.to[A](searcher.doc(sc.doc)))
        .map(items =>
          items -> reader
            .numDocs()
            .toLong
        )
    }

  }

  private def getQuery(filters: Map[SearchField, String]): Query = {
    if (filters.isEmpty)
      new MatchAllDocsQuery
    else {
      val booleanQuery = new BooleanQuery.Builder()
      filters.foreach { case (k, v) =>
        val term: Term       = new Term(k.value, v)
        val query: TermQuery = new TermQuery(term)
        booleanQuery.add(query, BooleanClause.Occur.MUST)
      }

      booleanQuery.build()
    }
  }

  def getDirectoryReader(reader: DirectoryReader): DirectoryReader =
    Option(DirectoryReader.openIfChanged(reader)).getOrElse(reader)

  def searchFunc(
    reader: DirectoryReader,
    searcher: IndexSearcher,
    analyzer: Analyzer
  ): (String, String, Int, Int) => Try[(List[ScoreDoc], Long)] = (searchingField, searchTxt, page, limit) =>
    Try {

      val collector: TopScoreDocCollector = TopScoreDocCollector.create(reader.numDocs, Int.MaxValue)
      val startIndex: Int                 = (page - 1) * limit
      val parser: QueryParser             = new QueryParser(searchingField, analyzer)
      parser.setPhraseSlop(4)
      val query: Query                    = parser.parse(searchTxt)
      val _                               = searcher.search(query, collector)
      val hits: TopDocs                   = collector.topDocs(startIndex, limit)
      val scores: List[ScoreDoc]          = hits.scoreDocs.toList

      (scores, hits.totalHits.value)
    }

  implicit class DocumentOps(val document: Document) extends AnyVal {
    def addOptional(fieldName: String, opt: Option[String]): Unit =
      opt.foreach(value => document.add(new StoredField(fieldName, value)))
  }

  implicit class InstitutionOps(val institution: Institution) extends AnyVal {
    def toDocument: Document = {
      val doc = new Document
      doc.add(new StringField(InstitutionField.ID.value, institution.id, Field.Store.YES))
      doc.add(new StringField(InstitutionField.ORIGIN.value, institution.origin, Field.Store.YES))
      doc.add(new StringField(InstitutionField.ORIGIN_ID.value, institution.originId, Field.Store.YES))
      doc.add(new SortedDocValuesField(InstitutionField.DESCRIPTION.value, new BytesRef(institution.description)))
      doc.add(new TextField(InstitutionField.DESCRIPTION.value, institution.description, Field.Store.YES))
      doc.add(new TextField(InstitutionField.TAX_CODE.value, institution.taxCode, Field.Store.YES))
      doc.add(new TextField(InstitutionField.CATEGORY.value, institution.category, Field.Store.YES))
      doc.add(new TextField(InstitutionField.DIGITAL_ADDRESS.value, institution.digitalAddress, Field.Store.YES))
      doc.add(new TextField(InstitutionField.ADDRESS.value, institution.address, Field.Store.YES))
      doc.add(new TextField(InstitutionField.ZIP_CODE.value, institution.zipCode, Field.Store.YES))
      doc.add(new TextField(InstitutionField.KIND.value, institution.kind, Field.Store.YES))
      doc.add(
        new TextField(InstitutionField.CLASSIFICATION.value, institution.classification.toString, Field.Store.YES)
      )

      doc
    }
  }

  implicit class CategoryOps(val category: Category) extends AnyVal {
    def toDocument: Document = {
      val doc = new Document
      val id  = createCategoryId(code = category.code, origin = category.origin)
      doc.add(new StringField(CategoryField.ID.value, id, Field.Store.NO))
      doc.add(new StringField(CategoryField.CODE.value, category.code, Field.Store.YES))
      doc.add(new StringField(CategoryField.ORIGIN.value, category.origin, Field.Store.YES))
      doc.add(new TextField(CategoryField.NAME.value, category.name, Field.Store.YES))
      doc.add(new TextField(CategoryField.KIND.value, category.kind, Field.Store.YES))
      doc
    }
  }

}
