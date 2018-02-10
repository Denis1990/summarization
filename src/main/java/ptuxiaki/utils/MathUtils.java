package ptuxiaki.utils;

import static java.lang.Math.log;
import static java.lang.Math.log1p;

public class MathUtils {

    /**
     * Return the log with base 3 of value (x + 1).
     * Use math identity:
     * logb(n) = loge(n) / loge(b)
     * @param x
     * @return
     */
    public static double log3(int x) {
        return log1p(x) / log(3);
    }

    /**
     * Return the log with base 2 of value (x + 2).
     * Use math identity:
     * logb(n) = loge(n) / loge(b)
     * @param x
     * @return
     */
    public static double log2(int x) {
        return log((double)x + 2) / log((double)2);
    }

}
