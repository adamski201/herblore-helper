package adamski.domain.models;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Getter
@EqualsAndHashCode
@ToString
public final class Recipe {
    private final int id;
    private final Ingredient primary;
    private final List<Ingredient> secondaries;
    private final Ingredient output;
    private final String tag;
    private final float xp;
    private final int level;

    public Recipe(int id, Ingredient primary, Ingredient[] secondaries, Ingredient output,
                  String tag, float xp, int level) {
        this.id = id;
        this.primary = primary;
        this.secondaries = Collections.unmodifiableList(Arrays.asList(secondaries.clone()));
        this.output = output;
        this.tag = tag;
        this.xp = xp;
        this.level = level;
    }
}
