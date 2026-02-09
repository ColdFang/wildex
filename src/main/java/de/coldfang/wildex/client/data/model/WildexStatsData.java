package de.coldfang.wildex.client.data.model;

import java.util.OptionalDouble;

public record WildexStatsData(
        OptionalDouble maxHealth,
        OptionalDouble armor,
        OptionalDouble movementSpeed,
        OptionalDouble attackDamage,
        OptionalDouble followRange,
        OptionalDouble knockbackResistance
) {
    public static WildexStatsData empty() {
        return new WildexStatsData(
                OptionalDouble.empty(),
                OptionalDouble.empty(),
                OptionalDouble.empty(),
                OptionalDouble.empty(),
                OptionalDouble.empty(),
                OptionalDouble.empty()
        );
    }
}
