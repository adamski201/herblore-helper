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
     * How far through the banked level towards the next, 0 to 1. Full at the last level.
     */
    double progress;

    double bankedXp;

    static XpBarView of(int currentXp, double bankedXp) {
        final int afterXp = (int) Math.min(Experience.MAX_SKILL_XP, currentXp + Math.max(0, bankedXp));
        final int bankedLevel = Experience.getLevelForXp(afterXp);

        return new XpBarView(Experience.getLevelForXp(currentXp), bankedLevel,
                progressThrough(bankedLevel, afterXp), bankedXp);
    }

    private static double progressThrough(int level, int xp) {
        if (level >= Experience.MAX_VIRT_LEVEL) return 1;

        final int start = Experience.getXpForLevel(level);
        final int end = Experience.getXpForLevel(level + 1);

        return (double) (xp - start) / (end - start);
    }
}
