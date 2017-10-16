package ptuxiaki.datastructures;

import java.util.ArrayList;
import java.util.List;

public class Paragraph {

    public static class Sentence {
        /** Location inside the paragraph */
        int location;

        /** The hashCode value of the sentence. */
        int sentenceHash;

        public Sentence(int location) {
            this.location = location;
        }

        public Sentence(int location, int hash) {
            this.location = location;
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

    public int numberOfParagraphs() {
        return sentences.size();
    }


    public boolean containsSentence(int hash) {
        for (Sentence s : sentences) {
            if (s.sentenceHash == hash)
                return true;
        }
        return false;
    }
}
