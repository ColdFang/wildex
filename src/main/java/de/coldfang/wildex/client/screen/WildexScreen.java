package de.coldfang.wildex.client.screen;

import de.coldfang.wildex.client.data.WildexCompletionCache;
import de.coldfang.wildex.client.data.WildexDiscoveryCache;
import de.coldfang.wildex.client.data.WildexMobDataResolver;
import de.coldfang.wildex.client.data.WildexMobIndexModel;
import de.coldfang.wildex.client.data.model.WildexMobData;
import de.coldfang.wildex.config.ClientConfig;
import de.coldfang.wildex.config.ClientConfig.DesignStyle;
import de.coldfang.wildex.config.CommonConfig;
import de.coldfang.wildex.network.WildexNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.EntityType;

import java.util.ArrayList;
import java.util.List;

public final class WildexScreen extends Screen {

    private static final Component TITLE = Component.literal("Wildex");
    private static final Component SEARCH_LABEL = Component.literal("Search");

    private static final int INK_COLOR = 0x2B1A10;

    private static final int LIST_GAP = 10;
    private static final int LIST_BOTTOM_CUT = 25;

    private static final int FRAME_BG = 0x22FFFFFF;
    private static final int FRAME_OUTER = 0x88301E14;
    private static final int FRAME_INNER = 0x55FFFFFF;

    private static final Component RESET_ZOOM_TOOLTIP = Component.literal("Reset Zoom");
    private static final Component DISCOVERED_ONLY_TOOLTIP = Component.literal("Show only discovered mobs");

    private static final ResourceLocation TROPHY_ICON =
            ResourceLocation.fromNamespaceAndPath("wildex", "textures/gui/trophy.png");
    private static final int TROPHY_TEX_SIZE = 128;
    private static final int TROPHY_DRAW_SIZE = 32;

    private static final Component TROPHY_TIP_TITLE = Component.literal("Spyglass Pulse");
    private static final List<Component> TROPHY_TOOLTIP = List.of(
            TROPHY_TIP_TITLE,
            Component.literal("Granted by completing the Wildex:"),
            Component.literal("While holding a Spyglass,"),
            Component.literal("left-click to reveal nearby mobs."),
            Component.literal(""),
            Component.literal("Cooldown: 15s • Radius: 32 blocks • Duration: 10s")
    );

    private static final int TROPHY_CORNER_X = 6;
    private static final int TROPHY_CORNER_Y = 6;

    private static final int TIP_BG = 0xE61A120C;
    private static final int TIP_BORDER = 0xAA301E14;
    private static final int TIP_TEXT = 0xF2E8D5;
    private static final int TIP_PAD = 4;
    private static final int TIP_LINE_GAP = 2;
    private static final int TIP_MAX_W = 170;

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
    private RightTabsWidget rightTabs;
    private MobPreviewResetButton previewResetButton;
    private WildexDiscoveredOnlyCheckbox discoveredOnlyCheckbox;

    private List<EntityType<?>> visibleEntries = List.of();

    private WildexTab lastTab = WildexTab.STATS;
    private int lastDiscoveryCount = -1;

    public WildexScreen() {
        super(TITLE);
    }

    @Override
    protected void init() {
        this.layout = WildexScreenLayout.compute(this.width, this.height);

        int btnW = 56;
        int btnH = 14;
        int btnX = this.width - btnW - 6;
        int btnY = 6;

        this.clearWidgets();

        this.addRenderableWidget(new WildexStyleButton(btnX, btnY, btnW, btnH, () -> {
            DesignStyle next = nextStyle(ClientConfig.INSTANCE.designStyle.get());
            ClientConfig.INSTANCE.designStyle.set(next);
            ClientConfig.SPEC.save();
            mobDataResolver.clearCache();
        }));

        if (CommonConfig.INSTANCE.hiddenMode.get()) {
            WildexNetwork.requestDiscoveredMobs();
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

        applyFiltersFromUi();
        refreshMobList();

        String selected = state.selectedMobId();
        if (selected != null && !selected.isBlank()) {
            this.mobList.setSelectedId(ResourceLocation.parse(selected));
        }

        WildexScreenLayout.Area tabsArea = layout.rightTabsArea();
        this.rightTabs = new RightTabsWidget(tabsArea.x(), tabsArea.y(), tabsArea.w(), tabsArea.h(), this.state);
        this.addRenderableWidget(this.rightTabs);

        WildexScreenLayout.Area resetArea = layout.previewResetButtonArea();
        this.previewResetButton = new MobPreviewResetButton(
                resetArea.x(),
                resetArea.y(),
                resetArea.w(),
                resetArea.h(),
                mobPreviewRenderer::resetZoom
        );
        this.addRenderableWidget(this.previewResetButton);

        this.lastTab = this.state.selectedTab();
        this.setInitialFocus(this.searchBox);

        this.lastDiscoveryCount = CommonConfig.INSTANCE.hiddenMode.get() ? WildexDiscoveryCache.count() : -1;

        requestAllForSelected(this.state.selectedMobId());
    }

    private void onSearchChanged(String ignored) {
        applyFiltersFromUi();
        refreshMobList();
    }

    private void requestAllForSelected(String mobId) {
        if (mobId == null || mobId.isBlank()) return;
        WildexNetwork.requestKillsForSelected(mobId);
        WildexNetwork.requestLootForSelected(mobId);
        WildexNetwork.requestSpawnsForSelected(mobId);
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
            requestAllForSelected(this.state.selectedMobId());
        }

        if (this.mobList == null) return;

        ResourceLocation id = this.mobList.selectedId();
        String next = id == null ? "" : id.toString();

        if (!next.equals(this.state.selectedMobId())) {
            this.state.setSelectedMobId(next);
            rightInfoRenderer.resetSpawnScroll();
            requestAllForSelected(next);
        }
    }

