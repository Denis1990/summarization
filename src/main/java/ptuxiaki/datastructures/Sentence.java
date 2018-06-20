package ptuxiaki.datastructures;

import ptuxiaki.utils.SentenceUtils;

import java.util.Arrays;
import java.util.List;

public class Sentence {
    /**
     * Sentence text
     */
    private String text;

    /**
     * The stemmed sentence
     */
    private String stemmedText;

    /**
     * What kind of sentence it is
     * @see SentenceType
     */
    private SentenceType type;

    /**
     * Sentence weight whether tf*Idf or tf*ISF
     */
    private double weight;

    /**
     * The weight based on how many title terms
     * this sentence has.
     */
    private double titleTerm;

    private double sentenceLocation;

    /**
     * Position inside the document
     * i.e first sentence third sentence etc
     */
    public final int position;

    /**
     * Position inside the paragraph
     */
    public final int parPosition;

    /**
     * How many words the original sentence has.
     */
    private final int wordsCount;

    /**
     * Construct a sentence with the given text.
     * This is considered the first sentence by default
     * @param text
     */
    public Sentence(String text) {
        this(text, SentenceType.SENTENCE, 0, 1);
    }

    /**
     *
     * @param text
     * @param position
     */
    public Sentence(String text, int position) {
        this(text, SentenceType.SENTENCE, position, 0);
    }

    /**
     * Construct a sentence with the given text
     * which is in the position {@code p} inside the document
     * This is considered a normal sentence.
     * @param text
     * @param position
     * @param parPosition
     */
    public Sentence(String text, int position, int parPosition) {
        this(text, SentenceType.SENTENCE, position, parPosition);
    }

    /**
     * Construct a sentence with the given text
     * which is in the position {@code p} inside the document
     * and is of type {@link SentenceType}
     * @param text
     * @param type
     * @param position
     */
    public Sentence(String text, SentenceType type, int position, int parPosition) {
        this.text = text;
        this.type = type;
        this.position = position;
        this.parPosition = parPosition;
        this.weight = 0.0;
        this.titleTerm = 0.0;
        this.stemmedText = SentenceUtils.stemSentence(text);
        this.wordsCount = text.split("\\s+").length;
    }

    public boolean isTitle() {
        return this.type == SentenceType.TITLE;
    }

    public boolean isSubTitle() {
        return this.type == SentenceType.SUBTITLE;
    }

    public SentenceType getType() {
        return type;
    }

    public String getText() {
        return this.text;
    }

    public String [] getStemmedTerms() {
        return this.stemmedText.split("\\s+");
    }

    public List<String> getStemmedTermsAsList() {
        return Arrays.asList(this.stemmedText.split("\\s+"));
    }

    public int getWordsCount() {
        return this.wordsCount;
    }

    public void setType(SentenceType type) {
        this.type = type;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public double getTitleTerm() {
        return titleTerm;
    }

    public void setTitleTerm(double titleTerm) {
        this.titleTerm = titleTerm;
    }

    public void setSentLocationWeight(double sl) {
        this.sentenceLocation = sl;
    }

    public double getSentLocationWeight() {
        return this.sentenceLocation;
    }
    public int getPosition() {
        return position;
    }

    public int getParPosition() {
        return parPosition;
    }

    @Override
    public String toString() {
        return String.format("%s | %s | %d | %d", text, type.toString(), parPosition, position);
    }
}
