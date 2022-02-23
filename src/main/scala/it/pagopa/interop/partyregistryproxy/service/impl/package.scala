package it.pagopa.interop.partyregistryproxy.service

import it.pagopa.interop.partyregistryproxy.common.system.ApplicationConfiguration
import it.pagopa.interop.partyregistryproxy.common.util.{CategoryField, InstitutionField, createCategoryId}
import it.pagopa.interop.partyregistryproxy.model.{Category, Institution}
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.document._
import org.apache.lucene.index.{DirectoryReader, IndexWriter}
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.search._
import org.apache.lucene.store.LockObtainFailedException
import org.apache.lucene.util.BytesRef

import javax.naming.directory.SearchResult
import scala.util.{Failure, Success, Try}

package object impl {

  def useWriter[A](writer: Try[IndexWriter], f: IndexWriter => Try[A], zero: A): Try[A] = writer match {
    case Success(wr)                           => f(wr)
    case Failure(_: LockObtainFailedException) => Success(zero)
    case Failure(ex)                           => Failure(ex)
  }

  def commitAndClose(writer: IndexWriter): Try[Unit] = Try {
    writer.commit()
    writer.close()
  }

  def getDirectoryReader(reader: DirectoryReader): DirectoryReader = {
    Option(DirectoryReader.openIfChanged(reader)).getOrElse(reader)
  }

  implicit class SearchResultOps(val result: SearchResult) extends AnyVal {
    def extract(attributeName: String): Option[String] = {
      Option(result.getAttributes.get(attributeName)).map(_.get().asInstanceOf[String])

    }
  }

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
      val query: Query           = parser.parse(searchTxt)
      val _                      = searcher.search(query, collector)
      val hits: TopDocs          = collector.topDocs(startIndex, limit)
      val scores: List[ScoreDoc] = hits.scoreDocs.toList

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
      doc.add(new SortedDocValuesField(InstitutionField.DESCRIPTION.value, new BytesRef(institution.description)))
      doc.add(new TextField(InstitutionField.DESCRIPTION.value, institution.description, Field.Store.YES))
      doc.add(new TextField(InstitutionField.TAX_CODE.value, institution.taxCode, Field.Store.YES))
      doc.add(new TextField(InstitutionField.CATEGORY.value, institution.category, Field.Store.YES))
      doc.add(new TextField(InstitutionField.DIGITAL_ADDRESS.value, institution.digitalAddress, Field.Store.YES))
      doc.add(new TextField(InstitutionField.ADDRESS.value, institution.address, Field.Store.YES))
      doc.add(new TextField(InstitutionField.ZIP_CODE.value, institution.zipCode, Field.Store.YES))
      doc.add(new TextField(InstitutionField.ORIGIN.value, ApplicationConfiguration.ipaOrigin, Field.Store.YES))

      doc.addOptional(InstitutionField.O.value, institution.o)
      doc.addOptional(InstitutionField.OU.value, institution.ou)
      doc.addOptional(InstitutionField.AOO.value, institution.aoo)

      doc
    }
  }

  implicit class CategoryOps(val category: Category) extends AnyVal {
    def toDocument: Document = {
      val doc = new Document
      val id  = createCategoryId(code = category.code, origin = category.origin)
      doc.add(new StringField(CategoryField.ID.value, id, Field.Store.NO))
      doc.add(new TextField(CategoryField.CODE.value, category.code, Field.Store.YES))
      doc.add(new TextField(CategoryField.ORIGIN.value, category.origin, Field.Store.YES))
      doc.add(new TextField(CategoryField.NAME.value, category.name, Field.Store.YES))
      doc.add(new TextField(CategoryField.KIND.value, category.kind, Field.Store.YES))
      doc
    }
  }

}
