package adamski.ui;

import net.runelite.api.Experience;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class XpBarViewTest {
    private static final double DELTA = 1e-9;

    @Test
    public void levelsComeFromTheXpTable() {
        final XpBarView view = XpBarView.of(Experience.getXpForLevel(70), 1_000_000);

        assertEquals(70, view.getCurrentLevel());
        assertEquals(Experience.getLevelForXp(Experience.getXpForLevel(70) + 1_000_000), view.getBankedLevel());
    }

    @Test
    public void progressIsHowFarThroughTheBankedLevel() {
        final int start = Experience.getXpForLevel(80);
        final int next = Experience.getXpForLevel(81);
        final int halfway = (next - start) / 2;

        final XpBarView view = XpBarView.of(start, halfway);

        assertEquals(80, view.getBankedLevel());
        assertEquals((double) halfway / (next - start), view.getProgress(), DELTA);
    }

    @Test
    public void nothingBankedLeavesTheLevelWhereItIs() {
        final XpBarView view = XpBarView.of(Experience.getXpForLevel(50), 0);

        assertEquals(50, view.getCurrentLevel());
        assertEquals(50, view.getBankedLevel());
        assertEquals(0, view.getProgress(), DELTA);
    }

    @Test
    public void theBarIsFullAtTheXpCap() {
        final XpBarView view = XpBarView.of(Experience.MAX_SKILL_XP - 10, 1_000_000);

        assertEquals(Experience.MAX_VIRT_LEVEL, view.getBankedLevel());
        assertEquals(1, view.getProgress(), DELTA);
    }

    @Test
    public void theBankedXpIsKeptAsIs() {
        assertEquals(1_234.5, XpBarView.of(0, 1_234.5).getBankedXp(), DELTA);
    }

    @Test
    public void bankedXpIsShortenedWithItsSuffix() {
        assertEquals("+3.179m xp banked", XpBar.bankedText(3_178_790));
        assertEquals("+450.1k xp banked", XpBar.bankedText(450_123));
        assertEquals("+820 xp banked", XpBar.bankedText(820));
    }
}
