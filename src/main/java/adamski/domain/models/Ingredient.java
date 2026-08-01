package adamski.domain.models;

import lombok.Value;

/**
 * An item and how much of it a recipe consumes or produces.
 */
@Value
public class Ingredient {
    int itemId;
    int quantity;
}
