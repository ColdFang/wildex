package de.coldfang.wildex.client.screen;

import de.coldfang.wildex.Wildex;
import de.coldfang.wildex.client.WildexClientConfigView;
import de.coldfang.wildex.client.data.WildexCompletionCache;
import de.coldfang.wildex.client.data.WildexDiscoveryCache;
import de.coldfang.wildex.client.data.WildexMobDataResolver;
import de.coldfang.wildex.client.data.WildexMobIndexModel;
import de.coldfang.wildex.client.data.WildexPlayerUiStateCache;
import de.coldfang.wildex.client.data.model.WildexMobData;
import de.coldfang.wildex.client.WildexNetworkClient;
import de.coldfang.wildex.config.ClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class WildexScreen extends Screen {

    private static final Component TITLE = Component.translatable("screen.wildex.title");
    private static final Component SEARCH_LABEL = Component.translatable("gui.wildex.search");
    private static final Component SHARE_BUTTON_LABEL = Component.translatable("gui.wildex.share.button");

    private static final int LIST_GAP = 10;
    private static final int LIST_BOTTOM_CUT = 80;


    private static final Component RESET_PREVIEW_TOOLTIP = Component.translatable("tooltip.wildex.reset_preview");
    private static final Component DISCOVERED_ONLY_TOOLTIP = Component.translatable("tooltip.wildex.discovered_only");
    private static final Component PREVIEW_CONTROLS_LABEL = Component.translatable("gui.wildex.preview_controls_hint");
    private static final List<Component> PREVIEW_CONTROLS_TOOLTIP = List.of(
            Component.translatable("tooltip.wildex.preview_controls.title"),
            Component.translatable("tooltip.wildex.preview_controls.line1"),
            Component.translatable("tooltip.wildex.preview_controls.line2")
    );

    private static final int STYLE_BUTTON_W = 56;
    private static final int STYLE_BUTTON_H = 14;
    private static final int STYLE_BUTTON_MARGIN = 6;
    private static final int STYLE_BUTTON_Y_OFFSET = 2;

    private static final int UI_SLIDER_W = 150;
    private static final int UI_SLIDER_H = 12;
    private static final int UI_SLIDER_KNOB_W = 10;
    private static final int UI_SLIDER_X = 8;
    private static final int UI_SLIDER_Y = 8;
    private static final float UI_SLIDER_RENDER_SCALE = 2.0f;
    private static final float UI_SLIDER_LABEL_SCALE = 2.0f;

    private static final float VERSION_SCALE = 0.55f;
    private static final float PREVIEW_HINT_SCALE = 0.62f;

    private final WildexScreenState state = new WildexScreenState();
    private final WildexBookRenderer renderer = new WildexBookRenderer();
    private final WildexMobPreviewRenderer mobPreviewRenderer = new WildexMobPreviewRenderer();
    private final WildexMobIndexModel mobIndex = new WildexMobIndexModel();

    private final WildexMobDataResolver mobDataResolver = new WildexMobDataResolver();
    private final WildexRightInfoRenderer rightInfoRenderer = new WildexRightInfoRenderer();
    private final WildexRightHeaderRenderer rightHeaderRenderer = new WildexRightHeaderRenderer();
    private final WildexTrophyRenderer trophyRenderer = new WildexTrophyRenderer();

    private WildexScreenLayout layout;

    private WildexSearchBox searchBox;
    private MobListWidget mobList;
    private MobPreviewResetButton previewResetButton;
    private WildexDiscoveredOnlyCheckbox discoveredOnlyCheckbox;
    private WildexStyleButton styleButton;
    private RightTabsWidget rightTabsWidget;
    private WildexShareOverlayController shareOverlay;

    private List<EntityType<?>> visibleEntries = List.of();
    private boolean suppressMobSelectionCallback = false;
    private boolean suppressUiStateSync = false;
    private boolean localUiStateDirtySinceOpen = false;

    private WildexTab lastTab = WildexTab.STATS;
    private int lastDiscoveryCount = -1;
    private final String versionLabel = resolveVersionLabel();

    private float cachedGuiScale = 1.0f;
    private int physicalScreenW = 1;
    private int physicalScreenH = 1;
    private WildexScreenSpace screenSpace;
    private boolean draggingUiScaleSlider = false;
    private int uiSliderX = UI_SLIDER_X;
    private int uiSliderY = UI_SLIDER_Y;
    private int uiSliderW = UI_SLIDER_W;
    private int uiSliderH = UI_SLIDER_H;
    private int uiSliderKnobW = UI_SLIDER_KNOB_W;
    private int currentMobListItemHeight = MobListWidget.itemHeightForUiScale(WildexUiScale.get());

    private int scaledTopButtonW() {
        return Math.max(20, Math.round(STYLE_BUTTON_W * WildexUiScale.get()));
    }

    private int scaledTopButtonH() {
        return Math.max(10, Math.round(STYLE_BUTTON_H * WildexUiScale.get()));
    }

    private int scaledTopButtonMargin() {
        return Math.max(2, Math.round(STYLE_BUTTON_MARGIN * WildexUiScale.get()));
    }

    private int scaledTopButtonYOffset() {
        return Math.round(STYLE_BUTTON_Y_OFFSET * WildexUiScale.get());
    }

    public WildexScreen() {
        super(TITLE);
    }

    @Override
    protected void init() {
        Minecraft mc = Minecraft.getInstance();
        this.layout = WildexScreenLayout.compute(
                Math.max(1, mc.getWindow().getWidth()),
                Math.max(1, mc.getWindow().getHeight()),
                WildexUiScale.get()
        );
        WildexUiTheme.Palette theme = WildexUiTheme.current();
        this.state.setSelectedTab(WildexTab.STATS);
        this.state.setSelectedMobId("");
        this.rightInfoRenderer.resetSpawnScroll();
        this.rightInfoRenderer.resetStatsScroll();
        this.rightInfoRenderer.resetLootScroll();

        WildexScreenLayout.Area styleButtonArea = this.layout.styleButtonArea(
                scaledTopButtonW(),
                scaledTopButtonH(),
                scaledTopButtonMargin(),
                scaledTopButtonYOffset()
        );

        this.clearWidgets();

        this.styleButton = this.addRenderableWidget(new WildexStyleButton(
                styleButtonArea.x(),
                styleButtonArea.y(),
                styleButtonArea.w(),
                styleButtonArea.h(),
                () -> {
            var next = WildexThemes.nextStyle(ClientConfig.INSTANCE.designStyle.get());
            ClientConfig.INSTANCE.designStyle.set(next);
            ClientConfig.SPEC.save();
            mobDataResolver.clearCache();
            // Recreate the screen so all layout metrics/widgets are rebuilt from the selected theme.
            if (this.minecraft != null) {
                this.minecraft.setScreen(new WildexScreen());
            }
        }));
        if (WildexClientConfigView.hiddenMode()) {
            WildexNetworkClient.requestDiscoveredMobs();
        }

        WildexScreenLayout.Area search = layout.leftSearchArea();

        this.searchBox = new WildexSearchBox(this.font, search.x(), search.y(), search.w(), search.h(), SEARCH_LABEL);
        this.searchBox.setMaxLength(64);
        this.searchBox.setValue(mobIndex.query());
        this.searchBox.setResponder(this::onSearchChanged);

        this.searchBox.setTextColor(theme.ink());
        this.searchBox.setTextColorUneditable(theme.ink());
        this.searchBox.setBordered(false);
        this.searchBox.setTextShadow(false);

        this.addRenderableWidget(this.searchBox);

        WildexScreenLayout.Area content = layout.leftContentArea();
        WildexScreenLayout.Area action = layout.leftActionArea();

        if (WildexClientConfigView.hiddenMode()) {
            int size = Math.max(8, action.h());
            int cbX = (action.x() + action.w()) - size;
            int cbY = action.y() + ((action.h() - size) / 2);

            this.discoveredOnlyCheckbox = new WildexDiscoveredOnlyCheckbox(
                    cbX,
                    cbY,
                    size,
                    false,
                    checked -> {
                        applyFiltersFromUi();
                        refreshMobList();
                    },
                    DISCOVERED_ONLY_TOOLTIP
            );
            this.addRenderableWidget(this.discoveredOnlyCheckbox);
        } else {
            this.discoveredOnlyCheckbox = null;
        }

        int listGap = Math.round(LIST_GAP * layout.scale());
        int bottomCut = Math.round(LIST_BOTTOM_CUT * layout.scale());

        int listX = content.x();
        int listY = action.y() + action.h() + listGap;
        int listW = content.w();
        int listH = Math.max(1, (content.y() + content.h()) - listY - bottomCut);

        this.mobList = new MobListWidget(
                Minecraft.getInstance(),
                listX,
                listY,
                listW,
                listH,
                this.currentMobListItemHeight,
                this::onMobSelected,
                this::onDebugDiscoverMob
        );
        this.addRenderableWidget(this.mobList);

        String restoredSelectedMobId = this.state.selectedMobId();
        ResourceLocation restoredSelectedRl = ResourceLocation.tryParse(restoredSelectedMobId == null ? "" : restoredSelectedMobId);

        suppressMobSelectionCallback = true;
        try {
            applyFiltersFromUi();
            refreshMobList();
            if (restoredSelectedRl != null) this.mobList.setSelectedId(restoredSelectedRl);
            ResourceLocation selected = this.mobList.selectedId();
            this.state.setSelectedMobId(selected == null ? "" : selected.toString());
        } finally {
            suppressMobSelectionCallback = false;
        }

        WildexScreenLayout.Area tabsArea = layout.rightTabsArea();
        this.rightTabsWidget = new RightTabsWidget(tabsArea.x(), tabsArea.y(), tabsArea.w(), tabsArea.h(), this.state);
        this.addRenderableWidget(this.rightTabsWidget);

        WildexScreenLayout.Area resetArea = layout.previewResetButtonArea();
        this.previewResetButton = new MobPreviewResetButton(
                resetArea.x(),
                resetArea.y(),
                resetArea.w(),
                resetArea.h(),
                mobPreviewRenderer::resetPreview
        );
        this.addRenderableWidget(this.previewResetButton);
        this.shareOverlay = new WildexShareOverlayController(
                this,
                this.font,
                SHARE_BUTTON_LABEL,
                STYLE_BUTTON_W,
                STYLE_BUTTON_H,
                STYLE_BUTTON_MARGIN,
                STYLE_BUTTON_Y_OFFSET,
                this::isSingleplayerShareBlocked,
                this::isShareEligibleForSelectedMob,
                () -> this.state.selectedMobId()
        );
        initShareWidgets();
        updateShareWidgetsVisibility();

        this.lastTab = this.state.selectedTab();
        this.setInitialFocus(this.searchBox);

        this.lastDiscoveryCount = WildexClientConfigView.hiddenMode() ? WildexDiscoveryCache.count() : -1;

        this.localUiStateDirtySinceOpen = false;
        if (WildexPlayerUiStateCache.hasServerState()) {
            applyServerUiState(WildexPlayerUiStateCache.tabId(), WildexPlayerUiStateCache.mobId());
        }
        WildexNetworkClient.requestServerConfig();
        WildexNetworkClient.requestPlayerUiState();
        requestAllForSelected(this.state.selectedMobId());
    }

    private void initShareWidgets() {
        if (this.shareOverlay == null) return;
        this.shareOverlay.initWidgets(this.layout);
    }

    private void updateShareWidgetsVisibility() {
        if (this.shareOverlay == null) return;
        this.shareOverlay.refreshVisibility();
        boolean blocked = this.shareOverlay.blocksRightInfo();
        if (this.rightTabsWidget != null) this.rightTabsWidget.visible = !blocked;
        if (this.previewResetButton != null) this.previewResetButton.visible = true;
    }

    private boolean isShareEligibleForSelectedMob() {
        ResourceLocation mobId = ResourceLocation.tryParse(this.state.selectedMobId());
        if (mobId == null) return false;

        if (!WildexClientConfigView.hiddenMode()) return true;
        return WildexDiscoveryCache.isDiscovered(mobId);
    }

    private void closeShareOverlayIfOpen() {
        if (this.shareOverlay == null) return;
        this.shareOverlay.closeOverlayIfOpen();
    }

    public void onShareCandidatesUpdated() {
        if (this.shareOverlay == null) return;
        this.shareOverlay.onShareCandidatesUpdated();
        updateShareWidgetsVisibility();
    }

    public void onSharePayoutStatusUpdated() {
        if (this.shareOverlay == null) return;
        this.shareOverlay.onSharePayoutStatusUpdated();
        updateShareWidgetsVisibility();
    }

    public void onServerConfigUpdated() {
        if (this.shareOverlay != null) {
            this.shareOverlay.refreshVisibility();
        }
        updateShareWidgetsVisibility();
        if (WildexClientConfigView.hiddenMode()) {
            WildexNetworkClient.requestDiscoveredMobs();
        }
    }

    private void onSearchChanged(String ignored) {
        applyFiltersFromUi();
        refreshMobList();
    }

    private void requestAllForSelected(String mobId) {
        if (mobId == null || mobId.isBlank()) return;
        WildexNetworkClient.requestKillsForSelected(mobId);
        WildexNetworkClient.requestLootForSelected(mobId);
        WildexNetworkClient.requestSpawnsForSelected(mobId);
    }

    private void applyFiltersFromUi() {
        String q = this.searchBox == null ? "" : this.searchBox.getValue();
        this.mobIndex.setQuery(q);

        boolean onlyDiscovered = this.discoveredOnlyCheckbox != null && this.discoveredOnlyCheckbox.isChecked();
        if (!onlyDiscovered) {
            this.visibleEntries = this.mobIndex.filtered();
            return;
        }

        List<EntityType<?>> base = this.mobIndex.filtered();
        List<EntityType<?>> out = new ArrayList<>(base.size());
        for (EntityType<?> type : base) {
            ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
            if (WildexDiscoveryCache.isDiscovered(id)) out.add(type);
        }
        this.visibleEntries = List.copyOf(out);
    }

    private void refreshMobList() {
        if (this.mobList == null) return;
        this.mobList.setEntries(this.visibleEntries);
        this.mobList.setScrollAmount(0);
    }

    @Override
    public void tick() {
        super.tick();
        updateShareWidgetsVisibility();
        if (this.shareOverlay != null) {
            this.shareOverlay.tick();
        }

        if (WildexClientConfigView.hiddenMode()) {
            int now = WildexDiscoveryCache.count();
            if (now != this.lastDiscoveryCount) {
                this.lastDiscoveryCount = now;

                ResourceLocation keep = this.mobList == null ? null : this.mobList.selectedId();

                applyFiltersFromUi();
                refreshMobList();

                if (keep != null && this.mobList != null) {
                    this.mobList.setSelectedId(keep);
                }
            }
        }

        WildexTab tabNow = this.state.selectedTab();
        if (tabNow != this.lastTab) {
            this.lastTab = tabNow;
            saveUiStateToServer();
            requestAllForSelected(this.state.selectedMobId());
        }

        if (this.mobList == null) return;

        ResourceLocation id = this.mobList.selectedId();
        String next = id == null ? "" : id.toString();

        if (!next.equals(this.state.selectedMobId())) {
        closeShareOverlayIfOpen();
        this.state.setSelectedMobId(next);
        rightInfoRenderer.resetSpawnScroll();
        rightInfoRenderer.resetStatsScroll();
        rightInfoRenderer.resetLootScroll();
        saveUiStateToServer();
        requestAllForSelected(next);
    }
    }

    private void onMobSelected(ResourceLocation id) {
        if (suppressMobSelectionCallback) return;
        if (this.shareOverlay != null) this.shareOverlay.onSelectionChanged();
        updateShareWidgetsVisibility();

        String next = id == null ? "" : id.toString();
        state.setSelectedMobId(next);
        rightInfoRenderer.resetSpawnScroll();
        rightInfoRenderer.resetStatsScroll();
        saveUiStateToServer();
        requestAllForSelected(next);
    }

    private void onDebugDiscoverMob(ResourceLocation mobId) {
        if (mobId == null) return;
        WildexNetworkClient.sendDebugDiscoverMob(mobId);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        double mxD = toPhysicalMouseXD(mouseX);
        double myD = toPhysicalMouseYD(mouseY);
        int mx = (int) Math.floor(mxD);
        int my = (int) Math.floor(myD);

        if (this.shareOverlay != null && this.layout != null && this.shareOverlay.handleDropdownMouseScrolled(mx, my, scrollY, this.layout)) {
            return true;
        }

        if (mobPreviewRenderer.isMouseOverPreview(this.layout, mx, my)) {
            mobPreviewRenderer.adjustZoom(scrollY);
            return true;
        }

        if (this.layout != null && this.state.selectedTab() == WildexTab.STATS) {
            if (this.shareOverlay != null && this.shareOverlay.isPanelOpen()) return super.mouseScrolled(mxD, myD, scrollX, scrollY);
            if (rightInfoRenderer.scrollStats(mx, my, scrollY)) {
                return true;
            }
        }

        if (this.layout != null && this.state.selectedTab() == WildexTab.SPAWNS) {
            if (this.shareOverlay != null && this.shareOverlay.isPanelOpen()) return super.mouseScrolled(mxD, myD, scrollX, scrollY);
            WildexScreenLayout.Area a = this.layout.rightInfoArea();
            if (a != null && mx >= a.x() && mx < a.x() + a.w() && my >= a.y() && my < a.y() + a.h()) {
                rightInfoRenderer.scrollSpawn(scrollY);
                return true;
            }
        }
        if (this.layout != null && this.state.selectedTab() == WildexTab.LOOT) {
            if (this.shareOverlay != null && this.shareOverlay.isPanelOpen()) return super.mouseScrolled(mxD, myD, scrollX, scrollY);
            if (rightInfoRenderer.scrollLoot(mx, my, scrollY)) {
                return true;
            }
        }

        return super.mouseScrolled(mxD, myD, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double mxD = toPhysicalMouseXD(mouseX);
        double myD = toPhysicalMouseYD(mouseY);
        int mx = (int) Math.floor(mxD);
        int my = (int) Math.floor(myD);

        if (button == 0 && isOnUiScaleSlider(mx, my)) {
            this.draggingUiScaleSlider = true;
            setUiScaleFromSliderMouse(mx);
            return true;
        }

        if (this.shareOverlay != null && this.layout != null && this.shareOverlay.handleDropdownMouseClicked(mx, my, button, this.layout)) {
            return true;
        }

        if (super.mouseClicked(mxD, myD, button)) return true;

        if (this.layout != null) {
            if (mobPreviewRenderer.isMouseOverPreview(this.layout, mx, my) && mobPreviewRenderer.beginRotationDrag(mx, my, button)) {
                return true;
            }
        }

        if (this.layout != null && this.state.selectedTab() == WildexTab.SPAWNS) {
            if (this.shareOverlay != null && this.shareOverlay.isPanelOpen()) return false;
            if (rightInfoRenderer.handleSpawnMouseClicked(mx, my, button, this.state)) {
                return true;
            }
        }
        if (this.layout != null && this.state.selectedTab() == WildexTab.STATS) {
            if (this.shareOverlay != null && this.shareOverlay.isPanelOpen()) return false;
            if (rightInfoRenderer.handleStatsMouseClicked(mx, my, button)) {
                return true;
            }
        }
        if (this.layout != null && this.state.selectedTab() == WildexTab.LOOT) {
            if (this.shareOverlay != null && this.shareOverlay.isPanelOpen()) return false;
            if (rightInfoRenderer.handleLootMouseClicked(mx, my, button)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        double mxD = toPhysicalMouseXD(mouseX);
        double myD = toPhysicalMouseYD(mouseY);
        int mx = (int) Math.floor(mxD);
        int my = (int) Math.floor(myD);
        double dragXD = dragX * this.cachedGuiScale;
        double dragYD = dragY * this.cachedGuiScale;

        if (button == 0 && this.draggingUiScaleSlider) {
            setUiScaleFromSliderMouse(mx);
            return true;
        }

        if (super.mouseDragged(mxD, myD, button, dragXD, dragYD)) return true;

        if (this.layout != null) {
            if (mobPreviewRenderer.updateRotationDrag(mx, my, button)) {
                return true;
            }
        }

        if (this.layout != null && this.state.selectedTab() == WildexTab.SPAWNS) {
            if (this.shareOverlay != null && this.shareOverlay.isPanelOpen()) return false;
            if (rightInfoRenderer.handleSpawnMouseDragged(mx, my, button)) {
                return true;
            }
        }
        if (this.layout != null && this.state.selectedTab() == WildexTab.STATS) {
            if (this.shareOverlay != null && this.shareOverlay.isPanelOpen()) return false;
            if (rightInfoRenderer.handleStatsMouseDragged(mx, my, button)) {
                return true;
            }
        }
        if (this.layout != null && this.state.selectedTab() == WildexTab.LOOT) {
            if (this.shareOverlay != null && this.shareOverlay.isPanelOpen()) return false;
            if (rightInfoRenderer.handleLootMouseDragged(mx, my, button)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        double mxD = toPhysicalMouseXD(mouseX);
        double myD = toPhysicalMouseYD(mouseY);
        int mx = (int) Math.floor(mxD);
        int my = (int) Math.floor(myD);

        if (button == 0 && this.draggingUiScaleSlider) {
            this.draggingUiScaleSlider = false;
            return true;
        }

        if (super.mouseReleased(mxD, myD, button)) return true;

        if (mobPreviewRenderer.endRotationDrag(button)) {
            return true;
        }

        if (this.layout != null && this.state.selectedTab() == WildexTab.SPAWNS) {
            if (this.shareOverlay != null && this.shareOverlay.isPanelOpen()) return false;
            if (rightInfoRenderer.handleSpawnMouseReleased(button)) {
                return true;
            }
        }
        if (this.layout != null && this.state.selectedTab() == WildexTab.STATS) {
            if (this.shareOverlay != null && this.shareOverlay.isPanelOpen()) return false;
            if (rightInfoRenderer.handleStatsMouseReleased(button)) {
                return true;
            }
        }
        if (this.layout != null && this.state.selectedTab() == WildexTab.LOOT) {
            if (this.shareOverlay != null && this.shareOverlay.isPanelOpen()) return false;
            if (rightInfoRenderer.handleLootMouseReleased(button)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void renderBackground(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void renderTransparentBackground(@NotNull GuiGraphics graphics) {
    }

    @Override
    protected void renderBlurredBackground(float partialTick) {
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        this.screenSpace = WildexScreenSpace.fromWindow(mc);
        this.cachedGuiScale = this.screenSpace.guiScale();
        this.physicalScreenW = this.screenSpace.physicalWidth();
        this.physicalScreenH = this.screenSpace.physicalHeight();
        int physicalMouseX = this.screenSpace.toPhysicalX(mouseX);
        int physicalMouseY = this.screenSpace.toPhysicalY(mouseY);

        this.layout = WildexScreenLayout.compute(this.physicalScreenW, this.physicalScreenH, WildexUiScale.get());
        syncMobListItemHeightIfNeeded();
        syncWidgetPositions();

        this.screenSpace.pushInverseScale(graphics);

        WildexUiTheme.Palette theme = WildexUiTheme.current();
        applyThemeToInputs(theme);
        syncShareTopButtonsPosition();

        WildexTrophyRenderer.Hitbox trophyHitbox = trophyRenderer.render(
                graphics,
                this.layout,
                WildexClientConfigView.hiddenMode() && WildexCompletionCache.isComplete()
        );

        renderer.render(graphics, this.layout, state, physicalMouseX, physicalMouseY, partialTick);
        renderVersionLabel(graphics);

        WildexScreenLayout.Area entriesArea = this.layout.leftEntriesCounterArea();
        Component entriesText = Component.translatable("gui.wildex.entries_count", (this.visibleEntries == null ? 0 : this.visibleEntries.size()));
        float entriesScale = resolveEntriesTextScale();
        int textW = Math.max(1, Math.round(WildexUiText.width(font, entriesText) * entriesScale));
        int textH = Math.max(1, Math.round(WildexUiText.lineHeight(font) * entriesScale));
        int entriesX = this.layout.leftSearchArea().x();
        int entriesY = entriesArea.y() + ((entriesArea.h() - textH) / 2) + 1;
        int maxEntriesY = this.layout.leftSearchArea().y() - textH - 1;
        entriesY = Math.min(entriesY, maxEntriesY);
        WildexUiRenderUtil.drawScaledText(graphics, this.font, entriesText, entriesX, entriesY, entriesScale, theme.ink());

        if (WildexClientConfigView.hiddenMode()) {
            WildexScreenLayout.Area discArea = this.layout.leftDiscoveryCounterArea();

            int discovered = WildexDiscoveryCache.count();
            int total = mobIndex.totalCount();

            Component discText = Component.translatable("gui.wildex.discovered_count", discovered, total);
            float discScale = resolveEntriesTextScale();
            int discTextH = Math.max(1, Math.round(WildexUiText.lineHeight(font) * discScale));

            int discX = discArea.x();
            int discY = discArea.y() + ((discArea.h() - discTextH) / 2) + Math.max(1, Math.round(2 * WildexUiScale.get()));
            WildexUiRenderUtil.drawScaledText(graphics, this.font, discText, discX, discY, discScale, theme.ink());
        }

        mobPreviewRenderer.render(graphics, this.layout, state, physicalMouseX, physicalMouseY, partialTick);
        WildexScreenLayout.Area previewArea = this.layout.rightPreviewArea();
        WildexScreenLayout.Area previewResetArea = this.layout.previewResetButtonArea();
        HintBounds previewHint = drawPreviewControlsHint(graphics, this.layout, previewArea, previewResetArea);

        boolean showShareNotice = this.shareOverlay != null && this.shareOverlay.shouldShowNotice();
        boolean showSharePanel = this.shareOverlay != null && this.shareOverlay.isPanelVisible();
        if (showSharePanel) {
            WildexUiRenderUtil.drawPanelFrame(graphics, this.layout.sharePanelArea(), theme);
            renderSharePanel(graphics);
        } else if (showShareNotice) {
            WildexUiRenderUtil.drawPanelFrame(graphics, this.layout.rightTabsArea(), theme);
            renderShareSingleplayerNotice(graphics);
        } else {
            WildexUiRenderUtil.drawPanelFrame(graphics, this.layout.rightTabsArea(), theme);
        }

        WildexMobData data = mobDataResolver.resolve(state.selectedMobId());

        rightHeaderRenderer.render(graphics, this.font, this.layout.rightHeaderArea(), state, data.header(), theme.ink());
        if (WildexThemes.isVintageLayout()) {
            WildexUiRenderUtil.drawRoundedPanelFrame(graphics, this.layout.rightHeaderArea(), theme, 3);
        }

        if (!showSharePanel && !showShareNotice) {
            rightInfoRenderer.render(
                    graphics,
                    this.font,
                    this.layout.rightInfoArea(),
                    state,
                    data,
                    theme.ink(),
                    physicalMouseX,
                    physicalMouseY
            );
        }

        drawUiScaleSlider(graphics, physicalMouseX, physicalMouseY);

        super.render(graphics, physicalMouseX, physicalMouseY, partialTick);
        renderMobListTopDivider(graphics);

        if (this.previewResetButton != null && this.previewResetButton.isHovered()) {
            WildexUiRenderUtil.renderTooltip(graphics, this.font, List.of(RESET_PREVIEW_TOOLTIP), physicalMouseX, physicalMouseY, this.physicalScreenW, this.physicalScreenH, theme);
        }

        if (this.discoveredOnlyCheckbox != null && this.discoveredOnlyCheckbox.isHovered()) {
            Component tip = this.discoveredOnlyCheckbox.tooltip();
            if (tip != null) WildexUiRenderUtil.renderTooltip(graphics, this.font, List.of(tip), physicalMouseX, physicalMouseY, this.physicalScreenW, this.physicalScreenH, theme);
        }

        if (trophyHitbox != null && trophyHitbox.contains(physicalMouseX, physicalMouseY)) {
            WildexUiRenderUtil.renderTooltip(graphics, this.font, trophyRenderer.tooltip(), physicalMouseX, physicalMouseY, this.physicalScreenW, this.physicalScreenH, theme);
        }

        if (!mobPreviewRenderer.isDraggingPreview() && isMouseOverPreviewControlsHint(physicalMouseX, physicalMouseY, previewHint)) {
            WildexUiRenderUtil.renderTooltip(graphics, this.font, PREVIEW_CONTROLS_TOOLTIP, physicalMouseX, physicalMouseY, this.physicalScreenW, this.physicalScreenH, theme);
        }
        if (this.shareOverlay != null && this.shareOverlay.isShareEntryButtonHovered()) {
            WildexUiRenderUtil.renderTooltip(graphics, this.font, List.of(this.shareOverlay.shareTooltip()), physicalMouseX, physicalMouseY, this.physicalScreenW, this.physicalScreenH, theme);
        }
        if (this.shareOverlay != null && this.shareOverlay.isShareClaimButtonHovered()) {
            WildexUiRenderUtil.renderTooltip(graphics, this.font, List.of(this.shareOverlay.shareClaimTooltip()), physicalMouseX, physicalMouseY, this.physicalScreenW, this.physicalScreenH, theme);
        }
        if (this.shareOverlay != null && this.shareOverlay.isShareOpenOffersHovered()) {
            WildexUiRenderUtil.renderTooltip(graphics, this.font, List.of(this.shareOverlay.shareOpenOffersTooltip()), physicalMouseX, physicalMouseY, this.physicalScreenW, this.physicalScreenH, theme);
        }

        this.screenSpace.popScale(graphics);
    }

    private boolean isSingleplayerSession() {
        Minecraft mc = Minecraft.getInstance();
        return mc.hasSingleplayerServer() && mc.getCurrentServer() == null;
    }

    private boolean isSingleplayerShareBlocked() {
        return isSingleplayerSession() && !WildexClientConfigView.debugMode();
    }

    private void renderMobListTopDivider(GuiGraphics graphics) {
        if (this.mobList == null || !this.mobList.visible) return;
        WildexUiTheme.Palette theme = WildexUiTheme.current();
        int x0 = this.mobList.getX();
        int x1 = x0 + this.mobList.getWidth() - 6; // keep aligned with list content area (excluding scrollbar)
        int y = this.mobList.getY();
        if (x1 <= x0) return;
        graphics.fill(x0, y, x1, y + 1, theme.frameOuter());
        graphics.fill(x0 + 1, y + 1, x1 - 1, y + 2, theme.frameInner());
    }

    private void applyThemeToInputs(WildexUiTheme.Palette theme) {
        if (theme == null) return;
        if (this.searchBox != null) {
            this.searchBox.setTextColor(theme.ink());
            this.searchBox.setTextColorUneditable(theme.ink());
        }
        if (this.shareOverlay != null) this.shareOverlay.applyThemeToInputs(theme);
    }

    private void syncShareTopButtonsPosition() {
        if (this.layout == null || this.shareOverlay == null) return;
        this.shareOverlay.syncTopButtonsPosition(this.layout);
    }

    private void renderShareSingleplayerNotice(GuiGraphics graphics) {
        if (this.shareOverlay == null || this.layout == null) return;
        this.shareOverlay.renderSingleplayerNotice(graphics, this.layout);
    }

    private void renderSharePanel(GuiGraphics graphics) {
        if (this.shareOverlay == null || this.layout == null) return;
        this.shareOverlay.renderPanel(graphics, this.layout);
    }

    private HintBounds drawPreviewControlsHint(
            GuiGraphics graphics,
            WildexScreenLayout screenLayout,
            WildexScreenLayout.Area previewArea,
            WildexScreenLayout.Area previewResetArea
    ) {
        if (previewArea == null || screenLayout == null) return null;

        WildexScreenLayout.PreviewControlsHintAnchor anchor = screenLayout.previewControlsHintAnchor();
        int hintBaseX = anchor.x();
        int textW = WildexUiText.width(font, PREVIEW_CONTROLS_LABEL);
        int scaledTextW = Math.max(1, Math.round(textW * PREVIEW_HINT_SCALE));
        int scaledTextH = Math.max(1, Math.round(WildexUiText.lineHeight(font) * PREVIEW_HINT_SCALE));
        int hintBaseY = anchor.bottomY() - scaledTextH;
        if (anchor.alignCenterToResetButton() && previewResetArea != null) {
            hintBaseY = previewResetArea.y() + Math.max(0, (previewResetArea.h() - scaledTextH) / 2);
        }
        int drawX = hintBaseX;
        float inv = 1.0f / PREVIEW_HINT_SCALE;

        int rightBound = anchor.rightBoundX();
        int availableW = Math.max(0, rightBound - hintBaseX);
        if (availableW <= 2) return null;

        graphics.pose().pushPose();
        WildexScissor.enablePhysical(graphics, hintBaseX, hintBaseY, hintBaseX + availableW, hintBaseY + scaledTextH + 1);
        graphics.pose().scale(PREVIEW_HINT_SCALE, PREVIEW_HINT_SCALE, 1.0f);
        WildexUiText.draw(graphics, 
                this.font,
                PREVIEW_CONTROLS_LABEL,
                Math.round(drawX * inv),
                Math.round(hintBaseY * inv),
                WildexUiTheme.current().inkMuted(),
                false
        );
        graphics.disableScissor();
        graphics.pose().popPose();

        return new HintBounds(hintBaseX, hintBaseY, Math.min(scaledTextW, availableW), scaledTextH);
    }

    private void renderVersionLabel(GuiGraphics graphics) {
        if (this.versionLabel.isBlank() || this.layout == null) return;

        int scaledTextW = Math.max(1, Math.round(WildexUiText.width(font, this.versionLabel) * VERSION_SCALE));
        int scaledTextH = Math.max(1, Math.round(WildexUiText.lineHeight(font) * VERSION_SCALE));
        WildexScreenLayout.Area versionArea = this.layout.versionLabelArea(
                scaledTopButtonW(),
                scaledTopButtonH(),
                scaledTopButtonMargin(),
                scaledTopButtonYOffset(),
                scaledTextW,
                scaledTextH
        );

        float inv = 1.0f / VERSION_SCALE;

        graphics.pose().pushPose();
        graphics.pose().scale(VERSION_SCALE, VERSION_SCALE, 1.0f);
        WildexUiText.draw(graphics, this.font, this.versionLabel, Math.round(versionArea.x() * inv), Math.round(versionArea.y() * inv), WildexUiTheme.current().inkMuted(), false);
        graphics.pose().popPose();
    }

    private static boolean isMouseOverRect(int mouseX, int mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }

    <T extends GuiEventListener & Renderable & NarratableEntry> T addShareWidget(T widget) {
        return this.addRenderableWidget(widget);
    }

    private static boolean isMouseOverPreviewControlsHint(int mouseX, int mouseY, HintBounds hint) {
        if (hint == null || hint.w() <= 0 || hint.h() <= 0) return false;
        return mouseX >= hint.x() && mouseX < hint.x() + hint.w() && mouseY >= hint.y() && mouseY < hint.y() + hint.h();
    }

    private record HintBounds(int x, int y, int w, int h) {
    }


    @Override
    public void onClose() {
        mobPreviewRenderer.clear();
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static String resolveVersionLabel() {
        String v = ModList.get()
                .getModContainerById(Wildex.MODID)
                .map(c -> c.getModInfo().getVersion().toString())
                .orElse("unknown");
        return "v" + v;
    }

    private float resolveEntriesTextScale() {
        float s = WildexUiScale.get();
        if (s <= 0.62f) return 0.72f;
        if (s <= 0.74f) return 0.80f;
        if (s <= 0.86f) return 0.90f;
        return 1.0f;
    }

    private double toPhysicalMouseXD(double mouseX) {
        if (this.screenSpace != null) return mouseX * this.screenSpace.guiScale();
        return mouseX * this.cachedGuiScale;
    }

    private double toPhysicalMouseYD(double mouseY) {
        if (this.screenSpace != null) return mouseY * this.screenSpace.guiScale();
        return mouseY * this.cachedGuiScale;
    }

    private void syncMobListItemHeightIfNeeded() {
        if (this.layout == null || this.mobList == null) return;
        int next = MobListWidget.itemHeightForUiScale(WildexUiScale.get());
        if (next == this.currentMobListItemHeight) return;

        this.currentMobListItemHeight = next;
        ResourceLocation keepSelected = this.mobList.selectedId();

        this.removeWidget(this.mobList);

        WildexScreenLayout.Area content = this.layout.leftContentArea();
        WildexScreenLayout.Area action = this.layout.leftActionArea();
        int listGap = Math.round(LIST_GAP * this.layout.scale());
        int bottomCut = Math.round(LIST_BOTTOM_CUT * this.layout.scale());

        int listX = content.x();
        int listY = action.y() + action.h() + listGap;
        int listW = content.w();
        int listH = Math.max(1, (content.y() + content.h()) - listY - bottomCut);

        this.mobList = new MobListWidget(
                Minecraft.getInstance(),
                listX,
                listY,
                listW,
                listH,
                this.currentMobListItemHeight,
                this::onMobSelected,
                this::onDebugDiscoverMob
        );
        this.addRenderableWidget(this.mobList);
        this.mobList.setEntries(this.visibleEntries);
        if (keepSelected != null) this.mobList.setSelectedId(keepSelected);
    }

    private void syncWidgetPositions() {
        if (this.layout == null) return;

        WildexScreenLayout.Area styleButtonArea = this.layout.styleButtonArea(
                scaledTopButtonW(),
                scaledTopButtonH(),
                scaledTopButtonMargin(),
                scaledTopButtonYOffset()
        );
        if (this.styleButton != null) {
            this.styleButton.setX(styleButtonArea.x());
            this.styleButton.setY(styleButtonArea.y());
            this.styleButton.setWidth(styleButtonArea.w());
            this.styleButton.setHeight(styleButtonArea.h());
        }

        if (this.searchBox != null) {
            WildexScreenLayout.Area a = this.layout.leftSearchArea();
            this.searchBox.setX(a.x());
            this.searchBox.setY(a.y());
            this.searchBox.setWidth(a.w());
            this.searchBox.setHeight(a.h());
        }

        if (this.discoveredOnlyCheckbox != null) {
            WildexScreenLayout.Area action = this.layout.leftActionArea();
            int size = Math.max(8, action.h());
            int cbX = (action.x() + action.w()) - size;
            int cbY = action.y() + ((action.h() - size) / 2);
            this.discoveredOnlyCheckbox.setX(cbX);
            this.discoveredOnlyCheckbox.setY(cbY);
            this.discoveredOnlyCheckbox.setWidth(size);
            this.discoveredOnlyCheckbox.setHeight(size);
        }

        if (this.mobList != null) {
            WildexScreenLayout.Area content = this.layout.leftContentArea();
            WildexScreenLayout.Area action = this.layout.leftActionArea();
            int listGap = Math.round(LIST_GAP * this.layout.scale());
            int bottomCut = Math.round(LIST_BOTTOM_CUT * this.layout.scale());

            int listX = content.x();
            int listY = action.y() + action.h() + listGap;
            int listW = content.w();
            int listH = Math.max(1, (content.y() + content.h()) - listY - bottomCut);
            this.mobList.setX(listX);
            this.mobList.setY(listY);
            this.mobList.setWidth(listW);
            this.mobList.setHeight(listH);
        }

        if (this.rightTabsWidget != null) {
            WildexScreenLayout.Area a = this.layout.rightTabsArea();
            this.rightTabsWidget.setX(a.x());
            this.rightTabsWidget.setY(a.y());
            this.rightTabsWidget.setWidth(a.w());
            this.rightTabsWidget.setHeight(a.h());
        }

        if (this.previewResetButton != null) {
            WildexScreenLayout.Area a = this.layout.previewResetButtonArea();
            this.previewResetButton.setX(a.x());
            this.previewResetButton.setY(a.y());
            this.previewResetButton.setWidth(a.w());
            this.previewResetButton.setHeight(a.h());
        }

        if (this.shareOverlay != null) {
            this.shareOverlay.relayout(this.layout);
        }

    }

    private void drawUiScaleSlider(GuiGraphics graphics, int mouseX, int mouseY) {
        float s = UI_SLIDER_RENDER_SCALE;
        this.uiSliderX = UI_SLIDER_X;
        this.uiSliderY = UI_SLIDER_Y;
        this.uiSliderW = Math.max(80, Math.round(UI_SLIDER_W * s));
        this.uiSliderH = Math.max(8, Math.round(UI_SLIDER_H * s));
        this.uiSliderKnobW = Math.max(8, Math.round(UI_SLIDER_KNOB_W * s));

        WildexUiTheme.Palette theme = WildexUiTheme.current();

        graphics.fill(this.uiSliderX, this.uiSliderY, this.uiSliderX + this.uiSliderW, this.uiSliderY + this.uiSliderH, 0xFF1E1E1E);
        graphics.fill(this.uiSliderX + 1, this.uiSliderY + 1, this.uiSliderX + this.uiSliderW - 1, this.uiSliderY + this.uiSliderH - 1, 0xFF454545);

        float t = WildexUiScale.toNormalized(WildexUiScale.get());
        int travel = Math.max(1, this.uiSliderW - this.uiSliderKnobW);
        int knobX = this.uiSliderX + Math.round(travel * t);
        int knobColor = isOnUiScaleSlider(mouseX, mouseY) || this.draggingUiScaleSlider ? 0xFFECECEC : 0xFFD8D8D8;
        graphics.fill(knobX, this.uiSliderY, knobX + this.uiSliderKnobW, this.uiSliderY + this.uiSliderH, knobColor);
        graphics.fill(knobX, this.uiSliderY, knobX + this.uiSliderKnobW, this.uiSliderY + 1, 0xFF111111);
        graphics.fill(knobX, this.uiSliderY + this.uiSliderH - 1, knobX + this.uiSliderKnobW, this.uiSliderY + this.uiSliderH, 0xFF111111);

        int percent = WildexUiScale.toDisplayPercent(WildexUiScale.get());
        Component label = Component.literal("Wildex UI " + percent + "%");
        float ls = UI_SLIDER_LABEL_SCALE;
        int labelY = this.uiSliderY + this.uiSliderH + Math.round(3 * s);
        graphics.pose().pushPose();
        graphics.pose().scale(ls, ls, 1.0f);
        graphics.drawString(
                this.font,
                label,
                Math.round(this.uiSliderX / ls),
                Math.round(labelY / ls),
                theme.inkOnDark(),
                false
        );
        graphics.pose().popPose();
    }

    private boolean isOnUiScaleSlider(int mouseX, int mouseY) {
        return mouseX >= this.uiSliderX
                && mouseX < this.uiSliderX + this.uiSliderW
                && mouseY >= this.uiSliderY
                && mouseY < this.uiSliderY + this.uiSliderH;
    }

    private void setUiScaleFromSliderMouse(int mouseX) {
        int travel = Math.max(1, this.uiSliderW - this.uiSliderKnobW);
        int rel = Math.max(0, Math.min(travel, mouseX - this.uiSliderX));
        float t = rel / (float) travel;
        float next = WildexUiScale.fromNormalized(t);
        WildexUiScale.set(next);
    }

    public void applyServerUiState(String tabId, String mobId) {
        if (localUiStateDirtySinceOpen) return;

        WildexTab tab = parseTabOrDefault(tabId);
        ResourceLocation targetMob = sanitizeMobId(mobId);
        boolean useDefaultSelection = targetMob == null;

        suppressMobSelectionCallback = true;
        suppressUiStateSync = true;
        try {
            this.state.setSelectedTab(useDefaultSelection ? WildexTab.STATS : tab);

            if (this.mobList != null) {
                if (useDefaultSelection) {
                    // No persisted mob for this world/player: fall back to default first entry.
                    this.mobList.setEntries(this.visibleEntries);
                } else {
                    this.mobList.setSelectedId(targetMob);
                }
            }

            ResourceLocation selected = this.mobList == null ? null : this.mobList.selectedId();
            if (selected != null) {
                this.state.setSelectedMobId(selected.toString());
            } else if (!useDefaultSelection && targetMob != null) {
                this.state.setSelectedMobId(targetMob.toString());
            } else {
                this.state.setSelectedMobId("");
            }
        } finally {
            suppressUiStateSync = false;
            suppressMobSelectionCallback = false;
        }

        this.lastTab = this.state.selectedTab();
        this.rightInfoRenderer.resetSpawnScroll();
        this.rightInfoRenderer.resetStatsScroll();
        requestAllForSelected(this.state.selectedMobId());
    }

    private void saveUiStateToServer() {
        if (suppressUiStateSync) return;

        WildexTab tab = this.state.selectedTab();
        String mobId = this.state.selectedMobId();
        ResourceLocation safeMob = sanitizeMobId(mobId);

        localUiStateDirtySinceOpen = true;
        WildexNetworkClient.savePlayerUiState(
                (tab == null ? WildexTab.STATS : tab).name(),
                safeMob == null ? "" : safeMob.toString()
        );
    }

    private static WildexTab parseTabOrDefault(String raw) {
        if (raw == null || raw.isBlank()) return WildexTab.STATS;
        try {
            return WildexTab.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return WildexTab.STATS;
        }
    }

    private static ResourceLocation sanitizeMobId(String raw) {
        ResourceLocation rl = ResourceLocation.tryParse(raw == null ? "" : raw);
        if (rl == null) return null;
        if (!BuiltInRegistries.ENTITY_TYPE.containsKey(rl)) return null;
        return rl;
    }
}





