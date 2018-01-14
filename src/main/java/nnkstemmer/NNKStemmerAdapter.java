package nnkstemmer;

import java.util.ArrayList;
import java.util.Arrays;

import static nnkstemmer.nnkstem.rswas;

public class NNKStemmerAdapter {
    static ArrayList<word_node> words = new ArrayList<>();

    public static int stemWord(char []data, int size) {
        String stemmed;
        int len;
        try {
            words.add(new word_node(String.valueOf(data)));
            rswas(words);
            stemmed = words.remove(0).getNormalized();
            if (stemmed == null) {
                len = size;
                return len;
            }
            len = stemmed.indexOf("\u0000");
            Arrays.fill(data, '\u0000');
            return len;
        } catch (NullPointerException npe){
            System.out.println("WWWWWWWWWWWWWWW");
            return 4;
        }
    }
}
