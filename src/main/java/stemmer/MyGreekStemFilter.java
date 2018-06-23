package stemmer;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.el.GreekLowerCaseFilter;
import org.apache.lucene.analysis.miscellaneous.SetKeywordMarkerFilter;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.KeywordAttribute;

import java.io.IOException;

/**
 * A {@link TokenFilter} that applies {@link nnkstemmer.nnkstem} to stem Greek
 * words.
 * <p>
 * To prevent terms from being stemmed use an instance of
 * {@link SetKeywordMarkerFilter} or a custom {@link TokenFilter} that sets
 * the {@link KeywordAttribute} before this {@link TokenStream}.
 * </p>
 * <p>
 * NOTE: Input is expected to be casefolded for Greek (including folding of final
 * sigma to sigma), and with diacritics removed. This can be achieved by using
 * either {@link GreekLowerCaseFilter} or ICUFoldingFilter before GreekStemFilter.
 *
 */

public final class MyGreekStemFilter extends TokenFilter {
    //private final GreekStemmer stemmer = new GreekStemmer();
    private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
    private final KeywordAttribute keywordAttr = addAttribute(KeywordAttribute.class);

    public MyGreekStemFilter(TokenStream input) {
        super(input);
    }

    @Override
    public boolean incrementToken() throws IOException {
        if (input.incrementToken()) {
            if (!keywordAttr.isKeyword()) {
                final int newlen = NNKStemmerAdapter.stemWord(termAtt.buffer(), termAtt.length());
                termAtt.setLength(newlen);
            }
            return true;
        } else {
            return false;
        }
    }
}
