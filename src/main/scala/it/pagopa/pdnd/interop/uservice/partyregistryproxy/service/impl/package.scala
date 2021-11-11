package it.pagopa.pdnd.interop.uservice.partyregistryproxy.service

import it.pagopa.pdnd.interop.uservice.partyregistryproxy.model.{Category, Institution}
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.document._
import org.apache.lucene.index.DirectoryReader
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.search._
import org.apache.lucene.util.BytesRef

import javax.naming.directory.SearchResult
import scala.util.Try

package object impl {

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
  }

  object CategoryFields {
    final val CODE = "code"
    final val NAME = "name"
    final val KIND = "kind"
  }

  def searchFunc(
    reader: DirectoryReader,
    analyzer: StandardAnalyzer,
    searcher: IndexSearcher
  ): (String, String, Int, Int) => Try[(List[ScoreDoc], Long)] = (searchingField, searchTxt, page, limit) =>
    Try {

      val collector: TopScoreDocCollector = TopScoreDocCollector.create(reader.numDocs, Int.MaxValue)
      val startIndex: Int                 = (page - 1) * limit
      val parser: QueryParser             = new QueryParser(searchingField, analyzer)
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
      doc.add(new StringField(InstitutionFields.ID, institution.id, Field.Store.YES))
      doc.add(new SortedDocValuesField(InstitutionFields.DESCRIPTION, new BytesRef(institution.description)))
      doc.add(new TextField(InstitutionFields.DESCRIPTION, institution.description, Field.Store.YES))
      doc.add(new TextField(InstitutionFields.TAX_CODE, institution.taxCode, Field.Store.YES))
      doc.add(new TextField(InstitutionFields.CATEGORY, institution.category, Field.Store.YES))
      doc.add(new TextField(InstitutionFields.DIGITAL_ADDRESS, institution.digitalAddress, Field.Store.YES))
      doc.add(new TextField(InstitutionFields.MANAGER_GIVEN_NAME, institution.manager.givenName, Field.Store.YES))
      doc.add(new TextField(InstitutionFields.MANAGER_FAMILY_NAME, institution.manager.familyName, Field.Store.YES))

      doc.addOptional(InstitutionFields.O, institution.o)
      doc.addOptional(InstitutionFields.OU, institution.ou)
      doc.addOptional(InstitutionFields.AOO, institution.aoo)

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
