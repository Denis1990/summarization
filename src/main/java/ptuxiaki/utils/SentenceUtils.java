package ptuxiaki.utils;

import org.apache.commons.lang3.text.StrBuilder;
import org.apache.lucene.analysis.el.GreekAnalyzer;
import org.apache.lucene.analysis.el.GreekStemmer;
import org.apache.lucene.analysis.util.CharArraySet;
import ptuxiaki.datastructures.Conf;
import stemmer.NNKStemmerAdapter;

public class SentenceUtils {

    private static GreekStemmer greekStemmer = new GreekStemmer();

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
        if (Conf.instance().stemmerClass().equals(PropertyKey.NNKSTEMER)) {
            return stemWordNNK(word);
        } else {
            return stemWordLucene(word);
        }
    }

    private static String stemWordNNK(String word) {
        final int l = NNKStemmerAdapter.stemWord(word.toCharArray(), word.length());
        return word.substring(0, l);
    }

    private static String stemWordLucene(String word) {
        final int l = greekStemmer.stem(word.toCharArray(), word.length());
        return word.substring(0, l);
    }


    public static String stemSentence(final String sentence) {
        String [] words = removeWhiteSpaces(replaceSigma(removeTonation(removeNumbers((removeSpecialChars(sentence.toLowerCase())))))).split("\\s+");
        StrBuilder strBuilder = new StrBuilder();
        for (String w : words) {
            if (!STOP_WORDS.contains(w)) {
                strBuilder.append(stemWord(w)).append(" ");
            }
        }
        return strBuilder.toString().trim();
    }

    public static String removeSpecialChars(final String word) {
        return word.replaceAll("[@#$%^&*();!\"',»«.\\-:…]", "");
    }

    public static String removeNumbers(final String word) {
        return word.replaceAll("[0-9]", "");
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
