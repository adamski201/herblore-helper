package adamski.domain;

import lombok.Value;

/**
 * An item that can be turned into a secondary e.g. birds nest -> crushed birds nest.
 */
@Value
public class SecondaryPrecursor {
    int itemId;

    int secondaryItemId;

    double yield;
}
