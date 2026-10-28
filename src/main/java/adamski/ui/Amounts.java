package adamski.ui;

public final class Amounts {
    private Amounts() {
    }

    /**
     * Short enough to sit beside an item name in a panel about 225px wide.
     */
    public static String abbreviate(double value) {
        final String sign = value < 0 ? "-" : "";
        final double magnitude = Math.abs(value);

        if (magnitude >= 1_000_000) return sign + String.format("%.1fm", magnitude / 1_000_000);
        if (magnitude >= 10_000) return sign + String.format("%.0fk", magnitude / 1_000);
        if (magnitude >= 1_000) return sign + String.format("%.1fk", magnitude / 1_000);

        return sign + String.format("%.0f", magnitude);
    }

    /**
     * As {@link #abbreviate}, but keeping one decimal from 1k up to 50k - "15.5k" rather than "16k" -
     * where a potion's dose count needs it.
     */
    public static String abbreviateDoses(double value) {
        final double magnitude = Math.abs(value);

        if (magnitude >= 1_000 && magnitude < 50_000) {
            return (value < 0 ? "-" : "") + String.format("%.1fk", magnitude / 1_000);
        }

        return abbreviate(value);
    }
}
