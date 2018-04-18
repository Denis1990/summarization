package ptuxiaki.datastructures;

import org.apache.commons.lang3.text.StrBuilder;
import org.apache.commons.lang3.tuple.Triple;

import java.util.ArrayList;
import java.util.List;

public class Paragraph {

    /**
     * Position inside the document
     * i.e first paragraphs third paragraph
     */
    private int pos;

    /**
     * A sentence is Triplet structure. The left element is the sentence itself as a string
     * The middle element is the position of the sentence inside the paragraph.
     * The right element is the position of the sentence inside the document.
     */
    private List<Triple<String, Integer, Integer>> sentences = new ArrayList<>();

    public Paragraph(int pos) {
        this.pos = pos;
    }

    public boolean addSentence(Triple<String, Integer, Integer> s) {
        return sentences.add(s);
    }

    public int numberOfSentences() {
        return sentences.size();
    }

    public String getFirstSentence() {
        return getIthSentence(0);
    }

    public String getIthSentence(int i) {
        return sentences.get(i).getLeft();
    }

    public Triple<String, Integer, Integer> getSentenceTriplet(int i) {
        return sentences.get(i);
    }

    public int getPositionInDocument() {
        return pos;
    }

    public boolean isEmpty() {
        return sentences.isEmpty();
    }

    @Override
    public String toString() {
        StrBuilder paragraph = new StrBuilder();
        paragraph.append("[");
        for (Triple<String, Integer, Integer> t : sentences) {
            paragraph.append(t.getLeft().trim());
            paragraph.append(",");
        }
        paragraph.append("]");
        return paragraph.toString();
    }
}
