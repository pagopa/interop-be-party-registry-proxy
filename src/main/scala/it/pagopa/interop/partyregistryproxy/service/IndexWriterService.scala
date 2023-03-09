package it.pagopa.interop.partyregistryproxy.service

import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.index.{IndexWriter, IndexWriterConfig}
import org.apache.lucene.store.FSDirectory

import scala.util.Try

trait IndexWriterService[A] {
  def dir: FSDirectory
  def tokenAnalyzer: Analyzer

  def adds(items: List[A])(indexWriter: IndexWriter): Try[Unit]

  def resource[B](f: IndexWriter => Try[B]): Try[B] = for {
    iw     <- Try(new IndexWriter(dir, new IndexWriterConfig(tokenAnalyzer)))
    result <- f(iw)
    _      <- close(iw)
  } yield result

  def deleteAll(writer: IndexWriter): Try[Long] = Try(writer.deleteAll())
  def commit(writer: IndexWriter): Try[Long]    = Try(writer.commit())
  def close(writer: IndexWriter): Try[Unit]     = Try {
    writer.commit()
    writer.close()
  }
}
