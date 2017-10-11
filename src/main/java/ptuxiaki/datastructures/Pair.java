package ptuxiaki.datastructures;

/**
 * This class holds a pair of integer and double.
 * The integer represents the index of a sentence in an array
 * while the value represents the combined values of <b>tf * idf</b> measurement
 * plus the title term factor <b>TT</b>
 */
public class Pair implements Comparable<Pair> {
    public int index;
    public double value;

    private Pair(int i, double d) {
        this.index = i;
        this.value = d;
    }

    public static Pair of(int i, double d) {
        return new Pair(i, d);
    }

    @Override
    public int compareTo(Pair p) {
        return Double.compare(this.value, p.value);
    }

}
