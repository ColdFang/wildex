package de.coldfang.wildex.client.screen;

import de.coldfang.wildex.Wildex;
import de.coldfang.wildex.client.data.WildexCompletionCache;
import de.coldfang.wildex.client.data.WildexDiscoveryCache;
import de.coldfang.wildex.client.data.WildexMobDataResolver;
import de.coldfang.wildex.client.data.WildexMobIndexModel;
import de.coldfang.wildex.client.data.WildexPlayerUiStateCache;
import de.coldfang.wildex.client.data.model.WildexMobData;
import de.coldfang.wildex.client.WildexNetworkClient;
import de.coldfang.wildex.config.ClientConfig;
import de.coldfang.wildex.config.ClientConfig.DesignStyle;
import de.coldfang.wildex.config.CommonConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.EntityType;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class WildexScreen extends Screen {

    private static final Component TITLE = Component.translatable("screen.wildex.title");
    private static final Component SEARCH_LABEL = Component.translatable("gui.wildex.search");

    private static final int INK_COLOR = 0x2B1A10;

    private static final int LIST_GAP = 10;
    private static final int LIST_BOTTOM_CUT = 25;

    private static final int FRAME_BG = 0x22FFFFFF;
    private static final int FRAME_OUTER = 0x88301E14;
    private static final int FRAME_INNER = 0x55FFFFFF;

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

    private static final Component TROPHY_TIP_TITLE = Component.translatable("tooltip.wildex.spyglass_pulse.title");
    private static final List<Component> TROPHY_TOOLTIP = List.of(
            TROPHY_TIP_TITLE,
            Component.translatable("tooltip.wildex.spyglass_pulse.line1"),
            Component.translatable("tooltip.wildex.spyglass_pulse.line2"),
            Component.translatable("tooltip.wildex.spyglass_pulse.line3"),
            Component.empty(),
            Component.translatable("tooltip.wildex.spyglass_pulse.line4")
    );

    private static final int TROPHY_CORNER_X = 6;
    private static final int TROPHY_CORNER_Y = 6;

    private static final int TIP_BG = 0xE61A120C;
    private static final int TIP_BORDER = 0xAA301E14;
    private static final int TIP_TEXT = 0xF2E8D5;
    private static final int TIP_PAD = 4;
    private static final int TIP_LINE_GAP = 2;
    private static final int TIP_MAX_W = 170;

    private static final int STYLE_BUTTON_W = 56;
    private static final int STYLE_BUTTON_H = 14;
    private static final int STYLE_BUTTON_MARGIN = 6;
    private static final int STYLE_BUTTON_Y_OFFSET = 2;

    private static final float VERSION_SCALE = 0.55f;
    private static final int VERSION_COLOR = 0x55301E14;
    private static final float PREVIEW_HINT_SCALE = 0.62f;
    private static final int PREVIEW_HINT_COLOR = 0x88301E14;
    private static final int PREVIEW_HINT_PAD_X = 6;
    private static final int PREVIEW_HINT_PAD_Y = 4;

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
        this.state.setSelectedTab(WildexTab.STATS);
        this.state.setSelectedMobId("");

        int btnW = STYLE_BUTTON_W;
        int btnX = this.width - btnW - STYLE_BUTTON_MARGIN;
        int btnY = STYLE_BUTTON_MARGIN + STYLE_BUTTON_Y_OFFSET;

        this.clearWidgets();

        this.addRenderableWidget(new WildexStyleButton(btnX, btnY, btnW, STYLE_BUTTON_H, () -> {
            DesignStyle next = nextStyle(ClientConfig.INSTANCE.designStyle.get());
            ClientConfig.INSTANCE.designStyle.set(next);
            ClientConfig.SPEC.save();
            mobDataResolver.clearCache();
        }));

        if (CommonConfig.INSTANCE.hiddenMode.get()) {
            WildexNetworkClient.requestDiscoveredMobs();
        }

        WildexScreenLayout.Area search = layout.leftSearchArea();

        this.searchBox = new WildexSearchBox(this.font, search.x(), search.y(), search.w(), search.h(), SEARCH_LABEL);
        this.searchBox.setMaxLength(64);
        this.searchBox.setValue(mobIndex.query());
        this.searchBox.setResponder(this::onSearchChanged);

        this.searchBox.setTextColor(INK_COLOR);
        this.searchBox.setTextColorUneditable(INK_COLOR);
        this.searchBox.setBordered(false);
        this.searchBox.setTextShadow(false);

        this.addRenderableWidget(this.searchBox);

        WildexScreenLayout.Area content = layout.leftContentArea();
        WildexScreenLayout.Area action = layout.leftActionArea();

        if (CommonConfig.INSTANCE.hiddenMode.get()) {
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
        this.addRenderableWidget(new RightTabsWidget(tabsArea.x(), tabsArea.y(), tabsArea.w(), tabsArea.h(), this.state));

        WildexScreenLayout.Area resetArea = layout.previewResetButtonArea();
        this.previewResetButton = new MobPreviewResetButton(
                resetArea.x(),
                resetArea.y(),
                resetArea.w(),
                resetArea.h(),
                mobPreviewRenderer::resetPreview
        );
        this.addRenderableWidget(this.previewResetButton);

        this.lastTab = this.state.selectedTab();
        this.setInitialFocus(this.searchBox);

        this.lastDiscoveryCount = CommonConfig.INSTANCE.hiddenMode.get() ? WildexDiscoveryCache.count() : -1;

        this.localUiStateDirtySinceOpen = false;
        if (WildexPlayerUiStateCache.hasServerState()) {
            applyServerUiState(WildexPlayerUiStateCache.tabId(), WildexPlayerUiStateCache.mobId());
        }
        WildexNetworkClient.requestPlayerUiState();
        requestAllForSelected(this.state.selectedMobId());
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

        if (CommonConfig.INSTANCE.hiddenMode.get()) {
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
            this.state.setSelectedMobId(next);
            rightInfoRenderer.resetSpawnScroll();
            saveUiStateToServer();
            requestAllForSelected(next);
        }
    }

    private void onMobSelected(ResourceLocation id) {
        if (suppressMobSelectionCallback) return;

        String next = id == null ? "" : id.toString();
        state.setSelectedMobId(next);
        rightInfoRenderer.resetSpawnScroll();
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

        if (mobPreviewRenderer.isMouseOverPreview(this.layout, mx, my)) {
            mobPreviewRenderer.adjustZoom(scrollY);
            return true;
        }

        if (this.layout != null && this.state.selectedTab() == WildexTab.SPAWNS) {
            WildexScreenLayout.Area a = this.layout.rightInfoArea();
            if (a != null && mouseX >= a.x() && mouseX < a.x() + a.w() && mouseY >= a.y() && mouseY < a.y() + a.h()) {
                rightInfoRenderer.scrollSpawn(scrollY);
                return true;
            }
        }

        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;

        if (this.layout != null) {
            int mx = (int) Math.floor(mouseX);
            int my = (int) Math.floor(mouseY);
            if (mobPreviewRenderer.isMouseOverPreview(this.layout, mx, my) && mobPreviewRenderer.beginRotationDrag(mx, my, button)) {
                return true;
            }
        }

        if (this.layout != null && this.state.selectedTab() == WildexTab.SPAWNS) {
            int mx = (int) Math.floor(mouseX);
            int my = (int) Math.floor(mouseY);
            if (rightInfoRenderer.handleSpawnMouseClicked(mx, my, button, this.state)) {
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
            int mx = (int) Math.floor(mouseX);
            int my = (int) Math.floor(mouseY);
            if (rightInfoRenderer.handleSpawnMouseDragged(mx, my, button)) {
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
            if (rightInfoRenderer.handleSpawnMouseReleased(button)) {
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
        renderer.render(graphics, layout, state, mouseX, mouseY, partialTick);
        renderVersionLabel(graphics);

        WildexScreenLayout.Area entriesArea = layout.leftEntriesCounterArea();
        Component entriesText = Component.translatable("gui.wildex.entries_count", (this.visibleEntries == null ? 0 : this.visibleEntries.size()));
        int entriesX = (entriesArea.x() + entriesArea.w()) - this.font.width(entriesText);
        int entriesY = entriesArea.y() + ((entriesArea.h() - this.font.lineHeight) / 2);
        graphics.drawString(this.font, entriesText, entriesX, entriesY, INK_COLOR, false);

        if (CommonConfig.INSTANCE.hiddenMode.get()) {
            WildexScreenLayout.Area discArea = layout.leftDiscoveryCounterArea();

            int discovered = WildexDiscoveryCache.count();
            int total = mobIndex.totalCount();

            Component discText = Component.translatable("gui.wildex.discovered_count", discovered, total);

            int discX = discArea.x();
            int discY = discArea.y() + ((discArea.h() - this.font.lineHeight) / 2);
            graphics.drawString(this.font, discText, discX, discY, INK_COLOR, false);
        }

        mobPreviewRenderer.render(graphics, layout, state, mouseX, mouseY, partialTick);
        WildexScreenLayout.Area previewArea = layout.rightPreviewArea();
        WildexScreenLayout.Area previewResetArea = layout.previewResetButtonArea();
        HintBounds previewHint = drawPreviewControlsHint(graphics, previewArea, previewResetArea);

        drawPanelFrame(graphics, layout.rightHeaderArea());
        drawPanelFrame(graphics, layout.rightTabsArea());
        drawPanelFrame(graphics, layout.rightInfoArea());

        WildexMobData data = mobDataResolver.resolve(state.selectedMobId());

        rightHeaderRenderer.render(graphics, this.font, layout.rightHeaderArea(), state, data.header(), INK_COLOR);

        rightInfoRenderer.render(
                graphics,
                this.font,
                layout.rightInfoArea(),
                state,
                data,
                INK_COLOR,
                mouseX,
                mouseY
        );

        super.render(graphics, mouseX, mouseY, partialTick);

        boolean trophyDrawn = false;
        int trophyX = 0;
        int trophyY = 0;

        if (CommonConfig.INSTANCE.hiddenMode.get() && WildexCompletionCache.isComplete()) {
            trophyX = TROPHY_CORNER_X;
            trophyY = TROPHY_CORNER_Y;

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

        if (this.previewResetButton != null && this.previewResetButton.isHovered()) {
            graphics.renderTooltip(this.font, RESET_PREVIEW_TOOLTIP, mouseX, mouseY);
        }

        if (this.discoveredOnlyCheckbox != null && this.discoveredOnlyCheckbox.isHovered()) {
            Component tip = this.discoveredOnlyCheckbox.tooltip();
            if (tip != null) graphics.renderTooltip(this.font, tip, mouseX, mouseY);
        }

        if (trophyDrawn && isMouseOverRect(mouseX, mouseY, trophyX, trophyY)) {
            renderWildexTooltip(graphics, TROPHY_TOOLTIP, mouseX, mouseY);
        }

        if (!mobPreviewRenderer.isDraggingPreview() && isMouseOverPreviewControlsHint(mouseX, mouseY, previewHint)) {
            renderWildexTooltip(graphics, PREVIEW_CONTROLS_TOOLTIP, mouseX, mouseY);
        }
    }

    private HintBounds drawPreviewControlsHint(
            GuiGraphics graphics,
            WildexScreenLayout.Area previewArea,
            WildexScreenLayout.Area previewResetArea
    ) {
        if (previewArea == null) return null;

        int hintBaseX = previewArea.x() + PREVIEW_HINT_PAD_X;
        int textW = this.font.width(PREVIEW_CONTROLS_LABEL);
        int scaledTextW = Math.max(1, Math.round(textW * PREVIEW_HINT_SCALE));
        int scaledTextH = Math.max(1, Math.round(this.font.lineHeight * PREVIEW_HINT_SCALE));
        int hintBaseY = (previewArea.y() + previewArea.h()) - PREVIEW_HINT_PAD_Y - scaledTextH;
        int drawX = hintBaseX;
        float inv = 1.0f / PREVIEW_HINT_SCALE;

        int rightBound = (previewArea.x() + previewArea.w()) - PREVIEW_HINT_PAD_X;
        if (previewResetArea != null) {
            rightBound = Math.min(rightBound, previewResetArea.x() - 2);
        }
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
                PREVIEW_HINT_COLOR,
                false
        );
        graphics.disableScissor();
        graphics.pose().popPose();

        return new HintBounds(hintBaseX, hintBaseY, Math.min(scaledTextW, availableW), scaledTextH);
    }

    private void renderVersionLabel(GuiGraphics graphics) {
        if (this.versionLabel.isBlank()) return;

        int btnX = this.width - STYLE_BUTTON_W - STYLE_BUTTON_MARGIN;
        int btnY = STYLE_BUTTON_MARGIN + STYLE_BUTTON_Y_OFFSET;

        int scaledTextW = Math.max(1, Math.round(this.font.width(this.versionLabel) * VERSION_SCALE));
        int scaledTextH = Math.max(1, Math.round(this.font.lineHeight * VERSION_SCALE));

        int x = btnX + STYLE_BUTTON_W - scaledTextW;
        int y = Math.max(0, btnY - scaledTextH - 1);

        float inv = 1.0f / VERSION_SCALE;

        graphics.pose().pushPose();
        graphics.pose().scale(VERSION_SCALE, VERSION_SCALE, 1.0f);
        graphics.drawString(this.font, this.versionLabel, Math.round(x * inv), Math.round(y * inv), VERSION_COLOR, false);
        graphics.pose().popPose();
    }

    private void renderWildexTooltip(GuiGraphics g, List<Component> lines, int mouseX, int mouseY) {
        if (lines.isEmpty()) return;

        int maxW = Math.max(80, Math.min(TIP_MAX_W, this.width - (TIP_PAD * 2) - 8));
        ArrayList<FormattedCharSequence> wrapped = new ArrayList<>();

        for (Component c : lines) {
            if (c == null) continue;
            if (c.getString().isEmpty()) {
                wrapped.add(FormattedCharSequence.EMPTY);
                continue;
            }
            wrapped.addAll(this.font.split(c, maxW));
        }

        if (wrapped.isEmpty()) return;

        int textW = 0;
        for (FormattedCharSequence s : wrapped) textW = Math.max(textW, this.font.width(s));
        int textH = wrapped.size() * this.font.lineHeight + Math.max(0, (wrapped.size() - 1) * TIP_LINE_GAP);

        int boxW = textW + TIP_PAD * 2;
        int boxH = textH + TIP_PAD * 2;

        int x = mouseX + 10;
        int y = mouseY + 10;

        int minX = 2;
        int maxX = this.width - boxW - 2;
        int minY = 2;
        int maxY = this.height - boxH - 2;

        if (x > maxX) x = maxX;
        if (x < minX) x = minX;
        if (y > maxY) y = maxY;
        if (y < minY) y = minY;

        int x0 = x;
        int y0 = y;
        int x1 = x + boxW;
        int y1 = y + boxH;

        g.fill(x0, y0, x1, y1, TIP_BG);

        g.fill(x0, y0, x1, y0 + 1, TIP_BORDER);
        g.fill(x0, y1 - 1, x1, y1, TIP_BORDER);
        g.fill(x0, y0, x0 + 1, y1, TIP_BORDER);
        g.fill(x1 - 1, y0, x1, y1, TIP_BORDER);

        int tx = x0 + TIP_PAD;
        int ty = y0 + TIP_PAD;

        for (FormattedCharSequence line : wrapped) {
            g.drawString(this.font, line, tx, ty, TIP_TEXT, false);
            ty += this.font.lineHeight + TIP_LINE_GAP;
        }
    }

    private static boolean isMouseOverRect(int mouseX, int mouseY, int x, int y) {
        return mouseX >= x && mouseX < x + TROPHY_DRAW_SIZE && mouseY >= y && mouseY < y + TROPHY_DRAW_SIZE;
    }

    private static boolean isMouseOverPreviewControlsHint(int mouseX, int mouseY, HintBounds hint) {
        if (hint == null || hint.w() <= 0 || hint.h() <= 0) return false;
        return mouseX >= hint.x() && mouseX < hint.x() + hint.w() && mouseY >= hint.y() && mouseY < hint.y() + hint.h();
    }

    private record HintBounds(int x, int y, int w, int h) {
    }

    private static void drawPanelFrame(GuiGraphics graphics, WildexScreenLayout.Area a) {
        if (a == null) return;

        int x0 = a.x();
        int y0 = a.y();
        int x1 = a.x() + a.w();
        int y1 = a.y() + a.h();

        graphics.fill(x0 + 2, y0 + 2, x1 - 2, y1 - 2, FRAME_BG);

        graphics.fill(x0, y0, x1, y0 + 1, FRAME_OUTER);
        graphics.fill(x0, y1 - 1, x1, y1, FRAME_OUTER);
        graphics.fill(x0, y0, x0 + 1, y1, FRAME_OUTER);
        graphics.fill(x1 - 1, y0, x1, y1, FRAME_OUTER);

        graphics.fill(x0 + 1, y0 + 1, x1 - 1, y0 + 2, FRAME_INNER);
        graphics.fill(x0 + 1, y1 - 2, x1 - 1, y1 - 1, FRAME_INNER);
        graphics.fill(x0 + 1, y0 + 1, x0 + 2, y1 - 1, FRAME_INNER);
        graphics.fill(x1 - 2, y0 + 1, x1 - 1, y1 - 1, FRAME_INNER);
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

    private static DesignStyle nextStyle(DesignStyle current) {
        return (current == DesignStyle.VINTAGE) ? DesignStyle.MODERN : DesignStyle.VINTAGE;
    }

    private static String resolveVersionLabel() {
        String v = ModList.get()
                .getModContainerById(Wildex.MODID)
                .map(c -> c.getModInfo().getVersion().toString())
                .orElse("unknown");
        return "v" + v;
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
