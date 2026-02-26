package de.coldfang.wildex.client.screen;

import de.coldfang.wildex.client.WildexClientConfigView;
import de.coldfang.wildex.client.data.WildexCompletionCache;
import de.coldfang.wildex.client.data.WildexDiscoveryCache;
import de.coldfang.wildex.client.data.WildexMobDataResolver;
import de.coldfang.wildex.client.data.WildexMobIndexModel;
import de.coldfang.wildex.client.data.WildexPlayerUiStateCache;
import de.coldfang.wildex.client.data.WildexViewedMobEntriesCache;
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
    private static final Component PREVIEW_BABY_TOOLTIP = Component.translatable("tooltip.wildex.preview_show_baby");
    private static final Component PREVIEW_ADULT_TOOLTIP = Component.translatable("tooltip.wildex.preview_show_adult");
    private static final Component DISCOVERED_ONLY_TOOLTIP = Component.translatable("tooltip.wildex.discovered_only");
    private static final Component PREVIEW_CONTROLS_LABEL = Component.translatable("gui.wildex.preview_controls_hint");
    private static final List<Component> PREVIEW_CONTROLS_TOOLTIP = List.of(
            Component.translatable("tooltip.wildex.preview_controls.title"),
            Component.translatable("tooltip.wildex.preview_controls.line1"),
            Component.translatable("tooltip.wildex.preview_controls.line2")
    );

    private static final int STYLE_BUTTON_W = 56;
    private static final int STYLE_BUTTON_H = 14;
    private static final int MENU_BUTTON_W = 68;
    private static final int MENU_BUTTON_H = 24;
    private static final int STYLE_BUTTON_MARGIN = 6;
    private static final int TOP_BUTTON_ROW_GAP = 2;
    private static final int THEME_BUTTON_Y_OFFSET = MENU_BUTTON_H + TOP_BUTTON_ROW_GAP - STYLE_BUTTON_MARGIN;
    private static final int GUI_SCALE_BUTTON_Y_OFFSET = THEME_BUTTON_Y_OFFSET + STYLE_BUTTON_H + TOP_BUTTON_ROW_GAP;

    private static final int UI_SLIDER_W = 150;
    private static final int UI_SLIDER_H = 16;
    private static final int UI_SLIDER_KNOB_W = 10;
    private static final int UI_SLIDER_RIGHT_MARGIN = 8;
    private static final float UI_SLIDER_RENDER_SCALE = 2.0f;
    private static final float UI_SLIDER_LABEL_SCALE = 2.0f;
    private static final int UI_SLIDER_TEX_SIZE = 128;
    private static final int THICK_FRAME_OUTER = 0x99D6B89C;
    private static final int THICK_FRAME_INNER = 0x77FFFFFF;
    private static final float TOP_MENU_OPEN_SPEED = 10.0f;
    private static final float TOP_MENU_CLOSE_SPEED = 16.0f;
    private static final int MODERN_MENU_SLIDER_BG = 0xFF14777D;
    private static final int MODERN_RIGHT_EDGE_OCCLUDER_TEX_W = 32;
    private static final int PREVIEW_TOGGLE_BUTTON_GAP = 2;
    private static final String PREVIEW_BABY_LABEL = "B";
    private static final String PREVIEW_ADULT_LABEL = "A";

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
    private MobPreviewResetButton previewBabyToggleButton;
    private WildexDiscoveredOnlyCheckbox discoveredOnlyCheckbox;
    private WildexStyleButton menuButton;
    private WildexStyleButton guiScaleToggleButton;
    private WildexStyleButton styleButton;
    private RightTabsWidget rightTabsWidget;
    private WildexShareOverlayController shareOverlay;

    private List<EntityType<?>> visibleEntries = List.of();
    private boolean suppressMobSelectionCallback = false;
    private boolean suppressUiStateSync = false;
    private boolean localUiStateDirtySinceOpen = false;

    private WildexTab lastTab = WildexTab.STATS;
    private int lastDiscoveryCount = -1;

    private float cachedGuiScale = 1.0f;
    private int physicalScreenW = 1;
    private int physicalScreenH = 1;
    private WildexScreenSpace screenSpace;
    private boolean draggingUiScaleSlider = false;
    private int uiSliderX = 8;
    private int uiSliderY = 8;
    private int uiSliderW = UI_SLIDER_W;
    private int uiSliderH = UI_SLIDER_H;
    private int uiSliderKnobW = UI_SLIDER_KNOB_W;
    private int currentMobListItemHeight = MobListWidget.itemHeightForUiScale(WildexUiScale.get());
    private static boolean rememberedTopMenuExpanded = false;
    private static boolean rememberedTrophyCollapsed = false;
    private boolean topMenuExpanded = false;
    private boolean trophyCollapsed = false;
    private WildexTrophyRenderer.RenderState trophyRenderState = null;
    private float topMenuExpandProgress = 0.0f;
    private long topMenuAnimLastNanos = 0L;
    private final boolean initialTopMenuExpanded;
    private boolean topMenuInitialized = false;

    private int scaledTopButtonW() {
        return Math.max(20, Math.round(STYLE_BUTTON_W * WildexUiScale.get()));
    }

    private int scaledMenuButtonW() {
        return Math.max(28, Math.round(MENU_BUTTON_W * WildexUiScale.get()));
    }

    private int scaledTopButtonH() {
        return Math.max(10, Math.round(STYLE_BUTTON_H * WildexUiScale.get()));
    }

    private int scaledMenuButtonH() {
        return Math.max(18, Math.round(MENU_BUTTON_H * WildexUiScale.get()));
    }

    private int scaledTopButtonMargin() {
        return Math.max(2, Math.round(STYLE_BUTTON_MARGIN * WildexUiScale.get()));
    }

    private int scaledGuiScaleButtonYOffset() {
        return Math.round(GUI_SCALE_BUTTON_Y_OFFSET * WildexUiScale.get());
    }

    private int scaledThemeButtonYOffset() {
        return Math.round(THEME_BUTTON_Y_OFFSET * WildexUiScale.get());
    }

    private WildexScreenLayout.Area menuButtonArea() {
        if (this.layout == null) return new WildexScreenLayout.Area(0, 0, scaledMenuButtonW(), scaledMenuButtonH());
        int w = scaledMenuButtonW();
        int h = scaledMenuButtonH();
        int x = Math.max(0, this.layout.screenWidth() - w);
        return new WildexScreenLayout.Area(x, 0, w, h);
    }

    private static WildexScreenLayout.Area previewBabyToggleArea(WildexScreenLayout.Area previewResetArea) {
        if (previewResetArea == null) return null;
        int x = previewResetArea.x() - previewResetArea.w() - PREVIEW_TOGGLE_BUTTON_GAP;
        return new WildexScreenLayout.Area(x, previewResetArea.y(), previewResetArea.w(), previewResetArea.h());
    }

    private WildexScreenLayout.Area previewBabyToggleAreaFromWidget() {
        if (this.previewBabyToggleButton == null) return null;
        return new WildexScreenLayout.Area(
                this.previewBabyToggleButton.getX(),
                this.previewBabyToggleButton.getY(),
                this.previewBabyToggleButton.getWidth(),
                this.previewBabyToggleButton.getHeight()
        );
    }

    public WildexScreen() {
        this(rememberedTopMenuExpanded);
    }

    public WildexScreen(boolean initialTopMenuExpanded) {
        super(TITLE);
        this.initialTopMenuExpanded = initialTopMenuExpanded;
    }

    @Override
    protected void init() {
        if (!this.topMenuInitialized) {
            this.topMenuExpanded = this.initialTopMenuExpanded;
            this.topMenuExpandProgress = this.topMenuExpanded ? 1.0f : 0.0f;
            this.topMenuAnimLastNanos = System.nanoTime();
            rememberedTopMenuExpanded = this.topMenuExpanded;
            this.topMenuInitialized = true;
        }
        this.trophyCollapsed = rememberedTrophyCollapsed;
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

        WildexScreenLayout.Area menuButtonArea = menuButtonArea();
        WildexScreenLayout.Area guiScaleButtonArea = this.layout.styleButtonArea(
                scaledTopButtonW(),
                scaledTopButtonH(),
                scaledTopButtonMargin(),
                scaledGuiScaleButtonYOffset()
        );
        WildexScreenLayout.Area styleButtonArea = this.layout.styleButtonArea(
                scaledTopButtonW(),
                scaledTopButtonH(),
                scaledTopButtonMargin(),
                scaledThemeButtonYOffset()
        );

        this.clearWidgets();

        this.menuButton = this.addRenderableWidget(new WildexStyleButton(
                menuButtonArea.x(),
                menuButtonArea.y(),
                menuButtonArea.w(),
                menuButtonArea.h(),
                Component.literal("Menu"),
                () -> {
                    topMenuExpanded = !topMenuExpanded;
                    rememberedTopMenuExpanded = topMenuExpanded;
                    this.topMenuAnimLastNanos = System.nanoTime();
                },
                null,
                this::topButtonBackgroundTexture,
                () -> this.topMenuExpanded ? "-" : "+",
                1.45f,
                this::menuButtonBackgroundColor,
                true,
                menuButtonOuterFrameColor(),
                menuButtonInnerFrameColor()
        ));
        this.menuButton.setFrameThickness(2, 2);
        this.menuButton.setFillInset(5);

        this.guiScaleToggleButton = this.addRenderableWidget(new WildexStyleButton(
                guiScaleButtonArea.x(),
                guiScaleButtonArea.y(),
                guiScaleButtonArea.w(),
                guiScaleButtonArea.h(),
                Component.literal("GUI Scale"),
                () -> {
                    boolean next = !ClientConfig.INSTANCE.hideGuiScaleSlider.get();
                    ClientConfig.INSTANCE.hideGuiScaleSlider.set(next);
                    ClientConfig.SPEC.save();
                    if (next) {
                        this.draggingUiScaleSlider = false;
                    }
                },
                null,
                this::topButtonBackgroundTexture
        ));
        this.guiScaleToggleButton.setFrameThickness(1, 1);

        this.styleButton = this.addRenderableWidget(new WildexStyleButton(
                styleButtonArea.x(),
                styleButtonArea.y(),
                styleButtonArea.w(),
                styleButtonArea.h(),
                Component.translatable("gui.wildex.theme"),
                () -> {
            var next = WildexThemes.nextStyle(ClientConfig.INSTANCE.designStyle.get());
            ClientConfig.INSTANCE.designStyle.set(next);
            ClientConfig.SPEC.save();
            mobDataResolver.clearCache();
            // Recreate the screen so all layout metrics/widgets are rebuilt from the selected theme.
            Minecraft mcRef = this.minecraft;
            if (mcRef != null) {
                mcRef.setScreen(new WildexScreen(this.topMenuExpanded));
            }
        },
                null,
                this::topButtonBackgroundTexture
        ));
        this.styleButton.setFrameThickness(1, 1);
        updateTopMenuButtonsVisibility();
        WildexNetworkClient.requestDiscoveredMobs();

        WildexScreenLayout.Area search = layout.leftSearchArea();

        this.searchBox = new WildexSearchBox(this.font, search.x(), search.y(), search.w(), search.h(), SEARCH_LABEL);
        this.searchBox.setMaxLength(64);
        this.searchBox.setValue(mobIndex.query());
        this.searchBox.setResponder(this::onSearchChanged);

        this.searchBox.setTextColor(theme.ink());
        this.searchBox.setTextColorUneditable(theme.ink());
        this.searchBox.setBordered(false);
        this.searchBox.setTextShadow(false);
        applySearchBoxTextNudge();

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
                this::onMobEntryClicked,
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
        WildexScreenLayout.Area babyToggleArea = previewBabyToggleArea(resetArea);
        this.previewBabyToggleButton = new MobPreviewResetButton(
                babyToggleArea == null ? 0 : babyToggleArea.x(),
                babyToggleArea == null ? 0 : babyToggleArea.y(),
                babyToggleArea == null ? resetArea.w() : babyToggleArea.w(),
                babyToggleArea == null ? resetArea.h() : babyToggleArea.h(),
                PREVIEW_BABY_LABEL,
                () -> {
                    if (!mobPreviewRenderer.toggleBabyPreview(this.state.selectedMobId())) return;
                    updatePreviewBabyToggleButtonState();
                }
        );
        this.addRenderableWidget(this.previewBabyToggleButton);
        this.shareOverlay = new WildexShareOverlayController(
                this,
                this.font,
                SHARE_BUTTON_LABEL,
                STYLE_BUTTON_W,
                STYLE_BUTTON_H,
                STYLE_BUTTON_MARGIN,
                GUI_SCALE_BUTTON_Y_OFFSET,
                this::isSingleplayerShareBlocked,
                this::isShareEligibleForSelectedMob,
                this.state::selectedMobId
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
        if (WildexClientConfigView.hiddenMode()) {
            WildexNetworkClient.requestViewedMobEntries();
        }
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
        WildexNetworkClient.requestDiscoveredMobs();
        if (WildexClientConfigView.hiddenMode()) {
            WildexNetworkClient.requestViewedMobEntries();
        }
    }

    private void onSearchChanged(String ignored) {
        applyFiltersFromUi();
        refreshMobList();
    }

    private void updateTopMenuButtonsVisibility() {
        if (this.menuButton != null) {
            this.menuButton.visible = true;
            this.menuButton.active = true;
        }
        boolean anyExpanded = this.topMenuExpanded || this.topMenuExpandProgress > 0.0f;
        if (this.guiScaleToggleButton != null) {
            this.guiScaleToggleButton.visible = anyExpanded;
            this.guiScaleToggleButton.active = this.topMenuExpanded && this.topMenuExpandProgress >= 1.0f;
        }
        if (this.styleButton != null) {
            this.styleButton.visible = anyExpanded;
            this.styleButton.active = this.topMenuExpanded && this.topMenuExpandProgress >= 1.0f;
        }
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
        updateTopMenuButtonsVisibility();
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
            this.mobPreviewRenderer.setBabyPreviewEnabled(false);
            rightInfoRenderer.resetSpawnScroll();
            rightInfoRenderer.resetStatsScroll();
            rightInfoRenderer.resetLootScroll();
            rightInfoRenderer.resetMiscScroll();
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
        this.mobPreviewRenderer.setBabyPreviewEnabled(false);
        rightInfoRenderer.resetSpawnScroll();
        rightInfoRenderer.resetStatsScroll();
        rightInfoRenderer.resetMiscScroll();
        saveUiStateToServer();
        requestAllForSelected(next);
        onMobEntryClicked(id);
    }

    private void onMobEntryClicked(ResourceLocation mobId) {
        if (mobId == null) return;
        if (!WildexClientConfigView.hiddenMode()) return;
        if (!WildexDiscoveryCache.isDiscovered(mobId)) return;

        boolean added = WildexViewedMobEntriesCache.add(mobId);
        if (!added) return;

        WildexNetworkClient.markMobEntryViewed(mobId);
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
        if (this.layout != null && this.state.selectedTab() == WildexTab.MISC) {
            if (this.shareOverlay != null && this.shareOverlay.isPanelOpen()) return super.mouseScrolled(mxD, myD, scrollX, scrollY);
            if (rightInfoRenderer.scrollMisc(mx, my, scrollY)) {
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

        if (button == 0 && this.trophyRenderState != null && this.trophyRenderState.toggleHitbox().contains(mx, my)) {
            this.trophyCollapsed = !this.trophyCollapsed;
            rememberedTrophyCollapsed = this.trophyCollapsed;
            return true;
        }

        if (!WildexClientConfigView.hideGuiScaleSlider() && button == 0 && isOnUiScaleSlider(mx, my)) {
            this.draggingUiScaleSlider = true;
            setUiScaleFromSliderMouse(my);
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
        if (this.layout != null && this.state.selectedTab() == WildexTab.MISC) {
            if (this.shareOverlay != null && this.shareOverlay.isPanelOpen()) return false;
            return rightInfoRenderer.handleMiscMouseClicked(mx, my, button);
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
            if (WildexClientConfigView.hideGuiScaleSlider()) {
                this.draggingUiScaleSlider = false;
                return false;
            }
            setUiScaleFromSliderMouse(my);
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
            if (rightInfoRenderer.handleSpawnMouseDragged(my, button)) {
                return true;
            }
        }
        if (this.layout != null && this.state.selectedTab() == WildexTab.STATS) {
            if (this.shareOverlay != null && this.shareOverlay.isPanelOpen()) return false;
            if (rightInfoRenderer.handleStatsMouseDragged(my, button)) {
                return true;
            }
        }
        if (this.layout != null && this.state.selectedTab() == WildexTab.LOOT) {
            if (this.shareOverlay != null && this.shareOverlay.isPanelOpen()) return false;
            if (rightInfoRenderer.handleLootMouseDragged(my, button)) {
                return true;
            }
        }
        if (this.layout != null && this.state.selectedTab() == WildexTab.MISC) {
            if (this.shareOverlay != null && this.shareOverlay.isPanelOpen()) return false;
            return rightInfoRenderer.handleMiscMouseDragged(my, button);
        }

        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        double mxD = toPhysicalMouseXD(mouseX);
        double myD = toPhysicalMouseYD(mouseY);

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
        if (this.layout != null && this.state.selectedTab() == WildexTab.MISC) {
            if (this.shareOverlay != null && this.shareOverlay.isPanelOpen()) return false;
            return rightInfoRenderer.handleMiscMouseReleased(button);
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
        updateTopMenuAnimation();
        syncMobListItemHeightIfNeeded();
        syncWidgetPositions();

        this.screenSpace.pushInverseScale(graphics);

        WildexUiTheme.Palette theme = WildexUiTheme.current();
        applyThemeToInputs(theme);
        syncShareTopButtonsPosition();

        this.trophyRenderState = trophyRenderer.render(
                graphics,
                this.layout,
                WildexClientConfigView.hiddenMode() && WildexCompletionCache.isComplete(),
                this.trophyCollapsed
        );

        renderer.render(graphics, this.layout, state, physicalMouseX, physicalMouseY, partialTick);

        WildexScreenLayout.Area entriesArea = this.layout.leftEntriesCounterArea();
        Component entriesText = Component.translatable("gui.wildex.entries_count", (this.visibleEntries == null ? 0 : this.visibleEntries.size()));
        float entriesScale = resolveEntriesTextScale();
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
            if (WildexThemes.isVintageLayout()) {
                discY += Math.max(1, Math.round(4 * this.layout.scale()));
            }
            WildexUiRenderUtil.drawScaledText(graphics, this.font, discText, discX, discY, discScale, theme.ink());
        }

        updatePreviewBabyToggleButtonState();
        mobPreviewRenderer.render(graphics, this.layout, state, physicalMouseX, physicalMouseY, partialTick);
        WildexScreenLayout.Area previewArea = this.layout.rightPreviewArea();
        WildexScreenLayout.Area previewResetArea = this.layout.previewResetButtonArea();
        WildexScreenLayout.Area previewBabyArea = (this.previewBabyToggleButton != null && this.previewBabyToggleButton.visible)
                ? previewBabyToggleAreaFromWidget()
                : null;
        HintBounds previewHint = drawPreviewControlsHint(graphics, this.layout, previewArea, previewResetArea, previewBabyArea);

        boolean showShareNotice = this.shareOverlay != null && this.shareOverlay.shouldShowNotice();
        boolean showSharePanel = this.shareOverlay != null && this.shareOverlay.isPanelVisible();
        boolean vintageLayout = WildexThemes.isVintageLayout();
        if (showSharePanel) {
            if (vintageLayout) {
                WildexUiRenderUtil.drawPanelFrame(graphics, this.layout.sharePanelArea(), theme, 2, 1);
            } else {
                WildexUiRenderUtil.drawPanelFrame(graphics, this.layout.sharePanelArea(), theme);
            }
            renderSharePanel(graphics);
        } else if (showShareNotice) {
            if (vintageLayout) {
                WildexUiRenderUtil.drawPanelFrame(graphics, this.layout.rightTabsArea(), theme, 3, 1);
            } else {
                WildexUiRenderUtil.drawPanelFrame(graphics, this.layout.rightTabsArea(), theme);
            }
            renderShareSingleplayerNotice(graphics);
        } else {
            if (vintageLayout) {
                WildexUiRenderUtil.drawPanelFrame(graphics, this.layout.rightTabsArea(), theme, 3, 1);
            } else {
                WildexUiRenderUtil.drawPanelFrame(graphics, this.layout.rightTabsArea(), theme);
            }
        }

        WildexMobData data = mobDataResolver.resolve(state.selectedMobId());

        rightHeaderRenderer.render(graphics, this.font, this.layout.rightHeaderArea(), state, data.header(), theme.ink());
        if (vintageLayout) {
            WildexUiRenderUtil.drawRoundedPanelFrame(graphics, this.layout.rightHeaderArea(), theme, 3, 3, 1);
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

        if (!WildexClientConfigView.hideGuiScaleSlider()) {
            drawUiScaleSlider(graphics, physicalMouseX, physicalMouseY);
        }

        super.render(graphics, physicalMouseX, physicalMouseY, partialTick);
        renderMobListTopDivider(graphics);

        if (this.previewResetButton != null && this.previewResetButton.isHovered()) {
            WildexUiRenderUtil.renderTooltip(graphics, this.font, List.of(RESET_PREVIEW_TOOLTIP), physicalMouseX, physicalMouseY, this.physicalScreenW, this.physicalScreenH, theme);
        }
        if (this.previewBabyToggleButton != null && this.previewBabyToggleButton.visible && this.previewBabyToggleButton.isHovered()) {
            Component toggleTip = this.mobPreviewRenderer.isBabyPreviewEnabled() ? PREVIEW_ADULT_TOOLTIP : PREVIEW_BABY_TOOLTIP;
            WildexUiRenderUtil.renderTooltip(graphics, this.font, List.of(toggleTip), physicalMouseX, physicalMouseY, this.physicalScreenW, this.physicalScreenH, theme);
        }

        if (this.discoveredOnlyCheckbox != null && this.discoveredOnlyCheckbox.isHovered()) {
            Component tip = this.discoveredOnlyCheckbox.tooltip();
            if (tip != null) WildexUiRenderUtil.renderTooltip(graphics, this.font, List.of(tip), physicalMouseX, physicalMouseY, this.physicalScreenW, this.physicalScreenH, theme);
        }

        if (this.trophyRenderState != null
                && this.trophyRenderState.expanded()
                && this.trophyRenderState.frameHitbox().contains(physicalMouseX, physicalMouseY)
                && !this.trophyRenderState.toggleHitbox().contains(physicalMouseX, physicalMouseY)) {
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

        renderModernRightEdgeOccluder(graphics);
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
        int x1 = x0 + this.mobList.getRowWidth(); // keep aligned with list content area (excluding scrollbar)
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
            applySearchBoxTextNudge();
        }
        if (this.shareOverlay != null) this.shareOverlay.applyThemeToInputs(theme);
    }

    private void applySearchBoxTextNudge() {
        if (this.searchBox == null) return;
        if (WildexThemes.isVintageLayout()) {
            this.searchBox.setTextNudge(5, 4);
        } else {
            this.searchBox.setTextNudge(2, 2);
        }
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
            WildexScreenLayout.Area previewResetArea,
            WildexScreenLayout.Area previewBabyArea
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
        float inv = 1.0f / PREVIEW_HINT_SCALE;

        int rightBound = anchor.rightBoundX();
        if (previewBabyArea != null) {
            rightBound = Math.min(rightBound, previewBabyArea.x() - 2);
        }
        int availableW = Math.max(0, rightBound - hintBaseX);
        if (availableW <= 2) return null;

        graphics.pose().pushPose();
        WildexScissor.enablePhysical(graphics, hintBaseX, hintBaseY, hintBaseX + availableW, hintBaseY + scaledTextH + 1);
        graphics.pose().scale(PREVIEW_HINT_SCALE, PREVIEW_HINT_SCALE, 1.0f);
        WildexUiText.draw(graphics, 
                this.font,
                PREVIEW_CONTROLS_LABEL,
                Math.round(hintBaseX * inv),
                Math.round(hintBaseY * inv),
                WildexUiTheme.current().inkMuted(),
                false
        );
        graphics.disableScissor();
        graphics.pose().popPose();

        return new HintBounds(hintBaseX, hintBaseY, Math.min(scaledTextW, availableW), scaledTextH);
    }

    <T extends GuiEventListener & Renderable & NarratableEntry> void addShareWidget(T widget) {
        this.addRenderableWidget(widget);
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
                this::onMobEntryClicked,
                this::onDebugDiscoverMob
        );
        this.addRenderableWidget(this.mobList);
        this.mobList.setEntries(this.visibleEntries);
        if (keepSelected != null) this.mobList.setSelectedId(keepSelected);
    }

    private void syncWidgetPositions() {
        if (this.layout == null) return;

        WildexScreenLayout.Area menuButtonArea = menuButtonArea();
        WildexScreenLayout.Area guiScaleButtonArea = this.layout.styleButtonArea(
                scaledTopButtonW(),
                scaledTopButtonH(),
                scaledTopButtonMargin(),
                scaledGuiScaleButtonYOffset()
        );
        WildexScreenLayout.Area styleButtonArea = this.layout.styleButtonArea(
                scaledTopButtonW(),
                scaledTopButtonH(),
                scaledTopButtonMargin(),
                scaledThemeButtonYOffset()
        );
        if (this.menuButton != null) {
            this.menuButton.setX(menuButtonArea.x());
            this.menuButton.setY(menuButtonArea.y());
            this.menuButton.setWidth(menuButtonArea.w());
            this.menuButton.setHeight(menuButtonArea.h());
        }

        int menuBottom = menuButtonArea.y() + menuButtonArea.h() - 1;
        float pStyle = this.topMenuExpandProgress;
        float pScale = Math.max(0.0f, Math.min(1.0f, (this.topMenuExpandProgress - 0.10f) / 0.90f));
        int styleYAnimated = menuBottom + Math.round((styleButtonArea.y() - menuBottom) * pStyle);
        int guiYAnimated = menuBottom + Math.round((guiScaleButtonArea.y() - menuBottom) * pScale);

        if (this.guiScaleToggleButton != null) {
            this.guiScaleToggleButton.setX(guiScaleButtonArea.x());
            this.guiScaleToggleButton.setY(guiYAnimated);
            this.guiScaleToggleButton.setWidth(guiScaleButtonArea.w());
            this.guiScaleToggleButton.setHeight(guiScaleButtonArea.h());
        }
        if (this.styleButton != null) {
            this.styleButton.setX(styleButtonArea.x());
            this.styleButton.setY(styleYAnimated);
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
        if (this.previewBabyToggleButton != null) {
            WildexScreenLayout.Area reset = this.layout.previewResetButtonArea();
            WildexScreenLayout.Area a = previewBabyToggleArea(reset);
            if (a != null) {
                this.previewBabyToggleButton.setX(a.x());
                this.previewBabyToggleButton.setY(a.y());
                this.previewBabyToggleButton.setWidth(a.w());
                this.previewBabyToggleButton.setHeight(a.h());
            }
        }

        if (this.shareOverlay != null) {
            this.shareOverlay.relayout(this.layout);
        }

        updateTopMenuButtonsVisibility();
    }

    private void drawUiScaleSlider(GuiGraphics graphics, int mouseX, int mouseY) {
        float s = UI_SLIDER_RENDER_SCALE;
        this.uiSliderW = Math.max(12, Math.round(UI_SLIDER_H * s));
        this.uiSliderH = Math.max(84, Math.round(UI_SLIDER_W * s));
        this.uiSliderKnobW = Math.max(10, Math.round(UI_SLIDER_KNOB_W * s));
        this.uiSliderX = this.physicalScreenW - this.uiSliderW - UI_SLIDER_RIGHT_MARGIN;
        this.uiSliderY = Math.max(2, (this.physicalScreenH - this.uiSliderH) / 2);
        if (this.uiSliderX < 2) this.uiSliderX = 2;
        if (this.uiSliderY + this.uiSliderH > this.physicalScreenH - 2) {
            this.uiSliderY = Math.max(2, this.physicalScreenH - 2 - this.uiSliderH);
        }

        WildexUiTheme.Palette theme = WildexUiTheme.current();
        ResourceLocation bgTex = topButtonBackgroundTexture();

        graphics.blit(
                bgTex,
                this.uiSliderX,
                this.uiSliderY,
                this.uiSliderW,
                this.uiSliderH,
                0,
                0,
                UI_SLIDER_TEX_SIZE,
                UI_SLIDER_TEX_SIZE,
                UI_SLIDER_TEX_SIZE,
                UI_SLIDER_TEX_SIZE
        );
        if (WildexThemes.isModernLayout()) {
            graphics.fill(this.uiSliderX, this.uiSliderY, this.uiSliderX + this.uiSliderW, this.uiSliderY + this.uiSliderH, MODERN_MENU_SLIDER_BG);
            graphics.fill(this.uiSliderX, this.uiSliderY, this.uiSliderX + this.uiSliderW, this.uiSliderY + this.uiSliderH, 0x22000000);
        } else {
            graphics.fill(this.uiSliderX, this.uiSliderY, this.uiSliderX + this.uiSliderW, this.uiSliderY + this.uiSliderH, 0x66140E0A);
        }
        drawThickFrame(graphics, this.uiSliderX, this.uiSliderY, this.uiSliderX + this.uiSliderW, this.uiSliderY + this.uiSliderH);

        float t = WildexUiScale.toNormalized(WildexUiScale.get());
        int travel = Math.max(1, this.uiSliderH - this.uiSliderKnobW);
        int knobY = this.uiSliderY + Math.round((1.0f - t) * travel);
        int knobColor = isOnUiScaleSlider(mouseX, mouseY) || this.draggingUiScaleSlider ? 0xFFECECEC : 0xFFD8D8D8;
        graphics.blit(
                bgTex,
                this.uiSliderX,
                knobY,
                this.uiSliderW,
                this.uiSliderKnobW,
                0,
                0,
                UI_SLIDER_TEX_SIZE,
                UI_SLIDER_TEX_SIZE,
                UI_SLIDER_TEX_SIZE,
                UI_SLIDER_TEX_SIZE
        );
        if (WildexThemes.isModernLayout()) {
            graphics.fill(this.uiSliderX, knobY, this.uiSliderX + this.uiSliderW, knobY + this.uiSliderKnobW, MODERN_MENU_SLIDER_BG);
        }
        graphics.fill(this.uiSliderX, knobY, this.uiSliderX + this.uiSliderW, knobY + this.uiSliderKnobW, knobColor);
        drawThickFrame(graphics, this.uiSliderX, knobY, this.uiSliderX + this.uiSliderW, knobY + this.uiSliderKnobW);

        float ls = UI_SLIDER_LABEL_SCALE;
        int lineScaledH = Math.round(this.font.lineHeight * ls);
        int labelGap = Math.round(3 * s);

        String topText = "200%";
        int topScaledW = Math.round(this.font.width(topText) * ls);
        int topX = this.uiSliderX + ((this.uiSliderW - topScaledW) / 2);
        int topY = this.uiSliderY - lineScaledH - labelGap;

        String bottomText = "50%";
        int bottomScaledW = Math.round(this.font.width(bottomText) * ls);
        int bottomX = this.uiSliderX + ((this.uiSliderW - bottomScaledW) / 2);
        int bottomY = this.uiSliderY + this.uiSliderH + labelGap;

        String labelText = "UI";
        int labelScaledW = Math.round(this.font.width(labelText) * ls);
        int labelX = this.uiSliderX + ((this.uiSliderW - labelScaledW) / 2);
        int labelY = bottomY + lineScaledH + Math.round(2 * s);

        topX = Math.max(2, Math.min(topX, this.physicalScreenW - topScaledW - 2));
        bottomX = Math.max(2, Math.min(bottomX, this.physicalScreenW - bottomScaledW - 2));
        labelX = Math.max(2, Math.min(labelX, this.physicalScreenW - labelScaledW - 2));
        graphics.pose().pushPose();
        graphics.pose().scale(ls, ls, 1.0f);
        graphics.drawString(this.font, topText, Math.round(topX / ls), Math.round(topY / ls), theme.inkOnDark(), false);
        graphics.drawString(this.font, bottomText, Math.round(bottomX / ls), Math.round(bottomY / ls), theme.inkOnDark(), false);
        int lx = Math.round(labelX / ls);
        int ly = Math.round(labelY / ls);
        graphics.drawString(this.font, labelText, lx, ly, theme.inkOnDark(), false);
        graphics.pose().popPose();

        if (this.draggingUiScaleSlider) {
            int currentPercent = WildexUiScale.toDisplayPercent(WildexUiScale.get());
            String live = currentPercent + "%";
            int liveScaledW = Math.round(this.font.width(live) * ls);
            int liveScaledH = Math.round(this.font.lineHeight * ls);
            int liveX = this.uiSliderX - liveScaledW - 10;
            int liveY = knobY + (this.uiSliderKnobW / 2) - (liveScaledH / 2);
            liveX = Math.max(2, liveX);
            liveY = Math.max(2, Math.min(liveY, this.physicalScreenH - liveScaledH - 2));
            graphics.pose().pushPose();
            graphics.pose().scale(ls, ls, 1.0f);
            graphics.drawString(this.font, live, Math.round(liveX / ls), Math.round(liveY / ls), theme.inkOnDark(), false);
            graphics.pose().popPose();
        }
    }

    private boolean isOnUiScaleSlider(int mouseX, int mouseY) {
        return mouseX >= this.uiSliderX
                && mouseX < this.uiSliderX + this.uiSliderW
                && mouseY >= this.uiSliderY
                && mouseY < this.uiSliderY + this.uiSliderH;
    }

    private void setUiScaleFromSliderMouse(int mouseY) {
        int travel = Math.max(1, this.uiSliderH - this.uiSliderKnobW);
        int rel = Math.max(0, Math.min(travel, (this.uiSliderY + travel) - mouseY));
        float t = rel / (float) travel;
        float next = WildexUiScale.MIN + ((WildexUiScale.MAX - WildexUiScale.MIN) * t);
        WildexUiScale.set(next);
    }

    private ResourceLocation topButtonBackgroundTexture() {
        var style = WildexThemes.current().layoutProfile();
        if (style == null) {
            return ResourceLocation.fromNamespaceAndPath("wildex", "textures/gui/trophy_bg_red.png");
        }
        return switch (style) {
            case MODERN -> ResourceLocation.fromNamespaceAndPath("wildex", "textures/gui/trophy_bg_modern.png");
            case JUNGLE -> ResourceLocation.fromNamespaceAndPath("wildex", "textures/gui/trophy_bg_jungle.png");
            case RUNES -> ResourceLocation.fromNamespaceAndPath("wildex", "textures/gui/trophy_bg_runes.png");
            case STEAMPUNK -> ResourceLocation.fromNamespaceAndPath("wildex", "textures/gui/trophy_bg_steampunk.png");
            case VINTAGE -> ResourceLocation.fromNamespaceAndPath("wildex", "textures/gui/trophy_bg_red.png");
        };
    }

    private Integer menuButtonBackgroundColor() {
        return WildexThemes.isModernLayout() ? MODERN_MENU_SLIDER_BG : null;
    }

    private Integer menuButtonOuterFrameColor() {
        return WildexThemes.isModernLayout() ? 0xFF1F9AA1 : 0xFF6F4E31;
    }

    private Integer menuButtonInnerFrameColor() {
        return WildexThemes.isModernLayout() ? 0xFF93E7EC : 0xFFC9A47A;
    }

    private static void drawThickFrame(GuiGraphics g, int x0, int y0, int x1, int y1) {
        int outerThickness = 3;
        int innerThickness = 3;
        for (int i = 0; i < outerThickness; i++) {
            g.fill(x0 + i, y0 + i, x1 - i, y0 + i + 1, THICK_FRAME_OUTER);
            g.fill(x0 + i, y1 - i - 1, x1 - i, y1 - i, THICK_FRAME_OUTER);
            g.fill(x0 + i, y0 + i, x0 + i + 1, y1 - i, THICK_FRAME_OUTER);
            g.fill(x1 - i - 1, y0 + i, x1 - i, y1 - i, THICK_FRAME_OUTER);
        }
        for (int i = 0; i < innerThickness; i++) {
            int off = outerThickness + i;
            g.fill(x0 + off, y0 + off, x1 - off, y0 + off + 1, THICK_FRAME_INNER);
            g.fill(x0 + off, y1 - off - 1, x1 - off, y1 - off, THICK_FRAME_INNER);
            g.fill(x0 + off, y0 + off, x0 + off + 1, y1 - off, THICK_FRAME_INNER);
            g.fill(x1 - off - 1, y0 + off, x1 - off, y1 - off, THICK_FRAME_INNER);
        }
    }

    private void renderModernRightEdgeOccluder(GuiGraphics graphics) {
        if (this.layout == null) return;
        if (!WildexThemes.isModernLayout()) return;
        if (!WildexClientConfigView.hiddenMode() || !WildexClientConfigView.shareOffersEnabled()) return;

        int screenX = Math.round(this.layout.x());
        int screenY = Math.round(this.layout.y());
        int screenW = Math.max(1, Math.round(WildexScreenLayout.TEX_W * this.layout.scale()));
        int screenH = Math.max(1, Math.round(WildexScreenLayout.TEX_H * this.layout.scale()));
        int occluderScreenW = Math.max(1, Math.round(MODERN_RIGHT_EDGE_OCCLUDER_TEX_W * this.layout.scale()));
        int occluderX = screenX + screenW - occluderScreenW;

        graphics.blit(
                this.layout.theme().backgroundTexture(),
                occluderX,
                screenY,
                occluderScreenW,
                screenH,
                WildexScreenLayout.TEX_W - MODERN_RIGHT_EDGE_OCCLUDER_TEX_W,
                0,
                MODERN_RIGHT_EDGE_OCCLUDER_TEX_W,
                WildexScreenLayout.TEX_H,
                WildexScreenLayout.TEX_W,
                WildexScreenLayout.TEX_H
        );
    }

    private void updateTopMenuAnimation() {
        long now = System.nanoTime();
        if (this.topMenuAnimLastNanos == 0L) {
            this.topMenuAnimLastNanos = now;
            return;
        }
        float dt = (now - this.topMenuAnimLastNanos) / 1_000_000_000.0f;
        this.topMenuAnimLastNanos = now;
        if (dt <= 0.0f) return;
        if (dt > 0.05f) dt = 0.05f;

        float speed = this.topMenuExpanded ? TOP_MENU_OPEN_SPEED : TOP_MENU_CLOSE_SPEED;
        float delta = speed * dt;
        if (this.topMenuExpanded) {
            this.topMenuExpandProgress = Math.min(1.0f, this.topMenuExpandProgress + delta);
        } else {
            this.topMenuExpandProgress = Math.max(0.0f, this.topMenuExpandProgress - delta);
        }
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
            } else if (!useDefaultSelection) {
                this.state.setSelectedMobId(targetMob.toString());
            } else {
                this.state.setSelectedMobId("");
            }
        } finally {
            suppressUiStateSync = false;
            suppressMobSelectionCallback = false;
        }

        this.lastTab = this.state.selectedTab();
        this.mobPreviewRenderer.setBabyPreviewEnabled(false);
        this.rightInfoRenderer.resetSpawnScroll();
        this.rightInfoRenderer.resetStatsScroll();
        requestAllForSelected(this.state.selectedMobId());
    }

    private void updatePreviewBabyToggleButtonState() {
        if (this.previewBabyToggleButton == null) return;

        ResourceLocation selectedMob = ResourceLocation.tryParse(this.state.selectedMobId());
        boolean discovered = selectedMob != null && WildexDiscoveryCache.isDiscovered(selectedMob);
        boolean canToggle = discovered && this.mobPreviewRenderer.canToggleBabyPreview(this.state.selectedMobId());

        this.previewBabyToggleButton.visible = canToggle;
        this.previewBabyToggleButton.active = canToggle;
        if (!canToggle) {
            this.mobPreviewRenderer.setBabyPreviewEnabled(false);
            this.previewBabyToggleButton.setLabel(PREVIEW_BABY_LABEL);
            return;
        }

        this.previewBabyToggleButton.setLabel(this.mobPreviewRenderer.isBabyPreviewEnabled() ? PREVIEW_ADULT_LABEL : PREVIEW_BABY_LABEL);
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





