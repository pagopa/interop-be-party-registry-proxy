package it.pagopa.interop.partyregistryproxy.service.impl

import it.pagopa.interop.partyregistryproxy.common.system.ApplicationConfiguration
import it.pagopa.interop.partyregistryproxy.common.util.InstitutionField.ORIGIN_ID
import it.pagopa.interop.partyregistryproxy.model.Institution
import it.pagopa.interop.partyregistryproxy.service.IndexWriterService
import it.pagopa.interop.partyregistryproxy.service.impl.analizer.InstitutionTokenAnalyzer
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.index.{IndexWriter, Term}
import org.apache.lucene.store.FSDirectory

import java.nio.file.Paths
import scala.util.Try

case object UoIndexWriterServiceImpl extends IndexWriterService[Institution] {

  override val dir: FSDirectory        = FSDirectory.open(Paths.get(ApplicationConfiguration.uooIndexDir))
  override val tokenAnalyzer: Analyzer = InstitutionTokenAnalyzer

  override def adds(items: List[Institution])(indexWriter: IndexWriter): Try[Unit] =
    Try(items.foreach { item =>
      indexWriter.updateDocument(new Term(ORIGIN_ID.value, item.originId), item.toDocument)
    })

}
