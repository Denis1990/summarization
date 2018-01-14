package nnkstemmer;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.el.GreekLowerCaseFilter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.standard.std40.StandardTokenizer40;
import org.apache.lucene.analysis.util.StopwordAnalyzerBase;
import org.apache.lucene.util.Version;

/**
 * {@link Analyzer} for the Greek language.
 * <p>
 *   This implementation uses the greek stemer provided by nnk.
 * </p>
 * <p><b>NOTE</b>: This class uses the same {@link org.apache.lucene.util.Version}
 * dependent settings as {@link StandardAnalyzer}.</p>
 */
public class MyGreekAnalyzer extends StopwordAnalyzerBase {

    @Override
    protected TokenStreamComponents createComponents(String fieldName) {
        final Tokenizer source;
        if (getVersion().onOrAfter(Version.LUCENE_4_7_0)) {
            source = new StandardTokenizer();
        } else {
            source = new StandardTokenizer40();
        }
        TokenStream result = new GreekLowerCaseFilter(source);
        result = new StandardFilter(result);
        result = new StopFilter(result, stopwords);
        result = new MyGreekStemFilter(result);
        return new TokenStreamComponents(source, result);
    }
}
