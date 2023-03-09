package it.pagopa.interop.partyregistryproxy.service.impl

import it.pagopa.interop.partyregistryproxy.common.system.ApplicationConfiguration
import it.pagopa.interop.partyregistryproxy.common.util.CategoryField.ID
import it.pagopa.interop.partyregistryproxy.common.util.createCategoryId
import it.pagopa.interop.partyregistryproxy.model.Category
import it.pagopa.interop.partyregistryproxy.service.IndexWriterService
import it.pagopa.interop.partyregistryproxy.service.impl.analizer.CategoryTokenAnalyzer
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.index.{IndexWriter, Term}
import org.apache.lucene.store.FSDirectory

import java.nio.file.Paths
import scala.util.Try

case object CategoryIndexWriterServiceImpl extends IndexWriterService[Category] {

  override val dir: FSDirectory        = FSDirectory.open(Paths.get(ApplicationConfiguration.categoriesIndexDir))
  override val tokenAnalyzer: Analyzer = CategoryTokenAnalyzer

  override def adds(items: List[Category])(indexWriter: IndexWriter): Try[Unit] =
    Try(items.foreach { item =>
      val id: String = createCategoryId(code = item.code, origin = item.origin)
      indexWriter.updateDocument(new Term(ID.value, id), item.toDocument)
    })

}
