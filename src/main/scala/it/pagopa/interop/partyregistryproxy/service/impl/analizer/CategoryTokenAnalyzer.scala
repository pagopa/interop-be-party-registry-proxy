package it.pagopa.interop.partyregistryproxy.service.impl.analizer

import org.apache.lucene.analysis.it.ItalianAnalyzer
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilter
import org.apache.lucene.analysis.ngram.NGramTokenFilter
import org.apache.lucene.analysis.standard.StandardTokenizer
import org.apache.lucene.analysis.{Analyzer, StopFilter}

case object CategoryTokenAnalyzer extends Analyzer {

  override def createComponents(fieldName: String): Analyzer.TokenStreamComponents = {
    val source: StandardTokenizer = new StandardTokenizer()
    val stopFilter: StopFilter    = new StopFilter(source, ItalianAnalyzer.getDefaultStopSet)
    val ascii: ASCIIFoldingFilter = new ASCIIFoldingFilter(stopFilter)
    val ngram: NGramTokenFilter   = new NGramTokenFilter(ascii, 3, 5, true)
    new Analyzer.TokenStreamComponents(source, ngram)
  }
}
