package de.coldfang.wildex.client.data.model;

public record WildexMobData(
        WildexStatsData stats,
        WildexHeaderData header
) {
    public static WildexMobData empty() {
        return new WildexMobData(WildexStatsData.empty(), WildexHeaderData.empty());
    }
}
