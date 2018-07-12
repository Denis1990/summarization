package ptuxiaki.datastructures;

import java.util.ArrayList;
import java.util.List;

/**
 * A class that represents a paragraph from a document.
 * It hold a list of {@link Sentence} objects as well
 * as an integer value coresponding to its position 
 * inside the document.
 */
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
    private List<Sentence> sentences = new ArrayList<>();

    /**
     * Add s sentence into the paragraph.
     */
    public boolean addSentence(Sentence s) {
        return sentences.add(s);
    }

    /**
     * Assign an int value that represents the position of the paragraph in the document.
     * i.e first paragraph, third paragraph etc
     * @param parPosition
     */
    public void setPosition(final int parPosition) {
        this.pos = parPosition;
    }

    public int numberOfSentences() {
        return sentences.size();
    }

    public Sentence getFirstSentence() {
        return getIthSentence(0);
    }

    public Sentence getIthSentence(int i) {
        return sentences.get(i);
    }

    public int getPositionInDocument() {
        return pos;
    }

    /**
     * Return the sentences that have more than minWords
     * @return
     */
    public List<Sentence> getAllSentences() {
        return sentences;
    }

    @Override
    public String toString() {
        StringBuilder paragraph = new StringBuilder();
        paragraph.append("--");
        for (Sentence s : sentences) {
            if (s.isTitle()) {
                paragraph.append(s.toString());
            } else if (s.isSubTitle()) {
                paragraph.append("\t").append(s.toString());
            } else {
                paragraph.append("\t\t").append(s.toString());
            }
            paragraph.append(System.lineSeparator());
        }
        return paragraph.toString();
    }
}
