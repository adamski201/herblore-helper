package adamski.domain.models;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

/**
 * What the bank makes: one chain per product, and the recipes those chains need.
 */
@Getter
@EqualsAndHashCode
@ToString
public final class ChainPlan {
    private final List<RecipeChain> chains;

    /**
     * Every chain's recipes together, one per primary, in the order the cascade must run them.
     */
    private final List<Recipe> selection;

    public ChainPlan(List<RecipeChain> chains, List<Recipe> selection) {
        this.chains = List.copyOf(chains);
        this.selection = List.copyOf(selection);
    }
}
