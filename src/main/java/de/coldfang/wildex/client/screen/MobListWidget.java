package de.coldfang.wildex.client.screen;

import de.coldfang.wildex.client.WildexClientConfigView;
import de.coldfang.wildex.client.data.WildexDiscoveryCache;
import de.coldfang.wildex.client.data.WildexEntityDisplayNameResolver;
import de.coldfang.wildex.client.data.WildexEntityVariantCatalog;
import de.coldfang.wildex.client.data.WildexEntityVariantCatalog.ProbeState;
import de.coldfang.wildex.client.data.WildexEntityVariantCatalog.SupportState;
import de.coldfang.wildex.client.data.WildexEntityVariantProbe;
import de.coldfang.wildex.client.data.WildexViewedMobEntriesCache;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class MobListWidget extends ObjectSelectionList<MobListWidget.Entry> {

    private static final int ITEM_H = 18;

    private static final int SCROLLBAR_W = 16;
    private static final int TEXT_PAD_X = 10;
    private static final int TEXT_NUDGE_Y = 1;

    private static final int MARQUEE_GAP_PX = 18;
    private static final float MARQUEE_SPEED_PX_PER_SEC = 22.0f;
    private static final int MARQUEE_PAUSE_MS = 650;

    private static final String HIDDEN_LABEL = "???";

    private static final int DEBUG_CB_SIZE = 9;
    private static final int DEBUG_CB_PAD = 6;
    private static final ResourceLocation EXPERIENCE_ORB_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/experience_orb.png");
    private static final Component NEW_BADGE_LABEL = Component.translatable("gui.wildex.list.new");
    private static final int NEW_BADGE_ORB_SIZE = 10;
    private static final int NEW_BADGE_PAD_LEFT = 4;
    private static final int NEW_BADGE_PAD_RIGHT = 6;
    private static final int NEW_BADGE_GAP = 3;

    private static final int EXPAND_BTN_MIN_SIZE = 12;
    private static final int EXPAND_BTN_MAX_SIZE = 22;
    private static final float EXPAND_BTN_ROW_FACTOR = 0.82f;
    private static final float EXPAND_BTN_GAP_FACTOR = 0.28f;
    private static final int EXPAND_BTN_MIN_GAP = 3;
    private static final int SUBENTRY_INDENT = 11;
    private static final int EXPAND_BTN_BG = 0xCC1A120C;
    private static final int EXPAND_BTN_SYMBOL = 0xFFFFF6E8;
    private static final String VARIANT_GROUP_DERIVED = "derived";
    private static final long VARIANT_UNSUPPORTED_RETRY_MS = 10_000L;
    private static final Component VARIANT_LOADING_LABEL = Component.translatable("gui.wildex.variants.loading");
    private static final Set<ResourceLocation> KNOWN_VARIANT_SUPPORTED_IDS = ConcurrentHashMap.newKeySet();

    private final Consumer<Selection> onSelect;
    private final Consumer<ResourceLocation> onEntryClicked;
    private final Consumer<ResourceLocation> onDebugDiscover;

    private List<EntityType<?>> sourceTypes = List.of();
    private final Map<ResourceLocation, EntityType<?>> typeById = new HashMap<>();
    private final Set<ResourceLocation> expandedIds = new HashSet<>();
    private final Set<ResourceLocation> variantUnsupportedIds = new HashSet<>();
    private final Map<ResourceLocation, Long> variantUnsupportedRetryAtMs = new HashMap<>();
    private final Set<String> expandedVariantGroupKeys = new HashSet<>();
    private long variantCatalogRevisionSeen = -1L;

    private ResourceLocation selectedId;
    private String selectedVariantOptionId = "";

    private boolean draggingScrollbar = false;
    private int scrollbarDragOffsetY = 0;

    public MobListWidget(
            Minecraft mc,
            int x,
            int y,
            int w,
            int h,
            int itemHeightPx,
            Consumer<Selection> onSelect,
            Consumer<ResourceLocation> onEntryClicked,
            Consumer<ResourceLocation> onDebugDiscover
    ) {
        super(mc, w, h, y, Math.max(14, itemHeightPx));
        this.onSelect = onSelect == null ? s -> {
        } : onSelect;
        this.onEntryClicked = onEntryClicked == null ? id -> {
        } : onEntryClicked;
        this.onDebugDiscover = onDebugDiscover == null ? id -> {
        } : onDebugDiscover;
        this.setX(x);
        this.setRenderHeader(false, 0);
    }

    public static int itemHeightForUiScale(float uiScale) {
        return Math.max(14, Math.round(ITEM_H * WildexUiScale.clamp(uiScale)));
    }

    public static void clearVariantUiCache() {
        KNOWN_VARIANT_SUPPORTED_IDS.clear();
    }

    public void setEntries(List<EntityType<?>> types) {
        ResourceLocation keepId = this.selectedId;
        String keepVariant = this.selectedVariantOptionId;

        this.sourceTypes = types == null ? List.of() : List.copyOf(types);
        this.variantCatalogRevisionSeen = WildexEntityVariantCatalog.cacheRevision();
        rebuildEntries();

        if (keepId != null && setSelectedSelection(keepId, keepVariant)) {
            return;
        }

        if (this.getSelected() == null && !this.children().isEmpty()) {
            Entry first = this.children().getFirst();
            this.setSelected(first);
            this.centerScrollOn(first);
        } else {
            syncSelectionFromSelected();
        }
    }

    public boolean setSelectedSelection(ResourceLocation id, String variantOptionId) {
        return setSelectedSelection(id, variantOptionId, true);
    }

    private boolean setSelectedSelection(ResourceLocation id, String variantOptionId, boolean centerOnFound) {
        this.selectedId = id;
        this.selectedVariantOptionId = variantOptionId == null ? "" : variantOptionId;

        if (id == null) {
            this.setSelected(null);
            return false;
        }

        if (!this.selectedVariantOptionId.isBlank()) {
            for (Entry e : this.children()) {
                if (!id.equals(e.id)) continue;
                if (!this.selectedVariantOptionId.equals(e.variantOptionId)) continue;
                this.setSelected(e);
                if (centerOnFound) this.centerScrollOn(e);
                return true;
            }
        }

        for (Entry e : this.children()) {
            if (!id.equals(e.id)) continue;
            if (e.variantSubentry) continue;
            this.setSelected(e);
            if (centerOnFound) this.centerScrollOn(e);
            return true;
        }

        this.setSelected(null);
        return false;
    }

    public void setSelectedId(ResourceLocation id) {
        setSelectedSelection(id, "");
    }

    public ResourceLocation selectedId() {
        return this.selectedId;
    }

    public String selectedVariantOptionId() {
        return this.selectedVariantOptionId;
    }

    @Override
    public void setSelected(Entry entry) {
        super.setSelected(entry);
        syncSelectionFromSelected();
        if (this.selectedId != null) {
            this.onSelect.accept(new Selection(this.selectedId, this.selectedVariantOptionId));
        }
    }

    @Override
    public int getRowLeft() {
        return this.getX();
    }

    @Override
    public int getRowWidth() {
        return this.width - SCROLLBAR_W;
    }

    @Override
    protected int getScrollbarPosition() {
        return this.getX() + this.width - SCROLLBAR_W;
    }

    @Override
    protected boolean scrollbarVisible() {
        return false;
    }

    @Override
    protected void renderListBackground(@NotNull GuiGraphics graphics) {
    }

    @Override
    protected void renderListSeparators(@NotNull GuiGraphics graphics) {
    }

    @Override
    protected void renderSelection(@NotNull GuiGraphics graphics, int y, int rowWidth, int itemHeight, int borderColor, int innerColor) {
    }

    @Override
    protected void enableScissor(@NotNull GuiGraphics graphics) {
        WildexScissor.enablePhysical(graphics, this.getX(), this.getY(), this.getRight(), this.getBottom());
    }

    @Override
    protected void updateScrollingState(double mouseX, double mouseY, int button) {
    }

    @Override
    public void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        refreshVariantProbeStateIfNeeded();
        super.renderWidget(graphics, mouseX, mouseY, partialTick);
        renderCustomScrollbar(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && hasCustomScrollbar() && isInScrollbar(mouseX, mouseY)) {
            int my = (int) Math.floor(mouseY);
            int thumbY = scrollbarThumbY();
            int thumbH = scrollbarThumbHeight();
            if (my >= thumbY && my < thumbY + thumbH) {
                this.draggingScrollbar = true;
                this.scrollbarDragOffsetY = my - thumbY;
            } else {
                setScrollFromThumbTop(my - (thumbH / 2));
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && this.draggingScrollbar) {
            int my = (int) Math.floor(mouseY);
            setScrollFromThumbTop(my - this.scrollbarDragOffsetY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && this.draggingScrollbar) {
            this.draggingScrollbar = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void renderCustomScrollbar(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {
        if (!hasCustomScrollbar()) {
            this.draggingScrollbar = false;
            return;
        }

        WildexUiTheme.Palette theme = WildexUiTheme.current();
        int x0 = getScrollbarPosition();
        int x1 = x0 + SCROLLBAR_W;
        int y0 = this.getY();
        int y1 = this.getBottom();
        graphics.fill(x0, y0, x1, y1, theme.scrollTrack());

        int thumbY = scrollbarThumbY();
        int thumbH = scrollbarThumbHeight();
        boolean hovered = isInScrollbar(mouseX, mouseY);
        int thumbColor = hovered || this.draggingScrollbar ? brighten(theme.scrollThumb()) : theme.scrollThumb();
        graphics.fill(x0, thumbY, x1, thumbY + thumbH, thumbColor);
    }

    private boolean hasCustomScrollbar() {
        return this.getMaxScroll() > 0;
    }

    private boolean isInScrollbar(double mouseX, double mouseY) {
        int x0 = getScrollbarPosition();
        int x1 = x0 + SCROLLBAR_W;
        int y0 = this.getY();
        int y1 = this.getBottom();
        return mouseX >= x0 && mouseX < x1 && mouseY >= y0 && mouseY < y1;
    }

    private int scrollbarThumbHeight() {
        int h = this.getHeight();
        int thumbH = Mth.clamp((int) ((float) (h * h) / (float) this.getMaxPosition()), 12, Math.max(12, h - 8));
        return Math.max(12, Math.min(h, thumbH));
    }

    private int scrollbarThumbY() {
        int maxScroll = this.getMaxScroll();
        int y0 = this.getY();
        if (maxScroll <= 0) return y0;
        int thumbH = scrollbarThumbHeight();
        int travel = Math.max(1, this.getHeight() - thumbH);
        float t = (float) this.getScrollAmount() / (float) maxScroll;
        return y0 + Math.round(travel * t);
    }

    private void setScrollFromThumbTop(int desiredThumbTop) {
        int thumbH = scrollbarThumbHeight();
        int y0 = this.getY();
        int y1 = this.getBottom();
        int clampedTop = Mth.clamp(desiredThumbTop, y0, y1 - thumbH);
        int travel = Math.max(1, (y1 - y0) - thumbH);
        float t = (clampedTop - y0) / (float) travel;
        this.setScrollAmount(t * this.getMaxScroll());
    }

    private static int brighten(int argb) {
        final int delta = 20;
        int a = (argb >>> 24) & 0xFF;
        int r = Math.min(255, ((argb >>> 16) & 0xFF) + delta);
        int g = Math.min(255, ((argb >>> 8) & 0xFF) + delta);
        int b = Math.min(255, (argb & 0xFF) + delta);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private void syncSelectionFromSelected() {
        Entry sel = this.getSelected();
        if (sel == null) {
            this.selectedId = null;
            this.selectedVariantOptionId = "";
            return;
        }
        this.selectedId = sel.id;
        this.selectedVariantOptionId = sel.variantOptionId == null ? "" : sel.variantOptionId;
    }

    private void refreshVariantProbeStateIfNeeded() {
        if (!WildexClientConfigView.showMobVariants()) return;
        if (!WildexClientConfigView.backgroundMobVariantProbe()) return;
        if (this.expandedIds.isEmpty()) return;

        long revision = WildexEntityVariantCatalog.cacheRevision();
        if (revision == this.variantCatalogRevisionSeen) return;
        this.variantCatalogRevisionSeen = revision;

        ResourceLocation keepId = this.selectedId;
        String keepVariant = this.selectedVariantOptionId;
        double keepScrollAmount = this.getScrollAmount();

        rebuildEntries();
        if (keepId != null) {
            setSelectedSelection(keepId, keepVariant, false);
        }
        this.setScrollAmount(Math.min(keepScrollAmount, this.getMaxScroll()));
    }

    private static boolean debugDiscoverEnabled() {
        return WildexClientConfigView.hiddenMode() && WildexClientConfigView.debugMode();
    }

    private static float resolveListTextScale() {
        float s = WildexUiScale.get();
        if (s <= 0.62f) return 0.72f;
        if (s <= 0.74f) return 0.80f;
        if (s <= 0.86f) return 0.90f;
        return 1.0f;
    }

    private static float marqueeOffset(long nowMs, int travelPx, int phasePx) {
        if (travelPx <= 0) return 0.0f;

        long cycleMs = (long) Math.ceil((travelPx / MARQUEE_SPEED_PX_PER_SEC) * 1000.0);
        long totalMs = (long) MARQUEE_PAUSE_MS + cycleMs + (long) MARQUEE_PAUSE_MS;

        long t = nowMs + (long) phasePx * 17L;
        long p = t % totalMs;
        if (p < 0) p += totalMs;

        if (p < MARQUEE_PAUSE_MS) return 0.0f;

        long moving = p - MARQUEE_PAUSE_MS;
        if (moving >= cycleMs) return (float) travelPx;

        float seconds = moving / 1000.0f;
        float off = seconds * MARQUEE_SPEED_PX_PER_SEC;
        if (off < 0) return 0.0f;
        if (off > travelPx) return travelPx;
        return off;
    }

    private static void renderDebugCheckbox(GuiGraphics graphics, int cbX, int cbY) {
        WildexUiTheme.Palette theme = WildexUiTheme.current();
        graphics.fill(cbX, cbY, cbX + DEBUG_CB_SIZE, cbY + DEBUG_CB_SIZE, theme.frameBg());

        graphics.fill(cbX, cbY, cbX + DEBUG_CB_SIZE, cbY + 1, theme.frameOuter());
        graphics.fill(cbX, cbY + DEBUG_CB_SIZE - 1, cbX + DEBUG_CB_SIZE, cbY + DEBUG_CB_SIZE, theme.frameOuter());
        graphics.fill(cbX, cbY, cbX + 1, cbY + DEBUG_CB_SIZE, theme.frameOuter());
        graphics.fill(cbX + DEBUG_CB_SIZE - 1, cbY, cbX + DEBUG_CB_SIZE, cbY + DEBUG_CB_SIZE, theme.frameOuter());

        int plus = WildexThemes.isVintageLayout() ? theme.ink() : theme.buttonInk();
        graphics.fill(cbX + 2, cbY + 4, cbX + DEBUG_CB_SIZE - 2, cbY + 5, plus);
        graphics.fill(cbX + 4, cbY + 2, cbX + 5, cbY + DEBUG_CB_SIZE - 2, plus);
    }

    private static int newBadgeWidth(Font font) {
        int labelW = WildexUiText.width(font, NEW_BADGE_LABEL);
        return NEW_BADGE_PAD_LEFT + NEW_BADGE_ORB_SIZE + NEW_BADGE_GAP + labelW + NEW_BADGE_PAD_RIGHT;
    }

    private static void renderNewBadge(GuiGraphics graphics, Font font, int rightX, int y, int rowHeight, boolean selected) {
        int badgeW = newBadgeWidth(font);
        int badgeX = rightX - badgeW;

        int orbX = badgeX + NEW_BADGE_PAD_LEFT;
        int orbY = y + ((rowHeight - NEW_BADGE_ORB_SIZE) / 2);
        long nowMs = System.currentTimeMillis();
        int frame = (int) ((nowMs / 120L) & 15L);
        int u = (frame & 3) * 16;
        int v = (frame >> 2) * 16;
        float phase = (nowMs / 50.0f) * 0.5f;
        float red = (float) ((Math.sin(phase) + 1.0) * 0.5);
        float blue = (float) ((Math.sin(phase + (Math.PI * 4.0 / 3.0)) + 1.0) * 0.1);
        graphics.setColor(red, 1.0f, blue, 1.0f);
        graphics.blit(
                EXPERIENCE_ORB_TEXTURE,
                orbX,
                orbY,
                NEW_BADGE_ORB_SIZE,
                NEW_BADGE_ORB_SIZE,
                (float) u,
                (float) v,
                16,
                16,
                64,
                64
        );
        graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);

        WildexUiTheme.Palette theme = WildexUiTheme.current();
        int textColor = selected ? theme.inkOnDark() : theme.ink();
        int textX = orbX + NEW_BADGE_ORB_SIZE + NEW_BADGE_GAP;
        int textY = y + ((rowHeight - WildexUiText.lineHeight(font)) / 2) + TEXT_NUDGE_Y;
        WildexUiText.draw(graphics, font, NEW_BADGE_LABEL, textX, textY, textColor, false);
    }

    private void rebuildEntries() {
        this.clearEntries();
        this.typeById.clear();

        Level level = this.minecraft.level;
        if (level == null) return;
        boolean hiddenMode = WildexClientConfigView.hiddenMode();
        boolean variantEntriesEnabled = WildexClientConfigView.showMobVariants();
        if (!variantEntriesEnabled) {
            this.expandedIds.clear();
            this.expandedVariantGroupKeys.clear();
            this.variantUnsupportedIds.clear();
            this.variantUnsupportedRetryAtMs.clear();
        }
        long nowMs = System.currentTimeMillis();

        for (EntityType<?> type : sourceTypes) {
            ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);

            this.typeById.put(id, type);
            Component name = WildexEntityDisplayNameResolver.resolve(type);
            boolean discovered = !hiddenMode || WildexDiscoveryCache.isDiscovered(id);
            boolean unsupportedCoolingDown = false;
            if (this.variantUnsupportedIds.contains(id)) {
                Long retryAt = this.variantUnsupportedRetryAtMs.get(id);
                if (retryAt != null && nowMs < retryAt) {
                    unsupportedCoolingDown = true;
                } else {
                    this.variantUnsupportedIds.remove(id);
                    this.variantUnsupportedRetryAtMs.remove(id);
                }
            }
            boolean expandable = variantEntriesEnabled && discovered && !unsupportedCoolingDown;
            this.addEntry(newBaseEntry(type, id, name, expandable));

            if (!variantEntriesEnabled || !discovered || !this.expandedIds.contains(id)) continue;
            VariantOptionsResolveResult resolved = resolveVariantOptions(type, level);
            if (resolved.pending()) {
                this.addEntry(newVariantLoadingEntry(type, id));
                continue;
            }
            List<WildexEntityVariantProbe.VariantOption> options = resolved.options();
            if (options == null || options.isEmpty()) {
                this.expandedIds.remove(id);
                this.variantUnsupportedIds.add(id);
                this.variantUnsupportedRetryAtMs.put(id, nowMs + VARIANT_UNSUPPORTED_RETRY_MS);
                KNOWN_VARIANT_SUPPORTED_IDS.remove(id);
                clearVariantGroupExpansionsFor(id);
                continue;
            }
            this.variantUnsupportedIds.remove(id);
            this.variantUnsupportedRetryAtMs.remove(id);
            KNOWN_VARIANT_SUPPORTED_IDS.add(id);

            VariantBuckets buckets = splitVariantBuckets(options);
            for (WildexEntityVariantProbe.VariantOption option : buckets.primary()) {
                Component optionName = Component.literal(option.label());
                this.addEntry(newVariantEntry(type, id, optionName, option.id(), 1));
            }

            if (!buckets.derived().isEmpty()) {
                Component groupName = Component.literal(derivedGroupLabel(buckets.derived()));
                this.addEntry(newVariantGroupEntry(type, id, groupName));
                if (isVariantGroupExpanded(id, VARIANT_GROUP_DERIVED)) {
                    for (WildexEntityVariantProbe.VariantOption option : buckets.derived()) {
                        Component optionName = Component.literal(option.label());
                        this.addEntry(newVariantEntry(type, id, optionName, option.id(), 2));
                    }
                }
            }
        }
    }

    private Entry newBaseEntry(EntityType<?> type, ResourceLocation id, Component name, boolean expandable) {
        return new Entry(type, id, name, "", false, expandable, false, "", 0);
    }

    private Entry newVariantEntry(EntityType<?> type, ResourceLocation id, Component name, String variantOptionId, int depth) {
        return new Entry(type, id, name, variantOptionId, true, false, false, "", Math.max(1, depth));
    }

    private Entry newVariantLoadingEntry(EntityType<?> type, ResourceLocation id) {
        return new Entry(type, id, VARIANT_LOADING_LABEL, "__loading__", true, false, false, "", 1);
    }

    private Entry newVariantGroupEntry(EntityType<?> type, ResourceLocation id, Component name) {
        return new Entry(type, id, name, "", true, true, true, VARIANT_GROUP_DERIVED, 1);
    }

    private void toggleExpanded(Entry entry) {
        if (entry == null || entry.id == null) return;
        if (!WildexClientConfigView.showMobVariants()) return;

        ResourceLocation keepId = this.selectedId;
        String keepVariant = this.selectedVariantOptionId;
        double keepScrollAmount = this.getScrollAmount();

        if (entry.variantGroupHeader) {
            String key = variantGroupExpansionKey(entry.id, entry.variantGroupKey);
            if (this.expandedVariantGroupKeys.contains(key)) {
                this.expandedVariantGroupKeys.remove(key);
            } else {
                this.expandedVariantGroupKeys.add(key);
            }
        } else {
            if (entry.variantSubentry) return;

            EntityType<?> type = this.typeById.get(entry.id);
            if (type == null) return;

            if (this.expandedIds.contains(entry.id)) {
                this.expandedIds.remove(entry.id);
                clearVariantGroupExpansionsFor(entry.id);
            } else {
                Level level = this.minecraft.level;
                if (level == null) return;

                VariantOptionsResolveResult resolved = resolveVariantOptions(type, level);
                if (resolved.pending()) {
                    this.variantUnsupportedIds.remove(entry.id);
                    this.variantUnsupportedRetryAtMs.remove(entry.id);
                    this.expandedIds.add(entry.id);
                } else if (resolved.options() == null || resolved.options().isEmpty()) {
                    this.variantUnsupportedIds.add(entry.id);
                    this.variantUnsupportedRetryAtMs.put(entry.id, System.currentTimeMillis() + VARIANT_UNSUPPORTED_RETRY_MS);
                    KNOWN_VARIANT_SUPPORTED_IDS.remove(entry.id);
                } else {
                    this.variantUnsupportedIds.remove(entry.id);
                    this.variantUnsupportedRetryAtMs.remove(entry.id);
                    this.expandedIds.add(entry.id);
                    KNOWN_VARIANT_SUPPORTED_IDS.add(entry.id);
                }
            }
        }

        rebuildEntries();
        if (!setSelectedSelection(keepId, keepVariant, false)) {
            setSelectedSelection(entry.id, "", false);
        }
        this.setScrollAmount(Math.min(keepScrollAmount, this.getMaxScroll()));
    }

    private VariantOptionsResolveResult resolveVariantOptions(EntityType<?> type, Level level) {
        if (type == null || level == null) return new VariantOptionsResolveResult(List.of(), false);

        if (!WildexClientConfigView.backgroundMobVariantProbe()) {
            List<WildexEntityVariantProbe.VariantOption> options = WildexEntityVariantCatalog.options(type, level);
            return new VariantOptionsResolveResult(options, false);
        }

        ProbeState probeState = WildexEntityVariantCatalog.requestOptions(type);
        if (probeState == ProbeState.PENDING) {
            return new VariantOptionsResolveResult(List.of(), true);
        }
        if (probeState == ProbeState.UNSUPPORTED) {
            return new VariantOptionsResolveResult(List.of(), false);
        }
        return new VariantOptionsResolveResult(WildexEntityVariantCatalog.cachedOptions(type), false);
    }

    private static VariantBuckets splitVariantBuckets(List<WildexEntityVariantProbe.VariantOption> options) {
        if (options == null || options.isEmpty()) return new VariantBuckets(List.of(), List.of());

        List<WildexEntityVariantProbe.VariantOption> primary = new ArrayList<>();
        List<WildexEntityVariantProbe.VariantOption> derived = new ArrayList<>();
        for (WildexEntityVariantProbe.VariantOption option : options) {
            if (isDerivedVariantOption(option)) {
                derived.add(option);
            } else {
                primary.add(option);
            }
        }

        if (primary.isEmpty() || derived.isEmpty()) {
            return new VariantBuckets(List.copyOf(options), List.of());
        }
        return new VariantBuckets(List.copyOf(primary), List.copyOf(derived));
    }

    private static boolean isDerivedVariantOption(WildexEntityVariantProbe.VariantOption option) {
        if (option == null) return false;
        String token = optionToken(option);
        if (looksDerivedToken(token)) return true;
        String label = option.label();
        if (label == null) return false;
        return label.toLowerCase(Locale.ROOT).contains("hybrid");
    }

    private static String optionToken(WildexEntityVariantProbe.VariantOption option) {
        if (option == null) return "";
        String id = option.id();
        if (id == null || id.isBlank()) return "";
        int sep = id.indexOf('|');
        if (sep < 0 || sep + 1 >= id.length()) return id;
        return id.substring(sep + 1);
    }

    private static boolean looksDerivedToken(String token) {
        if (token == null || token.isBlank()) return false;
        String lower = token.toLowerCase(Locale.ROOT);
        return lower.startsWith("hybrid_")
                || lower.contains("_hybrid_")
                || lower.endsWith("_hybrid")
                || lower.contains("+");
    }

    private static String derivedGroupLabel(List<WildexEntityVariantProbe.VariantOption> options) {
        if (options == null || options.isEmpty()) return "Derived Variants";
        boolean allHybrid = true;
        for (WildexEntityVariantProbe.VariantOption option : options) {
            String token = optionToken(option).toLowerCase(Locale.ROOT);
            String label = option.label();
            String lowerLabel = label == null ? "" : label.toLowerCase(Locale.ROOT);
            if (!(token.contains("hybrid") || lowerLabel.contains("hybrid"))) {
                allHybrid = false;
                break;
            }
        }
        String base = allHybrid ? "Hybrid Variants" : "Derived Variants";
        return base + " (" + options.size() + ")";
    }

    private boolean isVariantGroupExpanded(ResourceLocation id, String groupKey) {
        return this.expandedVariantGroupKeys.contains(variantGroupExpansionKey(id, groupKey));
    }

    private static String variantGroupExpansionKey(ResourceLocation id, String groupKey) {
        if (id == null) return "";
        String suffix = (groupKey == null || groupKey.isBlank()) ? "group" : groupKey;
        return id + "|" + suffix;
    }

    private void clearVariantGroupExpansionsFor(ResourceLocation id) {
        if (id == null || this.expandedVariantGroupKeys.isEmpty()) return;
        String prefix = id + "|";
        this.expandedVariantGroupKeys.removeIf(key -> key != null && key.startsWith(prefix));
    }

    private record VariantBuckets(
            List<WildexEntityVariantProbe.VariantOption> primary,
            List<WildexEntityVariantProbe.VariantOption> derived
    ) {
    }

    private record VariantOptionsResolveResult(
            List<WildexEntityVariantProbe.VariantOption> options,
            boolean pending
    ) {
    }

    public final class Entry extends ObjectSelectionList.Entry<Entry> {

        private final EntityType<?> type;
        private final ResourceLocation id;
        private final Component name;
        private final String variantOptionId;
        private final boolean variantSubentry;
        private final boolean expandable;
        private final boolean variantGroupHeader;
        private final String variantGroupKey;
        private final int depth;

        private int lastY = 0;
        private int lastRowHeight = ITEM_H;

        private Entry(
                EntityType<?> type,
                ResourceLocation id,
                Component name,
                String variantOptionId,
                boolean variantSubentry,
                boolean expandable,
                boolean variantGroupHeader,
                String variantGroupKey,
                int depth
        ) {
            this.type = type;
            this.id = id;
            this.name = name == null ? Component.empty() : name;
            this.variantOptionId = variantOptionId == null ? "" : variantOptionId;
            this.variantSubentry = variantSubentry;
            this.expandable = expandable;
            this.variantGroupHeader = variantGroupHeader;
            this.variantGroupKey = variantGroupKey == null ? "" : variantGroupKey;
            this.depth = Math.max(0, depth);
        }

        @Override
        public @NotNull Component getNarration() {
            return this.name;
        }

        private boolean isDiscovered(boolean hiddenMode) {
            return !hiddenMode || WildexDiscoveryCache.isDiscovered(this.id);
        }

        private boolean showDebugDiscover(boolean discovered) {
            return !this.variantSubentry && debugDiscoverEnabled() && !discovered;
        }

        private boolean isExpanded() {
            if (this.variantGroupHeader) {
                return MobListWidget.this.isVariantGroupExpanded(this.id, this.variantGroupKey);
            }
            return !this.variantSubentry && MobListWidget.this.expandedIds.contains(this.id);
        }

        private int contentStartX(int x0, int rowHeight) {
            int x = x0 + TEXT_PAD_X;
            if (this.depth > 0) {
                x += subentryIndent(rowHeight) * this.depth;
            }
            return x;
        }

        private int expandButtonX(int x0, int rowHeight) {
            return contentStartX(x0, rowHeight);
        }

        private int expandButtonSize(int rowHeight) {
            int byRow = Math.round(rowHeight * EXPAND_BTN_ROW_FACTOR);
            int byUiScale = Math.round(13.0f * WildexUiScale.clamp(WildexUiScale.get()));
            return Mth.clamp(Math.max(byRow, byUiScale), EXPAND_BTN_MIN_SIZE, EXPAND_BTN_MAX_SIZE);
        }

        private int expandButtonGap(int rowHeight) {
            int size = expandButtonSize(rowHeight);
            return Math.max(EXPAND_BTN_MIN_GAP, Math.round(size * EXPAND_BTN_GAP_FACTOR));
        }

        private int subentryIndent(int rowHeight) {
            int size = expandButtonSize(rowHeight);
            return Math.max(SUBENTRY_INDENT, size - 2);
        }

        private int expandButtonY(int y, int rowHeight, int buttonSize) {
            float textScale = resolveListTextScale();
            int scaledLineH = Math.max(1, Math.round(WildexUiText.lineHeight(MobListWidget.this.minecraft.font) * textScale));
            int textY = y + ((rowHeight - scaledLineH) / 2) + TEXT_NUDGE_Y;
            int aligned = textY + ((scaledLineH - buttonSize) / 2);
            int maxTop = y + Math.max(0, rowHeight - buttonSize);
            return Mth.clamp(aligned, y, maxTop);
        }

        private boolean isInsideExpandButton(double mouseX, double mouseY, int x0, int y, int rowHeight) {
            if (!this.expandable || (this.variantSubentry && !this.variantGroupHeader)) return false;
            int size = expandButtonSize(rowHeight);
            int bx = expandButtonX(x0, rowHeight);
            int by = expandButtonY(y, rowHeight, size);
            return mouseX >= bx && mouseX < bx + size && mouseY >= by && mouseY < by + size;
        }

        @Override
        public void render(
                @NotNull GuiGraphics graphics,
                int index,
                int y,
                int x,
                int rowWidth,
                int rowHeight,
                int mouseX,
                int mouseY,
                boolean hovered,
                float partialTick
        ) {
            this.lastY = y;
            this.lastRowHeight = rowHeight;

            boolean selected = Objects.equals(MobListWidget.this.getSelected(), this);

            int x0 = MobListWidget.this.getX();
            int x1 = MobListWidget.this.getX() + MobListWidget.this.width - SCROLLBAR_W;

            int y1 = y + rowHeight;
            int topClipPad = WildexThemes.isModernLayout() ? 4 : 2;
            int listTopClip = MobListWidget.this.getY() + topClipPad;
            int listBottomClip = MobListWidget.this.getY() + MobListWidget.this.getHeight();
            int rowY0 = Math.max(y, listTopClip);
            int rowY1 = Math.min(y1, listBottomClip);
            if (rowY1 <= rowY0) return;

            if (selected) {
                WildexUiTheme.Palette theme = WildexUiTheme.current();
                graphics.fill(x0, rowY0, x1, rowY1, theme.selectionBg());
                graphics.fill(x0, rowY0, x1, rowY0 + 1, theme.selectionBorder());
                graphics.fill(x0, rowY1 - 1, x1, rowY1, theme.selectionBorder());
                graphics.fill(x0, rowY0, x0 + 1, rowY1, theme.selectionBorder());
                graphics.fill(x1 - 1, rowY0, x1, rowY1, theme.selectionBorder());
            }

            boolean hiddenMode = WildexClientConfigView.hiddenMode();
            boolean discovered = isDiscovered(hiddenMode);
            boolean canExpand = canExpand(discovered);

            boolean showDebug = showDebugDiscover(discovered);
            boolean showNew = !this.variantSubentry && hiddenMode && discovered && !WildexViewedMobEntriesCache.isViewed(this.id);

            int textRightCut = 2;
            if (showDebug) {
                textRightCut = DEBUG_CB_SIZE + DEBUG_CB_PAD * 2;
            } else if (showNew) {
                textRightCut = newBadgeWidth(MobListWidget.this.minecraft.font);
            }

            WildexUiTheme.Palette theme = WildexUiTheme.current();
            int color = selected ? theme.inkOnDark() : theme.ink();

            int textX = contentStartX(x0, rowHeight);
            if (canExpand) {
                textX += expandButtonSize(rowHeight) + expandButtonGap(rowHeight);
            }

            float textScale = resolveListTextScale();
            int scaledLineH = Math.max(1, Math.round(WildexUiText.lineHeight(MobListWidget.this.minecraft.font) * textScale));
            int textY = y + ((rowHeight - scaledLineH) / 2) + TEXT_NUDGE_Y;

            int clipX1 = x1 - textRightCut;

            int availW = Math.max(1, clipX1 - textX);
            int availLogicalW = Math.max(1, Math.round(availW / Math.max(0.001f, textScale)));

            String s = discovered ? this.name.getString() : HIDDEN_LABEL;
            int textW = WildexUiText.width(MobListWidget.this.minecraft.font, s);

            WildexScissor.enablePhysical(graphics, textX, rowY0, clipX1, rowY1);
            try {
                boolean shouldMarquee = textW > availLogicalW && (selected || showNew);
                if (!shouldMarquee) {
                    if (textScale >= 0.999f) {
                        WildexUiText.draw(graphics, MobListWidget.this.minecraft.font, s, textX, textY, color, false);
                    } else {
                        float inv = 1.0f / textScale;
                        graphics.pose().pushPose();
                        graphics.pose().scale(textScale, textScale, 1.0f);
                        WildexUiText.draw(
                                graphics,
                                MobListWidget.this.minecraft.font,
                                s,
                                Math.round(textX * inv),
                                Math.round(textY * inv),
                                color,
                                false
                        );
                        graphics.pose().popPose();
                    }
                } else {
                    int travel = (textW - availLogicalW) + MARQUEE_GAP_PX;
                    int phase = (this.id.toString() + "|" + this.variantOptionId).hashCode();
                    float off = marqueeOffset(System.currentTimeMillis(), travel, phase);

                    int baseX = textX - Math.round(off);
                    if (textScale >= 0.999f) {
                        WildexUiText.draw(graphics, MobListWidget.this.minecraft.font, s, baseX, textY, color, false);
                    } else {
                        float inv = 1.0f / textScale;
                        graphics.pose().pushPose();
                        graphics.pose().scale(textScale, textScale, 1.0f);
                        WildexUiText.draw(
                                graphics,
                                MobListWidget.this.minecraft.font,
                                s,
                                Math.round(baseX * inv),
                                Math.round(textY * inv),
                                color,
                                false
                        );
                        graphics.pose().popPose();
                    }
                }
            } finally {
                graphics.disableScissor();
            }

            if (canExpand) {
                int buttonSize = expandButtonSize(rowHeight);
                int bx = expandButtonX(x0, rowHeight);
                int by = expandButtonY(y, rowHeight, buttonSize);
                boolean expanded = isExpanded();
                WildexUiRenderUtil.drawMenuStyleToggleButton(
                        graphics,
                        bx,
                        by,
                        buttonSize,
                        !expanded,
                        EXPAND_BTN_BG,
                        EXPAND_BTN_SYMBOL
                );
            }

            if (showDebug) {
                int cbX = x1 - DEBUG_CB_PAD - DEBUG_CB_SIZE;
                int cbY = y + ((rowHeight - DEBUG_CB_SIZE) / 2);
                renderDebugCheckbox(graphics, cbX, cbY);
            } else if (showNew) {
                renderNewBadge(graphics, MobListWidget.this.minecraft.font, x1, y, rowHeight, selected);
            }
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            int barLeft = MobListWidget.this.getScrollbarPosition();
            if (mouseX >= barLeft) return false;

            boolean hiddenMode = WildexClientConfigView.hiddenMode();
            boolean discovered = isDiscovered(hiddenMode);

            int x0 = MobListWidget.this.getX();
            boolean canExpand = canExpand(discovered);
            if (button == 0 && canExpand && isInsideExpandButton(mouseX, mouseY, x0, this.lastY, this.lastRowHeight)) {
                MobListWidget.this.toggleExpanded(this);
                return true;
            }

            boolean showDebug = showDebugDiscover(discovered);

            int x1 = MobListWidget.this.getX() + MobListWidget.this.width - SCROLLBAR_W;

            if (showDebug) {
                int cbX = x1 - DEBUG_CB_PAD - DEBUG_CB_SIZE;
                int cbY = this.lastY + ((this.lastRowHeight - DEBUG_CB_SIZE) / 2);

                boolean inCb =
                        mouseX >= cbX && mouseX < (cbX + DEBUG_CB_SIZE) &&
                                mouseY >= cbY && mouseY < (cbY + DEBUG_CB_SIZE);

                if (inCb) {
                    MobListWidget.this.setSelected(this);
                    MobListWidget.this.onDebugDiscover.accept(this.id);
                    return true;
                }
            }

            if (this.variantGroupHeader) {
                return true;
            }

            MobListWidget.this.setSelected(this);
            if (discovered && !this.variantSubentry) {
                MobListWidget.this.onEntryClicked.accept(this.id);
            }
            return true;
        }

        private boolean canExpand(boolean discovered) {
            if (!this.expandable || !discovered) return false;
            if (this.variantSubentry && !this.variantGroupHeader) return false;
            if (this.variantGroupHeader) return true;
            if (!WildexClientConfigView.backgroundMobVariantProbe()) return true;
            if (this.id != null && KNOWN_VARIANT_SUPPORTED_IDS.contains(this.id)) return true;
            if (this.type == null) return false;

            SupportState state = WildexEntityVariantCatalog.requestSupport(this.type);
            if (state == SupportState.SUPPORTED) {
                if (this.id != null) {
                    KNOWN_VARIANT_SUPPORTED_IDS.add(this.id);
                }
                return true;
            }
            if (state == SupportState.UNSUPPORTED && this.id != null) {
                KNOWN_VARIANT_SUPPORTED_IDS.remove(this.id);
            }
            return false;
        }
    }

    public record Selection(ResourceLocation mobId, String variantOptionId) {
    }
}
