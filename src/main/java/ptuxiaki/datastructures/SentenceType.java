package ptuxiaki.datastructures;

public enum SentenceType {

    TITLE(0, "Title"),
    SUBTITLE(1,"SubTitle"),
    SENTENCE(2, "Sentence");

    final int type;
    final String descr;

    SentenceType(final int type, final String descr) {
        this.type = type;
        this.descr = descr;
    }

    @Override
    public String toString() {
        return this.descr;
    }
}