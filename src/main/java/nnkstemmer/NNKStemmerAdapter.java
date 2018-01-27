package nnkstemmer;

import java.util.ArrayList;

import static nnkstemmer.nnkstem.rswas;

public class NNKStemmerAdapter {
    static ArrayList<word_node> words = new ArrayList<>();

    public static int stemWord(char []data, int size) {
        try {
            words.add(new word_node(String.valueOf(data)));
            rswas(words);
            final word_node wn = words.remove(0);
            if (wn.getType() == word_node.ShortWord) {
                return size;
            } else if (wn.getType() == word_node.StopWord) {
                return size;
            }
            return wn.getNormalized().length();
        } catch (NullPointerException npe){
            return 4;
        }
    }
}
