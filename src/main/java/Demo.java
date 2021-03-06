import nnkstemmer.word_node;
import org.apache.lucene.analysis.el.GreekAnalyzer;
import org.apache.lucene.analysis.el.GreekStemmer;
import org.apache.lucene.analysis.util.CharArraySet;
import ptuxiaki.indexing.Indexer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import static nnkstemmer.nnkstem.StrToWords;
import static nnkstemmer.nnkstem.rswas;
import static ptuxiaki.utils.SentenceUtils.*;

public class Demo {

    private static GreekStemmer grStemmer = new GreekStemmer();


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

    static ArrayList<word_node> words = new ArrayList<>();

    public static void luceneDemo() throws IOException {
        final String path = System.getenv("HOME") + File.separator + "Documents/demoLucene";
        Indexer indexer = new Indexer(System.getenv("HOME") + File.separator + "temp_index");
        if (!indexer.indexExists()) {
            indexer.indexDirectory(path);
        }
        indexer.printStatistics();
    }

    public static void main(String[] args) {
        try {
            luceneDemo();
//            stemmerDemo();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void stemmerDemo() throws IOException {
        String text = "Ο υπουργός Εξωτερικών της Τουρκίας Γιασάρ Γιακίς και ο αρχηγός Στρατού της χώρας, στρατηγός Αϊτάτς Γιαλμάν, κατά τη διάρκεια επαφών τους στα κατεχόμενα, προφανώς κινούμενοι σε αυτό το κλίμα, έπλεξαν το εγκώμιο του Ραούφ Ντενκτάς, δηλώνοντας ότι οι απόψεις του στο Κυπριακό ταυτίζονται με αυτές της Άγκυρας.";
        System.out.println("========WITH LUCENE STEMMER==========");
        for (String word : text.split("\\s+")) {
            if (STOP_WORDS.contains(word) || word.length() <= 3)
                continue;
            word = removeSpecialChars(removeWhiteSpaces(removeTonation(replaceSigma(word.toLowerCase()))));
            System.out.println(
                    String.format("[%s] => [%s]", word, stemWithGreekStemmer(word))
            );
        }

        System.out.println("========WITH NNKSTEMMER==========");
        ArrayList<word_node> doc_words = new ArrayList<>();
        StrToWords(text, doc_words);
        rswas(doc_words);
        System.out.println(doc_words);
        for (String word : text.split("\\s+")) {
            word = removeSpecialChars(removeTonation(word));
            System.out.println(
                    String.format("[%s] => [%s]", word, stemWithNNKStemmer(word))
            );
        }
    }


    public static String stemWithGreekStemmer(String word) {
        int l = grStemmer.stem(word.toCharArray(), word.length());
        return word.substring(0, l);
    }

    public static String stemWithNNKStemmer(String word) {
        words.add(new word_node(word));
        rswas(words);
        word_node wn = words.remove(0);
        if (wn.getType() == word_node.StopWord) {
            return "";
        } else if (wn.getType() == word_node.ShortWord) {
            return word;
        }
        return wn.getNormalized().toLowerCase();
    }
}

