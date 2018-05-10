package ptuxiaki.datastructures;

public enum SentenceType {

    /**
     * Title sentence code 0
     */
    TITLE(0, "Title"),
    /**
     * Subtitle sentence code 1
     */
    SUBTITLE(1,"SubTitle"),
    /**
     * Plain sentence code 2
     */
    SENTENCE(2, "Sentence");

    final int type;
    final String descr;

    /**
     * The types a sentence can fall to
     * Values are
     * <ul>
     *     <li>{@link SentenceType#TITLE}</li>
     *     <li>{@link SentenceType#SUBTITLE}</li>
     *     <li>{@link SentenceType#SENTENCE}</li>
     * </ul>
     * @param type
     * @param descr
     */
    SentenceType(final int type, final String descr) {
        this.type = type;
        this.descr = descr;
    }

    @Override
    public String toString() {
        return this.descr;
    }
}