package it.pagopa.pdnd.interop.uservice.partyregistryproxy.service

import it.pagopa.pdnd.interop.uservice.partyregistryproxy.model.{Category, Institution}
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.document._
import org.apache.lucene.index.{DirectoryReader, IndexWriterConfig}
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.search._

import javax.naming.directory.SearchResult
import scala.util.Try

package object impl {

  val analyzer: StandardAnalyzer = new StandardAnalyzer()

  val config: IndexWriterConfig = new IndexWriterConfig(analyzer)

  implicit class SearchResultOps(val result: SearchResult) extends AnyVal {
    def extract(attributeName: String): Option[String] = {
      Option(result.getAttributes.get(attributeName)).map(_.get().asInstanceOf[String])

    }
  }

  object InstitutionFields {
    final val ID              = "id"
    final val O               = "o"
    final val OU              = "ou"
    final val AOO             = "aoo"
    final val FISCAL_CODE     = "fiscalCode"
    final val CATEGORY        = "category"
    final val MANAGER_NAME    = "managerName"
    final val MANAGER_SURNAME = "managerSurname"
    final val DESCRIPTION     = "description"
    final val DIGITAL_ADDRESS = "digitalAddress"
  }

  object CategoryFields {
    final val CODE = "code"
    final val NAME = "name"
    final val KIND = "kind"
  }

  def search(searchTxt: String, searchingField: String, page: Int, limit: Int)(implicit
    reader: DirectoryReader,
    searcher: IndexSearcher
  ): Try[(List[ScoreDoc], Long)] =
    Try {
      val collector: TopScoreDocCollector = TopScoreDocCollector.create(reader.numDocs, Int.MaxValue)
      val startIndex: Int                 = (page - 1) * limit
      val parser: QueryParser             = new QueryParser(searchingField, analyzer)
      val query: Query                    = parser.parse(searchTxt)
      val _                               = searcher.search(query, collector)
      val hits: TopDocs                   = collector.topDocs(startIndex, limit)
      val scores: List[ScoreDoc]          = hits.scoreDocs.toList

      val results = scores
      reader.close()
      (results, hits.totalHits.value)
    }

  implicit class DocumentOps(val document: Document) extends AnyVal {
    def addOptional(fieldName: String, opt: Option[String]): Unit =
      opt.foreach(value => document.add(new StoredField(fieldName, value)))
  }

  implicit class InstitutionOps(val institution: Institution) extends AnyVal {
    def toDocument: Document = {
      val doc = new Document
      doc.add(new StringField(InstitutionFields.ID, institution.id, Field.Store.YES))
      doc.add(new TextField(InstitutionFields.DESCRIPTION, institution.description, Field.Store.YES))
      doc.add(new TextField(InstitutionFields.FISCAL_CODE, institution.fiscalCode, Field.Store.YES))
      doc.add(new TextField(InstitutionFields.CATEGORY, institution.category, Field.Store.YES))
      doc.add(new TextField(InstitutionFields.DIGITAL_ADDRESS, institution.digitalAddress, Field.Store.YES))

      doc.addOptional(InstitutionFields.O, institution.o)
      doc.addOptional(InstitutionFields.OU, institution.ou)
      doc.addOptional(InstitutionFields.AOO, institution.aoo)
      doc.addOptional(InstitutionFields.MANAGER_NAME, institution.managerName)
      doc.addOptional(InstitutionFields.MANAGER_SURNAME, institution.managerSurname)

      doc
    }
  }

  implicit class CategoryOps(val category: Category) extends AnyVal {
    def toDocument: Document = {
      val doc = new Document
      doc.add(new StringField(CategoryFields.CODE, category.code, Field.Store.YES))
      doc.add(new TextField(CategoryFields.NAME, category.name, Field.Store.YES))
      doc.add(new TextField(CategoryFields.KIND, category.kind, Field.Store.YES))
      doc
    }
  }

}
