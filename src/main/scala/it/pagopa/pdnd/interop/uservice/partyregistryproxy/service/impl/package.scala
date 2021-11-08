package it.pagopa.pdnd.interop.uservice.partyregistryproxy.service

import it.pagopa.pdnd.interop.uservice.partyregistryproxy.model.{Category, Institution}
import org.apache.lucene.document._
import org.apache.lucene.index.{DirectoryReader, Term}
import org.apache.lucene.search.BooleanClause.Occur
import org.apache.lucene.search._
import org.apache.lucene.search.suggest.Lookup
import org.apache.lucene.search.suggest.analyzing.AnalyzingSuggester
import org.apache.lucene.util.BytesRef

import javax.naming.directory.SearchResult
import scala.jdk.CollectionConverters._
import scala.util.Try

package object impl {

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

  def search(searchTxt: String, searchingField: String, page: Int, limit: Int)(
    reader: DirectoryReader,
    searcher: IndexSearcher
  ): Try[(List[ScoreDoc], Long)] =
    Try {

      val collector: TopScoreDocCollector = TopScoreDocCollector.create(reader.numDocs, Int.MaxValue)
      val startIndex: Int                 = (page - 1) * limit

      val builder1 = new BooleanQuery.Builder
      val builder2 = new PhraseQuery.Builder

      val words: List[(String, Int)] = searchTxt.split("\\s").toList.zipWithIndex

      @annotation.tailrec
      def helper(
        words: List[(String, Int)],
        acc: BooleanQuery.Builder,
        builder: PhraseQuery.Builder
      ): BooleanQuery.Builder = words match {
        case word :: Nil =>
          val query1      = builder.build()
          val query2      = new WildcardQuery(new Term(searchingField, s"$word*"))
          val newBuilder1 = acc.add(query1, Occur.MUST)
          val newBuilder2 = newBuilder1.add(query2, Occur.SHOULD)
          helper(Nil, newBuilder2, builder)
        case word :: xs =>
          val newBuilder = builder.add(new Term(searchingField, word._1), word._2)
          helper(xs, acc, newBuilder)
        case Nil =>
          acc

      }

      val qs = helper(words, builder1, builder2)

      val query: Query           = qs.build()
      val _                      = searcher.search(query, collector)
      val hits: TopDocs          = collector.topDocs(startIndex, limit)
      val scores: List[ScoreDoc] = hits.scoreDocs.toList

      val results = scores
      (results, hits.totalHits.value)
    }

  def suggestTermsFor(term: String)(analyzingSuggester: AnalyzingSuggester): List[String] = {
    val lookup: List[Lookup.LookupResult] = analyzingSuggester.lookup(term, false, 10).asScala.toList
    val suggestions                       = lookup.map(l => l.key.toString)
    suggestions
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
