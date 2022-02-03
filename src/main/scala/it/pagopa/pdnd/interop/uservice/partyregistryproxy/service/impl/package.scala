package it.pagopa.pdnd.interop.uservice.partyregistryproxy.service

import it.pagopa.pdnd.interop.uservice.partyregistryproxy.common.system.ApplicationConfiguration
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.common.util.createCategoryId
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.model.{Category, Institution}
import org.apache.lucene.analysis.it.ItalianAnalyzer
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilter
import org.apache.lucene.analysis.ngram.NGramTokenFilter
import org.apache.lucene.analysis.standard.StandardTokenizer
import org.apache.lucene.analysis.{Analyzer, LowerCaseFilter, StopFilter}
import org.apache.lucene.document._
import org.apache.lucene.index.{DirectoryReader, IndexWriter}
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.search._
import org.apache.lucene.store.LockObtainFailedException
import org.apache.lucene.util.BytesRef

import javax.naming.directory.SearchResult
import scala.util.{Failure, Success, Try}

package object impl {

  case object NGramTokenAnalyzer extends Analyzer {

    override def createComponents(fieldName: String): Analyzer.TokenStreamComponents = {
      val source: StandardTokenizer    = new StandardTokenizer()
      val lowerFilter: LowerCaseFilter = new LowerCaseFilter(source)
      val stopFilter: StopFilter       = new StopFilter(lowerFilter, ItalianAnalyzer.getDefaultStopSet)
      val ascii: ASCIIFoldingFilter    = new ASCIIFoldingFilter(stopFilter)
      val ngram: NGramTokenFilter      = new NGramTokenFilter(ascii, 3, 5, true)
      new Analyzer.TokenStreamComponents(source, ngram)
    }
  }

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

  object InstitutionFields {
    final val ID                  = "id"
    final val O                   = "o"
    final val OU                  = "ou"
    final val AOO                 = "aoo"
    final val TAX_CODE            = "taxCode"
    final val CATEGORY            = "category"
    final val MANAGER_GIVEN_NAME  = "managerGivenName"
    final val MANAGER_FAMILY_NAME = "managerFamilyName"
    final val DESCRIPTION         = "description"
    final val DIGITAL_ADDRESS     = "digitalAddress"
    final val ORIGIN              = "origin"
  }

  object CategoryFields {
    final val ID     = "id"
    final val CODE   = "code"
    final val NAME   = "name"
    final val KIND   = "kind"
    final val ORIGIN = "origin"
  }

  def searchFunc(
    reader: DirectoryReader,
    searcher: IndexSearcher
  ): (String, String, Int, Int) => Try[(List[ScoreDoc], Long)] = (searchingField, searchTxt, page, limit) =>
    Try {

      val collector: TopScoreDocCollector = TopScoreDocCollector.create(reader.numDocs, Int.MaxValue)
      val startIndex: Int                 = (page - 1) * limit
      val parser: QueryParser             = new QueryParser(searchingField, NGramTokenAnalyzer)
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
      doc.add(new StringField(InstitutionFields.ID, institution.id, Field.Store.YES))
      doc.add(new SortedDocValuesField(InstitutionFields.DESCRIPTION, new BytesRef(institution.description)))
      doc.add(new TextField(InstitutionFields.DESCRIPTION, institution.description, Field.Store.YES))
      doc.add(new TextField(InstitutionFields.TAX_CODE, institution.taxCode, Field.Store.YES))
      doc.add(new TextField(InstitutionFields.CATEGORY, institution.category, Field.Store.YES))
      doc.add(new TextField(InstitutionFields.DIGITAL_ADDRESS, institution.digitalAddress, Field.Store.YES))
      doc.add(new TextField(InstitutionFields.MANAGER_GIVEN_NAME, institution.manager.givenName, Field.Store.YES))
      doc.add(new TextField(InstitutionFields.MANAGER_FAMILY_NAME, institution.manager.familyName, Field.Store.YES))
      doc.add(new TextField(InstitutionFields.ORIGIN, ApplicationConfiguration.ipaOrigin, Field.Store.YES))

      doc.addOptional(InstitutionFields.O, institution.o)
      doc.addOptional(InstitutionFields.OU, institution.ou)
      doc.addOptional(InstitutionFields.AOO, institution.aoo)

      doc
    }
  }

  implicit class CategoryOps(val category: Category) extends AnyVal {
    def toDocument: Document = {
      val doc = new Document
      val id  = createCategoryId(code = category.code, origin = category.origin)
      doc.add(new StringField(CategoryFields.ID, id, Field.Store.NO))
      doc.add(new TextField(CategoryFields.CODE, category.code, Field.Store.YES))
      doc.add(new TextField(CategoryFields.ORIGIN, category.origin, Field.Store.YES))
      doc.add(new TextField(CategoryFields.NAME, category.name, Field.Store.YES))
      doc.add(new TextField(CategoryFields.KIND, category.kind, Field.Store.YES))
      doc.add(new TextField(CategoryFields.ORIGIN, category.origin, Field.Store.YES))
      doc
    }
  }

}
