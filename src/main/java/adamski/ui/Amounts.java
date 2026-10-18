package adamski.ui;

final class Amounts {
    private Amounts() {
    }

    /**
     * Short enough to sit beside an item name in a panel about 225px wide.
     */
    static String abbreviate(double value) {
        final String sign = value < 0 ? "-" : "";
        final double magnitude = Math.abs(value);

        if (magnitude >= 1_000_000) return sign + String.format("%.1fm", magnitude / 1_000_000);
        if (magnitude >= 10_000) return sign + String.format("%.0fk", magnitude / 1_000);
        if (magnitude >= 1_000) return sign + String.format("%.1fk", magnitude / 1_000);

        return sign + String.format("%.0f", magnitude);
    }
}
