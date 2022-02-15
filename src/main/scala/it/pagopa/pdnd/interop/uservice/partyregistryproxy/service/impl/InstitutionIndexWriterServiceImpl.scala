package it.pagopa.pdnd.interop.uservice.partyregistryproxy.service.impl

import it.pagopa.pdnd.interop.uservice.partyregistryproxy.common.system.ApplicationConfiguration
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.common.util.InstitutionField
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.model.Institution
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.service.IndexWriterService
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.service.impl.analizer.InstitutionTokenAnalyzer
import org.apache.lucene.index.{IndexWriter, IndexWriterConfig, Term}
import org.apache.lucene.store.FSDirectory

import java.nio.file.Paths
import scala.util.Try

case object InstitutionIndexWriterServiceImpl extends IndexWriterService[Institution] {

  private val dir: FSDirectory          = FSDirectory.open(Paths.get(ApplicationConfiguration.institutionsIndexDir))
  private val config: IndexWriterConfig = new IndexWriterConfig(InstitutionTokenAnalyzer)
  private val writer: Try[IndexWriter]  = Try(new IndexWriter(dir, config))

  override def adds(items: List[Institution]): Try[Unit] =
    useWriter[Unit](
      writer,
      wr =>
        Try {
          items.foreach { item =>
            wr.updateDocument(new Term(InstitutionField.ID.value, item.id), item.toDocument)
          }
        },
      ()
    )

  def deleteAll(): Try[Long] =
    useWriter(writer, wr => Try(wr.deleteAll()), -1L)

  def commit(): Try[Unit] = {
    useWriter(writer, wr => commitAndClose(wr), ())
  }

}
