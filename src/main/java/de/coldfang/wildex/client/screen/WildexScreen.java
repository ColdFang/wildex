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
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.Button;
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

    private static final ResourceLocation TROPHY_ICON =
            ResourceLocation.fromNamespaceAndPath("wildex", "textures/gui/trophy.png");
    private static final int TROPHY_TEX_SIZE = 128;
    private static final int TROPHY_DRAW_SIZE = 32;
    private static final int TROPHY_FRAME_PAD_X = 2;
    private static final int TROPHY_FRAME_PAD_Y = 4;
    private static final int TROPHY_BG_EXTEND_RIGHT = 8;
    private static final int TROPHY_BG_EXTEND_LEFT = 4;
    private static final int TROPHY_BG_VINTAGE = 0xE6EFE3D2;
    private static final int TROPHY_BG_MODERN = 0xCC171310;

    private static final Component TROPHY_TIP_TITLE = Component.translatable("tooltip.wildex.spyglass_pulse.title");
    private static final List<Component> TROPHY_TOOLTIP = List.of(
            TROPHY_TIP_TITLE,
            Component.translatable("tooltip.wildex.spyglass_pulse.line1"),
            Component.translatable("tooltip.wildex.spyglass_pulse.line2"),
            Component.translatable("tooltip.wildex.spyglass_pulse.line3"),
            Component.empty(),
            Component.translatable("tooltip.wildex.spyglass_pulse.line4")
    );

    private static final int TROPHY_ATTACH_GAP_X = 0;
    private static final int TROPHY_ATTACH_GAP_Y = 15;

    private static final int STYLE_BUTTON_W = 56;
    private static final int STYLE_BUTTON_H = 14;
    private static final int STYLE_BUTTON_MARGIN = 6;
    private static final int STYLE_BUTTON_Y_OFFSET = 2;
    // TEMP: GUI scale test button for fast layout iteration. Remove before release.
    private static final int TEST_GUI_SCALE_BUTTON_X = 6;
    private static final int TEST_GUI_SCALE_BUTTON_Y = 6;
    private static final int TEST_GUI_SCALE_BUTTON_W = 62;
    private static final int TEST_GUI_SCALE_BUTTON_H = 14;
    private static final Component TEST_GUI_SCALE_TOOLTIP = Component.literal("Cycle Minecraft GUI Scale (TEMP)");

    private static final float VERSION_SCALE = 0.55f;
    private static final float PREVIEW_HINT_SCALE = 0.62f;

    private final WildexScreenState state = new WildexScreenState();
    private final WildexBookRenderer renderer = new WildexBookRenderer();
    private final WildexMobPreviewRenderer mobPreviewRenderer = new WildexMobPreviewRenderer();
    private final WildexMobIndexModel mobIndex = new WildexMobIndexModel();

    private final WildexMobDataResolver mobDataResolver = new WildexMobDataResolver();
    private final WildexRightInfoRenderer rightInfoRenderer = new WildexRightInfoRenderer();
    private final WildexRightHeaderRenderer rightHeaderRenderer = new WildexRightHeaderRenderer();

    private WildexScreenLayout layout;

    private WildexSearchBox searchBox;
    private MobListWidget mobList;
    private MobPreviewResetButton previewResetButton;
    private WildexDiscoveredOnlyCheckbox discoveredOnlyCheckbox;
    private Button testGuiScaleButton;
    private RightTabsWidget rightTabsWidget;
    private WildexShareOverlayController shareOverlay;

    private List<EntityType<?>> visibleEntries = List.of();
    private boolean suppressMobSelectionCallback = false;
    private boolean suppressUiStateSync = false;
    private boolean localUiStateDirtySinceOpen = false;

    private WildexTab lastTab = WildexTab.STATS;
    private int lastDiscoveryCount = -1;
    private final String versionLabel = resolveVersionLabel();

    public WildexScreen() {
        super(TITLE);
    }

    @Override
    protected void init() {
        this.layout = WildexScreenLayout.compute(this.width, this.height);
        WildexUiTheme.Palette theme = WildexUiTheme.current();
        this.state.setSelectedTab(WildexTab.STATS);
        this.state.setSelectedMobId("");
        this.rightInfoRenderer.resetSpawnScroll();
        this.rightInfoRenderer.resetStatsScroll();
        this.rightInfoRenderer.resetLootScroll();

        WildexScreenLayout.Area styleButtonArea = this.layout.styleButtonArea(
                STYLE_BUTTON_W,
                STYLE_BUTTON_H,
                STYLE_BUTTON_MARGIN,
                STYLE_BUTTON_Y_OFFSET
        );

        this.clearWidgets();

        this.addRenderableWidget(new WildexStyleButton(
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
        if (WildexClientConfigView.debugMode()) {
            this.testGuiScaleButton = this.addRenderableWidget(Button.builder(testGuiScaleLabel(), b -> cycleGuiScaleForTesting())
                    .bounds(TEST_GUI_SCALE_BUTTON_X, TEST_GUI_SCALE_BUTTON_Y, TEST_GUI_SCALE_BUTTON_W, TEST_GUI_SCALE_BUTTON_H)
                    .build());
        } else {
            this.testGuiScaleButton = null;
        }

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
        int mx = (int) Math.floor(mouseX);
        int my = (int) Math.floor(mouseY);

        if (this.shareOverlay != null && this.layout != null && this.shareOverlay.handleDropdownMouseScrolled(mx, my, scrollY, this.layout)) {
            return true;
        }

        if (mobPreviewRenderer.isMouseOverPreview(this.layout, mx, my)) {
            mobPreviewRenderer.adjustZoom(scrollY);
            return true;
        }

        if (this.layout != null && this.state.selectedTab() == WildexTab.STATS) {
            if (this.shareOverlay != null && this.shareOverlay.isPanelOpen()) return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
            if (rightInfoRenderer.scrollStats(mx, my, scrollY)) {
                return true;
            }
        }

        if (this.layout != null && this.state.selectedTab() == WildexTab.SPAWNS) {
            if (this.shareOverlay != null && this.shareOverlay.isPanelOpen()) return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
            WildexScreenLayout.Area a = this.layout.rightInfoArea();
            if (a != null && mouseX >= a.x() && mouseX < a.x() + a.w() && mouseY >= a.y() && mouseY < a.y() + a.h()) {
                rightInfoRenderer.scrollSpawn(scrollY);
                return true;
            }
        }
        if (this.layout != null && this.state.selectedTab() == WildexTab.LOOT) {
            if (this.shareOverlay != null && this.shareOverlay.isPanelOpen()) return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
            if (rightInfoRenderer.scrollLoot(mx, my, scrollY)) {
                return true;
            }
        }

        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.shareOverlay != null && this.layout != null && this.shareOverlay.handleDropdownMouseClicked(mouseX, mouseY, button, this.layout)) {
            return true;
        }

        if (super.mouseClicked(mouseX, mouseY, button)) return true;

        if (this.layout != null) {
            int mx = (int) Math.floor(mouseX);
            int my = (int) Math.floor(mouseY);
            if (mobPreviewRenderer.isMouseOverPreview(this.layout, mx, my) && mobPreviewRenderer.beginRotationDrag(mx, my, button)) {
                return true;
            }
        }

        if (this.layout != null && this.state.selectedTab() == WildexTab.SPAWNS) {
            if (this.shareOverlay != null && this.shareOverlay.isPanelOpen()) return false;
            int mx = (int) Math.floor(mouseX);
            int my = (int) Math.floor(mouseY);
            if (rightInfoRenderer.handleSpawnMouseClicked(mx, my, button, this.state)) {
                return true;
            }
        }
        if (this.layout != null && this.state.selectedTab() == WildexTab.STATS) {
            if (this.shareOverlay != null && this.shareOverlay.isPanelOpen()) return false;
            int mx = (int) Math.floor(mouseX);
            int my = (int) Math.floor(mouseY);
            if (rightInfoRenderer.handleStatsMouseClicked(mx, my, button)) {
                return true;
            }
        }
        if (this.layout != null && this.state.selectedTab() == WildexTab.LOOT) {
            if (this.shareOverlay != null && this.shareOverlay.isPanelOpen()) return false;
            int mx = (int) Math.floor(mouseX);
            int my = (int) Math.floor(mouseY);
            if (rightInfoRenderer.handleLootMouseClicked(mx, my, button)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (super.mouseDragged(mouseX, mouseY, button, dragX, dragY)) return true;

        if (this.layout != null) {
            int mx = (int) Math.floor(mouseX);
            int my = (int) Math.floor(mouseY);
            if (mobPreviewRenderer.updateRotationDrag(mx, my, button)) {
                return true;
            }
        }

        if (this.layout != null && this.state.selectedTab() == WildexTab.SPAWNS) {
            if (this.shareOverlay != null && this.shareOverlay.isPanelOpen()) return false;
            int mx = (int) Math.floor(mouseX);
            int my = (int) Math.floor(mouseY);
            if (rightInfoRenderer.handleSpawnMouseDragged(mx, my, button)) {
                return true;
            }
        }
        if (this.layout != null && this.state.selectedTab() == WildexTab.STATS) {
            if (this.shareOverlay != null && this.shareOverlay.isPanelOpen()) return false;
            int mx = (int) Math.floor(mouseX);
            int my = (int) Math.floor(mouseY);
            if (rightInfoRenderer.handleStatsMouseDragged(mx, my, button)) {
                return true;
            }
        }
        if (this.layout != null && this.state.selectedTab() == WildexTab.LOOT) {
            if (this.shareOverlay != null && this.shareOverlay.isPanelOpen()) return false;
            int mx = (int) Math.floor(mouseX);
            int my = (int) Math.floor(mouseY);
            if (rightInfoRenderer.handleLootMouseDragged(mx, my, button)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (super.mouseReleased(mouseX, mouseY, button)) return true;

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
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        WildexUiTheme.Palette theme = WildexUiTheme.current();
        applyThemeToInputs(theme);
        syncShareTopButtonsPosition();

        boolean trophyDrawn = false;
        int trophyX = 0;
        int trophyY = 0;
        int trophyHitW = TROPHY_DRAW_SIZE;
        int trophyHitH = TROPHY_DRAW_SIZE;
        if (WildexClientConfigView.hiddenMode() && WildexCompletionCache.isComplete()) {
            int frameW = TROPHY_DRAW_SIZE + (TROPHY_FRAME_PAD_X * 2);
            int frameH = TROPHY_DRAW_SIZE + (TROPHY_FRAME_PAD_Y * 2);
            int texLeft = Math.round(layout.x());
            int texBottom = Math.round(layout.y() + (WildexScreenLayout.TEX_H * layout.scale()));
            int anchorX = texLeft - frameW + TROPHY_ATTACH_GAP_X;
            int anchorY = texBottom - frameH - TROPHY_ATTACH_GAP_Y;
            WildexScreenLayout.Area trophyArea = new WildexScreenLayout.Area(anchorX, anchorY, frameW, frameH);
            int trophyBg = WildexThemes.isVintageLayout() ? TROPHY_BG_VINTAGE : TROPHY_BG_MODERN;
            WildexUiRenderUtil.drawDockedTrophyFrame(graphics, trophyArea, theme, 3, trophyBg, TROPHY_BG_EXTEND_LEFT, TROPHY_BG_EXTEND_RIGHT);

            trophyX = anchorX + TROPHY_FRAME_PAD_X;
            trophyY = anchorY + TROPHY_FRAME_PAD_Y;
            trophyHitW = frameW;
            trophyHitH = frameH;

            graphics.blit(
                    TROPHY_ICON,
                    trophyX,
                    trophyY,
                    TROPHY_DRAW_SIZE,
                    TROPHY_DRAW_SIZE,
                    0,
                    0,
                    TROPHY_TEX_SIZE,
                    TROPHY_TEX_SIZE,
                    TROPHY_TEX_SIZE,
                    TROPHY_TEX_SIZE
            );

            trophyDrawn = true;
        }

        renderer.render(graphics, layout, state, mouseX, mouseY, partialTick);
        renderVersionLabel(graphics);

        WildexScreenLayout.Area entriesArea = layout.leftEntriesCounterArea();
        Component entriesText = Component.translatable("gui.wildex.entries_count", (this.visibleEntries == null ? 0 : this.visibleEntries.size()));
        float entriesScale = resolveEntriesTextScale();
        int textW = Math.max(1, Math.round(this.font.width(entriesText) * entriesScale));
        int textH = Math.max(1, Math.round(this.font.lineHeight * entriesScale));
        int entriesX = layout.leftSearchArea().x();
        int entriesY = entriesArea.y() + ((entriesArea.h() - textH) / 2) + 1;
        int maxEntriesY = layout.leftSearchArea().y() - textH - 1;
        entriesY = Math.min(entriesY, maxEntriesY);
        WildexUiRenderUtil.drawScaledText(graphics, this.font, entriesText, entriesX, entriesY, entriesScale, theme.ink());

        if (WildexClientConfigView.hiddenMode()) {
            WildexScreenLayout.Area discArea = layout.leftDiscoveryCounterArea();

            int discovered = WildexDiscoveryCache.count();
            int total = mobIndex.totalCount();

            Component discText = Component.translatable("gui.wildex.discovered_count", discovered, total);
            float discScale = resolveEntriesTextScale();
            int discTextH = Math.max(1, Math.round(this.font.lineHeight * discScale));

            int discX = discArea.x();
            int discY = discArea.y() + ((discArea.h() - discTextH) / 2);
            WildexUiRenderUtil.drawScaledText(graphics, this.font, discText, discX, discY, discScale, theme.ink());
        }

        mobPreviewRenderer.render(graphics, layout, state, mouseX, mouseY, partialTick);
        WildexScreenLayout.Area previewArea = layout.rightPreviewArea();
        WildexScreenLayout.Area previewResetArea = layout.previewResetButtonArea();
        HintBounds previewHint = drawPreviewControlsHint(graphics, layout, previewArea, previewResetArea);

        boolean showShareNotice = this.shareOverlay != null && this.shareOverlay.shouldShowNotice();
        boolean showSharePanel = this.shareOverlay != null && this.shareOverlay.isPanelVisible();
        if (showSharePanel) {
            WildexUiRenderUtil.drawPanelFrame(graphics, layout.sharePanelArea(), theme);
            renderSharePanel(graphics);
        } else if (showShareNotice) {
            WildexUiRenderUtil.drawPanelFrame(graphics, layout.rightTabsArea(), theme);
            renderShareSingleplayerNotice(graphics);
        } else {
            WildexUiRenderUtil.drawPanelFrame(graphics, layout.rightTabsArea(), theme);
        }

        WildexMobData data = mobDataResolver.resolve(state.selectedMobId());

        rightHeaderRenderer.render(graphics, this.font, layout.rightHeaderArea(), state, data.header(), theme.ink());
        if (WildexThemes.isVintageLayout()) {
            WildexUiRenderUtil.drawRoundedPanelFrame(graphics, layout.rightHeaderArea(), theme, 3);
        }

        if (!showSharePanel && !showShareNotice) {
            rightInfoRenderer.render(
                    graphics,
                    this.font,
                    layout.rightInfoArea(),
                    state,
                    data,
                    theme.ink(),
                    mouseX,
                    mouseY
            );
        }

        super.render(graphics, mouseX, mouseY, partialTick);
        renderMobListTopDivider(graphics);

        if (this.previewResetButton != null && this.previewResetButton.isHovered()) {
            WildexUiRenderUtil.renderTooltip(graphics, this.font, List.of(RESET_PREVIEW_TOOLTIP), mouseX, mouseY, this.width, this.height, theme);
        }

        if (this.discoveredOnlyCheckbox != null && this.discoveredOnlyCheckbox.isHovered()) {
            Component tip = this.discoveredOnlyCheckbox.tooltip();
            if (tip != null) graphics.renderTooltip(this.font, tip, mouseX, mouseY);
        }

        if (trophyDrawn && isMouseOverRect(mouseX, mouseY, trophyX - TROPHY_FRAME_PAD_X, trophyY - TROPHY_FRAME_PAD_Y, trophyHitW, trophyHitH)) {
            WildexUiRenderUtil.renderTooltip(graphics, this.font, TROPHY_TOOLTIP, mouseX, mouseY, this.width, this.height, theme);
        }

        if (!mobPreviewRenderer.isDraggingPreview() && isMouseOverPreviewControlsHint(mouseX, mouseY, previewHint)) {
            WildexUiRenderUtil.renderTooltip(graphics, this.font, PREVIEW_CONTROLS_TOOLTIP, mouseX, mouseY, this.width, this.height, theme);
        }
        if (this.shareOverlay != null && this.shareOverlay.isShareEntryButtonHovered()) {
            WildexUiRenderUtil.renderTooltip(graphics, this.font, List.of(this.shareOverlay.shareTooltip()), mouseX, mouseY, this.width, this.height, theme);
        }
        if (this.shareOverlay != null && this.shareOverlay.isShareClaimButtonHovered()) {
            WildexUiRenderUtil.renderTooltip(graphics, this.font, List.of(this.shareOverlay.shareClaimTooltip()), mouseX, mouseY, this.width, this.height, theme);
        }
        if (this.shareOverlay != null && this.shareOverlay.isShareOpenOffersHovered()) {
            WildexUiRenderUtil.renderTooltip(graphics, this.font, List.of(this.shareOverlay.shareOpenOffersTooltip()), mouseX, mouseY, this.width, this.height, theme);
        }
        if (this.testGuiScaleButton != null && this.testGuiScaleButton.isHovered()) {
            WildexUiRenderUtil.renderTooltip(graphics, this.font, List.of(TEST_GUI_SCALE_TOOLTIP), mouseX, mouseY, this.width, this.height, theme);
        }
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
        int textW = this.font.width(PREVIEW_CONTROLS_LABEL);
        int scaledTextW = Math.max(1, Math.round(textW * PREVIEW_HINT_SCALE));
        int scaledTextH = Math.max(1, Math.round(this.font.lineHeight * PREVIEW_HINT_SCALE));
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
        graphics.enableScissor(hintBaseX, hintBaseY, hintBaseX + availableW, hintBaseY + scaledTextH + 1);
        graphics.pose().scale(PREVIEW_HINT_SCALE, PREVIEW_HINT_SCALE, 1.0f);
        graphics.drawString(
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

        int scaledTextW = Math.max(1, Math.round(this.font.width(this.versionLabel) * VERSION_SCALE));
        int scaledTextH = Math.max(1, Math.round(this.font.lineHeight * VERSION_SCALE));
        WildexScreenLayout.Area versionArea = this.layout.versionLabelArea(
                STYLE_BUTTON_W,
                STYLE_BUTTON_H,
                STYLE_BUTTON_MARGIN,
                STYLE_BUTTON_Y_OFFSET,
                scaledTextW,
                scaledTextH
        );

        float inv = 1.0f / VERSION_SCALE;

        graphics.pose().pushPose();
        graphics.pose().scale(VERSION_SCALE, VERSION_SCALE, 1.0f);
        graphics.drawString(this.font, this.versionLabel, Math.round(versionArea.x() * inv), Math.round(versionArea.y() * inv), WildexUiTheme.current().inkMuted(), false);
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

    // TEMP helper: cycles Minecraft's native GUI scale for quick layout testing.
    private void cycleGuiScaleForTesting() {
        Minecraft mc = Minecraft.getInstance();

        OptionInstance<Integer> option = mc.options.guiScale();
        int current = option.get();
        int max = Math.max(1, mc.getWindow().calculateScale(0, mc.isEnforceUnicode()));
        int next;
        if (current == 0) next = 1;
        else if (current >= max) next = 0;
        else next = current + 1;

        option.set(next);
        mc.options.save();
        mc.resizeDisplay();
        this.init();
    }

    private Component testGuiScaleLabel() {
        Minecraft mc = Minecraft.getInstance();
        int scale = mc.options.guiScale().get();
        return Component.literal(scale == 0 ? "GUI Auto" : "GUI " + scale);
    }

    private float resolveEntriesTextScale() {
        Minecraft mc = Minecraft.getInstance();
        int opt = mc.options.guiScale().get();
        int gui = opt == 0 ? Math.max(1, mc.getWindow().calculateScale(0, mc.isEnforceUnicode())) : opt;
        if (gui >= 6) return 0.72f;
        if (gui == 5) return 0.80f;
        if (gui == 4) return 0.90f;
        return 1.0f;
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

