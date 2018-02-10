package ptuxiaki.datastructures;

import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;

public class Paragraph {

    public static class Sentence {
        /** Location inside the paragraph */
        public int position;

        /** The hashCode value of the sentence. */
        public int sentenceHash;

        public Sentence(int location, int hash) {
            this.position = location;
            this.sentenceHash = hash;
        }
    }

    /**
     * Position inside the document
     * i.e first paragraphs third paragraph
     */
    private int pos;

    private List<Pair<String, Integer>> sentences = new ArrayList<>();

    public Paragraph(int pos) {
        this.pos = pos;
    }

    public boolean addSentence(Pair<String, Integer> s) {
        return sentences.add(s);
    }

    public int numberOfSentences() {
        return sentences.size();
    }

    public String getIthSentence(int i) {
        return sentences.get(i).getKey();
    }

//    public boolean containsSentence(int hash) {
//        for (Sentence s : sentences) {
//            if (s.sentenceHash == hash)
//                return true;
//        }
//        return false;
//    }

    public int getPosition() {
        return pos;
    }
}
