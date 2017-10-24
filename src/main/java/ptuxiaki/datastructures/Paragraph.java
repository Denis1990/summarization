package ptuxiaki.datastructures;

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

    private int serialNo;
    private List<Sentence> sentences = new ArrayList<>();


    public Paragraph(int serialNo) {
        this.serialNo = serialNo;
    }

    public boolean addSentence(Sentence s) {
        return sentences.add(s);
    }

    public int numberOfSentences() {
        return sentences.size();
    }

    public Sentence getIthSentence(int i) {
        return sentences.get(i);
    }

    public List<Sentence> getSentences() {
        return sentences;
    }

    public boolean containsSentence(int hash) {
        for (Sentence s : sentences) {
            if (s.sentenceHash == hash)
                return true;
        }
        return false;
    }

    public int getSerialNo() {
        return serialNo;
    }
}
