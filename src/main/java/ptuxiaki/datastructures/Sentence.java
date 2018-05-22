package ptuxiaki.datastructures;

public class Sentence {
    /**
     * Sentence text
     */
    private String text;

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

    /**
     * Position inside the document
     * i.e first sentence third sentence etc
     */
    public final int position;

    /**
     * Construct a sentence with the given text.
     * This is considered the first sentence by default
     * @param text
     */
    public Sentence(String text) {
        this(text, SentenceType.SENTENCE, 0);
    }

    /**
     * Construct a sentence with the given text
     * which is in the position {@code p} inside the document
     * This is considered a normal sentence.
     * @param text
     * @param position
     */
    public Sentence(String text, int position) {
        this(text, SentenceType.SENTENCE, position);
    }

    /**
     * Construct a sentence with the given text
     * which is in the position {@code p} inside the document
     * and is of type {@link SentenceType}
     * @param text
     * @param type
     * @param position
     */
    public Sentence(String text, SentenceType type, int position) {
        this.text = text;
        this.type = type;
        this.position = position;
        this.weight = 0.0;
        this.titleTerm = 0.0;
    }

    public SentenceType getType() {
        return type;
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

    public int getPosition() {
        return position;
    }

    @Override
    public String toString() {
        return text;
    }
}