package ptuxiaki.utils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static ptuxiaki.utils.MathUtils.log2p;
import static ptuxiaki.utils.MathUtils.log3;

public class MathUtilsTest {

    @Test
    public void testLog3() {
        assertEquals(1.0, log3(1), 0.0);
        assertEquals(1.46, log3(3), 0.01);
        assertEquals(2.10, log3(8), 0.01);
        assertEquals(2.52, log3(14), 0.01);
    }

    @Test
    public void testLog2() {
        assertEquals(1.0, log2p(1), 0.0);
        assertEquals(2.0, log2p(3), 0.0);
        assertEquals(3.17, log2p(8), 0.01);
        assertEquals(3.91, log2p(14), 0.01);
    }
}