    private void onMobSelected(ResourceLocation id) {
        String next = id == null ? "" : id.toString();
        state.setSelectedMobId(next);
        rightInfoRenderer.resetSpawnScroll();
        requestAllForSelected(next);
    }

    private void onDebugDiscoverMob(ResourceLocation mobId) {
        if (mobId == null) return;
        WildexNetwork.sendDebugDiscoverMob(mobId);
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
                int logicalViewportH = Math.max(1, a.h());
                int selectedContentH = Math.max(1, a.h() * 4);
                rightInfoRenderer.scrollSpawn(scrollY, logicalViewportH, selectedContentH);
                return true;
            }
        }

        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderer.render(graphics, layout, state, mouseX, mouseY, partialTick);

        WildexScreenLayout.Area entriesArea = layout.leftEntriesCounterArea();
        String entriesText = "Entries: " + (this.visibleEntries == null ? 0 : this.visibleEntries.size());
        int entriesX = (entriesArea.x() + entriesArea.w()) - this.font.width(entriesText);
        int entriesY = entriesArea.y() + ((entriesArea.h() - this.font.lineHeight) / 2);
        graphics.drawString(this.font, entriesText, entriesX, entriesY, INK_COLOR, false);

        if (CommonConfig.INSTANCE.hiddenMode.get()) {
            WildexScreenLayout.Area discArea = layout.leftDiscoveryCounterArea();

            int discovered = WildexDiscoveryCache.count();
            int total = mobIndex.totalCount();

            String discText = "Discovered: " + discovered + " / " + total;

            int discX = discArea.x();
            int discY = discArea.y() + ((discArea.h() - this.font.lineHeight) / 2);
            graphics.drawString(this.font, discText, discX, discY, INK_COLOR, false);
        }

        mobPreviewRenderer.render(graphics, layout, state, mouseX, mouseY, partialTick);

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

            float s = (float) TROPHY_DRAW_SIZE / (float) TROPHY_TEX_SIZE;

            graphics.pose().pushPose();
            graphics.pose().translate(trophyX, trophyY, 0);
            graphics.pose().scale(s, s, 1.0f);

            graphics.blit(
                    RenderType::guiTextured,
                    TROPHY_ICON,
                    0,
                    0,
                    0.0f,
                    0.0f,
                    TROPHY_TEX_SIZE,
                    TROPHY_TEX_SIZE,
                    TROPHY_TEX_SIZE,
                    TROPHY_TEX_SIZE
            );

            graphics.pose().popPose();

            trophyDrawn = true;
        }


        if (this.previewResetButton != null && this.previewResetButton.isHovered()) {
            graphics.renderTooltip(this.font, RESET_ZOOM_TOOLTIP, mouseX, mouseY);
        }

        if (this.discoveredOnlyCheckbox != null && this.discoveredOnlyCheckbox.isHovered()) {
            Component tip = this.discoveredOnlyCheckbox.tooltip();
            if (tip != null) graphics.renderTooltip(this.font, tip, mouseX, mouseY);
        }

        if (trophyDrawn && isMouseOverRect(mouseX, mouseY, trophyX, trophyY, TROPHY_DRAW_SIZE, TROPHY_DRAW_SIZE)) {
            renderWildexTooltip(graphics, TROPHY_TOOLTIP, mouseX, mouseY);
        }
    }

    private void renderWildexTooltip(GuiGraphics g, List<Component> lines, int mouseX, int mouseY) {
        if (lines == null || lines.isEmpty()) return;

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

    private static boolean isMouseOverRect(int mouseX, int mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
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
}
