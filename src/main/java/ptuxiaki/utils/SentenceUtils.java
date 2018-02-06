package ptuxiaki.utils;

import org.apache.commons.lang3.text.StrBuilder;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.lucene.analysis.el.GreekAnalyzer;
import org.apache.lucene.analysis.util.CharArraySet;
import ptuxiaki.datastructures.SentenceType;
import stemmer.NNKStemmerAdapter;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class SentenceUtils {

    private static NNKStemmerAdapter grStemmer = new NNKStemmerAdapter();

    private static CharArraySet STOP_WORDS = GreekAnalyzer.getDefaultStopSet();

    static {
        STOP_WORDS.add("μασ");
        STOP_WORDS.add("του");
        STOP_WORDS.add("τουσ");
        STOP_WORDS.add("τισ");
        STOP_WORDS.add("τη");
        STOP_WORDS.add("στου");
        STOP_WORDS.add("στουσ");
        STOP_WORDS.add("στα");
    }

    public static String stemWord(String word) {
        final int l = NNKStemmerAdapter.stemWord(word.toCharArray(), word.length());
        return word.substring(0, l);
    }

    public static String stemSentence(final String sentence) {
        String [] words = Arrays.stream(sentence.split("\\s+"))
                .filter(s -> !s.isEmpty())
                .map(String::toLowerCase)
                .map(SentenceUtils::removeSpecialChars)
                .map(SentenceUtils::removeTonation)
                .map(SentenceUtils::removeWhiteSpaces)
                .map(SentenceUtils::replaceSigma)
                .collect(Collectors.toList()).toArray(new String[]{});
        StrBuilder strBuilder = new StrBuilder();
        for (String w : words) {
            if (!STOP_WORDS.contains(w)) {
                strBuilder.append(stemWord(w.toLowerCase())).append(" ");
            }
        }
        return strBuilder.toString().trim();
    }

    // FIXME: Implement this method
    public static long keywords(String sentence, Set<Pair<String, SentenceType>> titleWords) {
        return 7L;
    }

    public static String removeSpecialChars(final String word) {
        return word.replaceAll("[@#$%^&*()!\"\\,»«\\.-]", " ").trim();
    }

    public static String removeWhiteSpaces(final String sentence) {
        return sentence.replaceAll("\\n", " ")
                .replaceAll("\\r", " ")
                .replaceAll("\\t", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    public static String removeTonation(final String word) {
        return word.replaceAll("ά","α")
                .replaceAll("έ", "ε")
                .replaceAll("ή", "η")
                .replaceAll("ί", "ι")
                .replaceAll("ό", "ο")
                .replaceAll("ύ", "υ")
                .replaceAll("ώ", "ω")
                .replaceAll("ϋ", "υ")
                .replaceAll("ΰ", "υ")
                .replaceAll("ϊ", "ι")
                .replaceAll("ΐ", "ι");
    }

    public static String replaceSigma(final String word) {
        return word.replaceAll("ς", "σ");
    }
}
