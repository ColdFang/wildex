package de.coldfang.wildex.client.screen;

import de.coldfang.wildex.client.data.WildexDiscoveryCache;
import de.coldfang.wildex.client.data.WildexViewedMobEntriesCache;
import de.coldfang.wildex.client.WildexClientConfigView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
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

    private final Consumer<ResourceLocation> onSelect;
    private final Consumer<ResourceLocation> onEntryClicked;
    private final Consumer<ResourceLocation> onDebugDiscover;
    private ResourceLocation selectedId;
    private boolean draggingScrollbar = false;
    private int scrollbarDragOffsetY = 0;

    public MobListWidget(
            Minecraft mc,
            int x,
            int y,
            int w,
            int h,
            int itemHeightPx,
            Consumer<ResourceLocation> onSelect,
            Consumer<ResourceLocation> onEntryClicked,
            Consumer<ResourceLocation> onDebugDiscover
    ) {
        super(mc, w, h, y, Math.max(14, itemHeightPx));
        this.onSelect = onSelect == null ? id -> {
        } : onSelect;
        this.onEntryClicked = onEntryClicked == null ? id -> {
        } : onEntryClicked;
        this.onDebugDiscover = onDebugDiscover == null ? id -> { } : onDebugDiscover;
        this.setX(x);
        this.setRenderHeader(false, 0);
    }

    public static int itemHeightForUiScale(float uiScale) {
        return Math.max(14, Math.round(ITEM_H * WildexUiScale.clamp(uiScale)));
    }

    public void setEntries(List<EntityType<?>> types) {
        this.clearEntries();
        if (types == null) return;

        for (EntityType<?> type : types) {
            ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
            this.addEntry(new Entry(type, id));
        }

        if (this.getSelected() == null && !this.children().isEmpty()) {
            Entry first = this.children().getFirst();
            this.setSelected(first);
            this.centerScrollOn(first);
        } else {
            syncSelectedIdFromSelected();
        }
    }

    public void setSelectedId(ResourceLocation id) {
        this.selectedId = id;

        if (id == null) {
            this.setSelected(null);
            return;
        }

        for (Entry e : this.children()) {
            if (id.equals(e.id)) {
                this.setSelected(e);
                this.centerScrollOn(e);
                return;
            }
        }

        this.setSelected(null);
    }

    public ResourceLocation selectedId() {
        return this.selectedId;
    }

    @Override
    public void setSelected(Entry entry) {
        super.setSelected(entry);
        syncSelectedIdFromSelected();
        if (this.selectedId != null) {
            this.onSelect.accept(this.selectedId);
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
        // We render and handle a custom, wider scrollbar ourselves.
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
        // Disable vanilla drag logic (hardcoded 6px scrollbar), custom logic handles this widget.
    }

    @Override
    public void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
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

    private void syncSelectedIdFromSelected() {
        Entry sel = this.getSelected();
        this.selectedId = sel == null ? null : sel.id;
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

    public final class Entry extends ObjectSelectionList.Entry<Entry> {

        private final ResourceLocation id;
        private final Component name;

        private int lastY = 0;
        private int lastRowHeight = ITEM_H;

        private Entry(EntityType<?> type, ResourceLocation id) {
            this.id = id;
            this.name = type.getDescription();
        }

        @Override
        public @NotNull Component getNarration() {
            return this.name;
        }

        private boolean isDiscovered(boolean hiddenMode) {
            return !hiddenMode || WildexDiscoveryCache.isDiscovered(this.id);
        }

        private boolean showDebugDiscover(boolean discovered) {
            return debugDiscoverEnabled() && !discovered;
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
            int listTopClip = MobListWidget.this.getY() + topClipPad; // keep clear under top divider line
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

            boolean showDebug = showDebugDiscover(discovered);
            boolean showNew = hiddenMode && discovered && !WildexViewedMobEntriesCache.isViewed(this.id);

            int textRightCut = 2;
            if (showDebug) {
                textRightCut = DEBUG_CB_SIZE + DEBUG_CB_PAD * 2;
            } else if (showNew) {
                textRightCut = newBadgeWidth(MobListWidget.this.minecraft.font);
            }

            WildexUiTheme.Palette theme = WildexUiTheme.current();
            int color = selected ? theme.inkOnDark() : theme.ink();

            int textX = x0 + TEXT_PAD_X;
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
                        WildexUiText.draw(graphics, 
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
                    int phase = this.id.toString().hashCode();
                    float off = marqueeOffset(System.currentTimeMillis(), travel, phase);

                    int baseX = textX - Math.round(off);
                    if (textScale >= 0.999f) {
                        WildexUiText.draw(graphics, MobListWidget.this.minecraft.font, s, baseX, textY, color, false);
                    } else {
                        float inv = 1.0f / textScale;
                        graphics.pose().pushPose();
                        graphics.pose().scale(textScale, textScale, 1.0f);
                        WildexUiText.draw(graphics, 
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

            MobListWidget.this.setSelected(this);
            if (discovered) {
                MobListWidget.this.onEntryClicked.accept(this.id);
            }
            return true;
        }
    }
}




