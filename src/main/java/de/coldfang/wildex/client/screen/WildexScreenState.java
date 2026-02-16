package de.coldfang.wildex.client.screen;

public final class WildexScreenState {

    private String selectedMobId = "";
    private WildexTab selectedTab = WildexTab.STATS;
    private boolean spawnFilterNatural = true;
    private boolean spawnFilterStructures = true;

    public String selectedMobId() {
        return selectedMobId;
    }

    public void setSelectedMobId(String id) {
        this.selectedMobId = id == null ? "" : id;
    }

    public WildexTab selectedTab() {
        return selectedTab;
    }

    public void setSelectedTab(WildexTab tab) {
        this.selectedTab = tab == null ? WildexTab.STATS : tab;
    }

    public boolean spawnFilterNatural() {
        return spawnFilterNatural;
    }

    public boolean spawnFilterStructures() {
        return spawnFilterStructures;
    }

    public void toggleSpawnFilterNatural() {
        setSpawnFilters(!spawnFilterNatural, spawnFilterStructures);
    }

    public void toggleSpawnFilterStructures() {
        setSpawnFilters(spawnFilterNatural, !spawnFilterStructures);
    }

    private void setSpawnFilters(boolean natural, boolean structures) {
        // Keep at least one source enabled so the list never becomes impossible to recover.
        if (!natural && !structures) {
            return;
        }
        this.spawnFilterNatural = natural;
        this.spawnFilterStructures = structures;
    }
}
