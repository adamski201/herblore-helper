package adamski.domain;

/**
 * What kind of step a recipe is. The panel lists the steps on the way to a potion - seed, degrime,
 * unf - first and in that order.
 */
public enum RecipeTag {
    /**
     * Grows or cuts a primary out of something else: a seed into herbs, a coral frag into coral, a
     * leaping sturgeon into caviar.
     */
    SEED,
    DEGRIME,
    UNF,
    POTION,
    CAVIAR,
    MISC
}
