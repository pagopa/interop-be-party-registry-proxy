package it.pagopa.pdnd.interop.uservice.partyregistryproxy.service.impl

import it.pagopa.pdnd.interop.uservice.partyregistryproxy.model.Institution
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.service.SearchService
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.index.{DirectoryReader, IndexWriter, IndexWriterConfig}
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.search.{IndexSearcher, Query, ScoreDoc, TopDocs, TopScoreDocCollector}
import org.apache.lucene.store.FSDirectory

import java.nio.file.Paths
import scala.jdk.CollectionConverters.IterableHasAsJava
import scala.util.Try
@SuppressWarnings(Array("org.wartremover.warts.NonUnitStatements"))
final case class SearchServiceImpl(pathDir: String) extends SearchService {

  private val dir      = FSDirectory.open(Paths.get(pathDir))
  private val analyzer = new StandardAnalyzer()
  private val config   = new IndexWriterConfig(analyzer)
  private val writer   = new IndexWriter(dir, config)

  def add(institution: Institution): Try[Long] = Try(writer.addDocument(institution.toDocument))

  def delete(institutionId: String): Try[Long] = Try {
    val parser: QueryParser = new QueryParser(InstitutionFields.ID, analyzer)
    val query: Query        = parser.parse(institutionId)
    writer.deleteDocuments(query)
  }

  def adds(institutions: Iterator[Institution]): Try[Long] = Try {
    writer.addDocuments(institutions.toList.map(_.toDocument).asJava)
  }

  def deleteAll(): Try[Long] = Try(writer.deleteAll())

  def commit(): Long = writer.commit()

  def close(): Unit = writer.close()

  def searchByDescription(description: String, page: Int, limit: Int): Try[(List[Institution], Long)] = Try {
    val reader: DirectoryReader         = DirectoryReader.open(writer)
    val searcher: IndexSearcher         = new IndexSearcher(reader)
    val collector: TopScoreDocCollector = TopScoreDocCollector.create(reader.numDocs, Int.MaxValue)
    val startIndex: Int                 = (page - 1) * limit
    val parser: QueryParser             = new QueryParser(InstitutionFields.DESCRIPTION, analyzer)
    val query: Query                    = parser.parse(description)
    val _                               = searcher.search(query, collector)
    val hits: TopDocs                   = collector.topDocs(startIndex, limit);
    val scores: List[ScoreDoc]          = hits.scoreDocs.toList

    val results = scores.map(sc => searcher.doc(sc.doc).toInstitution)
    reader.close()
    (results, hits.totalHits.value)
  }

  def searchById(id: String): Try[Option[Institution]] = Try {
    val reader: DirectoryReader = DirectoryReader.open(writer)
    val searcher: IndexSearcher = new IndexSearcher(reader)
    val parser: QueryParser     = new QueryParser(InstitutionFields.ID, analyzer)
    val query: Query            = parser.parse(id)
    val hits: TopDocs           = searcher.search(query, 1)

    val results = hits.scoreDocs.headOption.map(sc => searcher.doc(sc.doc).toInstitution)
    reader.close()
    results
  }

  def count(description: String): Try[Int] = Try {
    val reader: DirectoryReader = DirectoryReader.open(writer)
    val searcher: IndexSearcher = new IndexSearcher(reader)
    val parser: QueryParser     = new QueryParser(InstitutionFields.DESCRIPTION, analyzer)
    val query: Query            = parser.parse(description)
    val rowCount: Int           = searcher.count(query)
    reader.close()
    rowCount
  }

}
