package stemmer;


import nnkstemmer.word_node;
import org.apache.commons.collections4.multiset.SynchronizedMultiSet;

import java.util.ArrayList;

import static nnkstemmer.nnkstem.rswas;

public class NNKStemmerAdapter {
    static ArrayList<nnkstemmer.word_node> words = new ArrayList<>();

    public static int stemWord(char []data, int size) {
        try {
            // use the portion of the array that has characters
            // otherwise the entire data array is filled, and the
            // space that is leftover is filled with \\u0000
            words.add(new word_node(String.valueOf(data, 0, size)));
            rswas(words);
            final word_node wn = words.remove(0);
            if (wn.getType() == word_node.ShortWord) {
                return size;
            } else if (wn.getType() == word_node.StopWord) {
                return size;
            }
            return (wn.getNormalized() == null) ? wn.getWord().length() : wn.getNormalized().length();
        } catch (Exception e) {
            System.out.print(e.getMessage());
            System.out.println(words.remove(0));
            return size;
        }
    }
}
