package it.pagopa.pdnd.interop.uservice.partyregistryproxy.service.impl

import it.pagopa.pdnd.interop.uservice.partyregistryproxy.common.system.ApplicationConfiguration
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.common.util.{CategoryField, createCategoryId}
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.model.Category
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.service.IndexWriterService
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.service.impl.analizer.CategoryTokenAnalyzer
import org.apache.lucene.index.{IndexWriter, IndexWriterConfig, Term}
import org.apache.lucene.store.FSDirectory

import java.nio.file.Paths
import scala.util.Try

case object CategoryIndexWriterServiceImpl extends IndexWriterService[Category] {

  private val dir: FSDirectory          = FSDirectory.open(Paths.get(ApplicationConfiguration.categoriesIndexDir))
  private val config: IndexWriterConfig = new IndexWriterConfig(CategoryTokenAnalyzer)
  private val writer: Try[IndexWriter]  = Try(new IndexWriter(dir, config))

  override def adds(items: List[Category]): Try[Unit] =
    useWriter[Unit](
      writer,
      wr =>
        Try {
          items.foreach { item =>
            val id: String = createCategoryId(code = item.code, origin = item.origin)
            wr.updateDocument(new Term(CategoryField.ID.value, id), item.toDocument)
          }
        },
      ()
    )

  def deleteAll(): Try[Long] = {
    useWriter(writer, wr => Try(wr.deleteAll()), -1L)
  }

  def commit(): Try[Unit] = {
    useWriter(writer, wr => commitAndClose(wr), ())
  }

}
