package adamski.ui;

import lombok.Value;
import net.runelite.api.Experience;

/**
 * What the xp bar shows: the level now, the level once everything banked is made, and how far
 * through that banked level the player would be. Virtual levels count, up to 126.
 */
@Value
class XpBarView {
    int currentLevel;

    int bankedLevel;

    /**
     * The level the fill is heading for - one past the banked level, or the banked level itself at
     * the last level.
     */
    int nextLevel;

    /**
     * How far through the banked level towards the next, 0 to 1. Full at the last level.
     */
    double progress;

    /**
     * Where the bar starts filling from: how far through the level the player already is, when the
     * bank gains no level - so the bank's own share shows on top of it. 0 once a level is gained,
     * since the whole bar is then a level the bank reaches.
     */
    double startProgress;

    double bankedXp;

    static XpBarView of(int currentXp, double bankedXp) {
        final int afterXp = (int) Math.min(Experience.MAX_SKILL_XP, currentXp + Math.max(0, bankedXp));
        final int bankedLevel = Experience.getLevelForXp(afterXp);

        final int currentLevel = Experience.getLevelForXp(currentXp);

        return new XpBarView(currentLevel, bankedLevel,
                Math.min(bankedLevel + 1, Experience.MAX_VIRT_LEVEL),
                progressThrough(bankedLevel, afterXp),
                bankedLevel > currentLevel ? 0 : progressThrough(currentLevel, currentXp),
                bankedXp);
    }

    /**
     * @return whether the bank takes the player up at least one level
     */
    boolean levelGained() {
        return bankedLevel > currentLevel;
    }

    private static double progressThrough(int level, int xp) {
        if (level >= Experience.MAX_VIRT_LEVEL) return 1;

        final int start = Experience.getXpForLevel(level);
        final int end = Experience.getXpForLevel(level + 1);

        return (double) (xp - start) / (end - start);
    }
}
