package ptuxiaki.utils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static ptuxiaki.utils.MathUtils.log2;
import static ptuxiaki.utils.MathUtils.log3;

public class MathUtilsTest {

    @Test
    public void testLog3() {
        assertEquals(2.0, log3(8), 0.0);
        assertEquals(4.0, log3(80), 0.0);
        assertEquals(0.0, log3(0), 0.0);
    }

    @Test
    public void testLog2() {
        assertEquals(4.0, log2(14), 0.0);
        assertEquals(2.0, log2(2), 0.0);
        assertEquals(1.0, log2(0), 0.0);
    }
}
