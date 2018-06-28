package ptuxiaki.datastructures;

import ptuxiaki.utils.SentenceUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class Sentence implements Comparable<Sentence> {
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
     * Sentence termsWeight whether tf*Idf or tf*ISF
     */
    private double termsWeight;

    /**
     * The termsWeight based on how many title terms
     * this sentence has.
     */
    private double titleTermWeight;

    /**
     * Weight based on the position inside the paragraph
     */
    private double sentenceLocationWeight;

    /**
     * The composite weight calculated using
     * titleTermWeight, sentenceLocationWeight and termsWeight
     */
    private double sentenceWeight;

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
        this.termsWeight = -1.0;
        this.titleTermWeight = -1.0;
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

    public double getTermsWeight() {
        return termsWeight;
    }

    public void setTermsWeight(double termsWeight) {
        this.termsWeight = termsWeight;
    }

    /**
     * <p>Update the value of term termsWeight.</p>
     * This method is called on baxendales algorithm to essentially boost
     * the termsWeight of the first sentence of the paragraph
     * @param wsl the coefficient to multiply with
     */
    public void updateTermsWeight(double wsl) {
        termsWeight += (termsWeight * wsl);
    }

    /**
     * Compute the total termsWeight of the sentence.
     * Add the 3 individual termsWeight multiplying each by it's coeffient first.
     * @param wtt Title term coefficient
     * @param wst Term termsWeight coefficient
     * @param wsl Sentence location coefficient
     * @return the total termsWeight according to equation (wtt * tt) + (wsl * sl) + (wst * st)
     */
    public void compositeWeight(double wtt, double wst, double wsl) {
        sentenceWeight = wtt * titleTermWeight + wst * termsWeight + wsl * sentenceLocationWeight;
    }

    public double getTitleTermWeight() {
        return titleTermWeight;
    }

    public void setTitleTermWeight(double titleTermWeight) {
        this.titleTermWeight = titleTermWeight;
    }

    public void setSLWeight(final double c) {
        if (sentenceLocationWeight == 0) {
            sentenceLocationWeight = c;
        } else {
            sentenceLocationWeight *= c;
        }
    }

    /**
     * @param n
     * @return True if the sentence has n or less words, False otherwise
     */
    public boolean hasLessThanNWords(int n) {
        return wordsCount <= n;
    }

    public int getPosition() {
        return position;
    }

    public boolean isFirstInParagraph() {
        return parPosition == 0;
    }

    @Override
    public String toString() {
        return String.format("%s | %s | %d | %d | %f | %f | %f", text, type.toString(), parPosition, position, titleTermWeight, sentenceWeight, sentenceLocationWeight);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Sentence)) return false;
        Sentence sentence = (Sentence) o;
        return Objects.equals(text, sentence.text) &&
                Objects.equals(stemmedText, sentence.stemmedText) &&
                type == sentence.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(text, stemmedText, type);
    }

    @Override
    public int compareTo(Sentence s) {
        if (this.sentenceWeight < s.sentenceWeight) {
            return -1;
        } else if (this.sentenceWeight < s.sentenceWeight) {
            return 1;
        } else {
            return 0;
        }
    }
}
