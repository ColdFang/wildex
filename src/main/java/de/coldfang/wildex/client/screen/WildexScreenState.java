package de.coldfang.wildex.client.screen;

public final class WildexScreenState {

    private String selectedMobId = "";
    private WildexTab selectedTab = WildexTab.STATS;

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
}
