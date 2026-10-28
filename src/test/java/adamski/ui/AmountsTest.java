package adamski.ui;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AmountsTest {
    @Test
    public void dosesKeepADecimalFromOneThousandUpToFiftyThousand() {
        assertEquals("15.5k", Amounts.abbreviateDoses(15_510));
        assertEquals("1.6k", Amounts.abbreviateDoses(1_569));
        assertEquals("49.9k", Amounts.abbreviateDoses(49_900));
    }

    @Test
    public void dosesOutsideThatRangeReadAsAnyOtherAmount() {
        assertEquals("968", Amounts.abbreviateDoses(968));
        assertEquals("50k", Amounts.abbreviateDoses(50_000));
        assertEquals("1.2m", Amounts.abbreviateDoses(1_200_000));
    }

    @Test
    public void otherAmountsDropTheDecimalFromTenThousand() {
        assertEquals("16k", Amounts.abbreviate(15_510));
    }
}
