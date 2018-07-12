package ptuxiaki.datastructures;

import ptuxiaki.utils.SentenceUtils;

import java.util.Arrays;
import java.util.List;


/**
 * A sentence representation that holds along with its text, the weights assigned to it by the different algorithms.
 *
 */
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
     * If this sentence is considered among the sentences for summarization
     */
    private boolean ignored;

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
        this.termsWeight = -10.0;
        this.titleTermWeight = -10.0;
        this.sentenceLocationWeight = -10.0;
        this.stemmedText = SentenceUtils.stemSentence(text);
        this.wordsCount = text.split("\\s+").length;
        this.ignored = true;
    }

    public boolean isTitle() {
        return this.type == SentenceType.TITLE;
    }

    public boolean isSubTitle() {
        return this.type == SentenceType.SUBTITLE;
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

    public double getTermsWeight() {
        return termsWeight;
    }

    public void setTermsWeight(double termsWeight) {
        this.termsWeight = termsWeight;
        ignored = false;
    }

    /**
     * <p>Update the value of term termsWeight.</p>
     * This method is called on baxendales algorithm to essentially boost
     * the termsWeight of the first sentence of the paragraph
     * @param wsl the coefficient to multiply with
     */
    public void updateTermsWeight(double wsl) {
        termsWeight += (termsWeight * wsl);
        ignored = false;
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

    /**
     * Set the title term weight. Only if it a sentence.
     * @param titleTermWeight
     */
    public void setTitleTermWeight(double titleTermWeight) {
        this.titleTermWeight = titleTermWeight;
        ignored = false;
    }

    public void setSLWeight(final double c) {
        sentenceLocationWeight = c;
        ignored = false;
    }

    /**
     * @param n
     * @return True if the sentence has n or less words, False otherwise
     */
    public boolean hasLessThanNWords(int n) {
        if (wordsCount <= n) {
            ignored = true;
        }
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
        //For example when one of the coefficients are 0
        final String s = ignored ? "X %-45.45s... | %s | %-2d | %-2d | %+2.3f | %+2.3f | %+2.3f | %+2.3f" : "%-47.47s... | %s | %-2d | %-2d | %+2.3f | %+2.3f | %+2.3f | %+2.3f";
        return String.format(s, text, type.toString(), parPosition, position, titleTermWeight, termsWeight, sentenceLocationWeight, sentenceWeight);
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
