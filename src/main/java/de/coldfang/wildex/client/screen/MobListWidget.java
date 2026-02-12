package de.coldfang.wildex.client.screen;

import de.coldfang.wildex.client.data.WildexDiscoveryCache;
import de.coldfang.wildex.config.CommonConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public final class MobListWidget extends ObjectSelectionList<MobListWidget.Entry> {

    private static final int ITEM_H = 18;

    private static final int INK_COLOR = 0x2B1A10;
    private static final int INK_COLOR_SELECTED = 0xF2E8D8;

    private static final int SEL_BG = 0xCC000000;
    private static final int SEL_BORDER = 0xFFFFFFFF;

    private static final int SCROLLBAR_W = 6;
    private static final int TEXT_PAD_X = 10;
    private static final int TEXT_NUDGE_Y = 1;

    private static final int MARQUEE_GAP_PX = 18;
    private static final float MARQUEE_SPEED_PX_PER_SEC = 22.0f;
    private static final int MARQUEE_PAUSE_MS = 650;

    private static final String HIDDEN_LABEL = "???";

    private static final int DEBUG_CB_SIZE = 9;
    private static final int DEBUG_CB_PAD = 6;
    private static final int DEBUG_CB_FRAME = 0xCC2B1A10;
    private static final int DEBUG_CB_BG = 0x22FFFFFF;
    private static final int DEBUG_CB_CHECK = 0xFF2B1A10;

    private final Consumer<ResourceLocation> onSelect;
    private final Consumer<ResourceLocation> onDebugDiscover;

    private ResourceLocation selectedId;

    public MobListWidget(
            Minecraft mc,
            int x,
            int y,
            int w,
            int h,
            Consumer<ResourceLocation> onSelect,
            Consumer<ResourceLocation> onDebugDiscover
    ) {
        super(mc, w, h, y, ITEM_H);
        this.onSelect = onSelect == null ? id -> { } : onSelect;
        this.onDebugDiscover = onDebugDiscover == null ? id -> { } : onDebugDiscover;
        this.setX(x);
        this.setRenderHeader(false, 0);
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
    protected void renderListBackground(@NotNull GuiGraphics graphics) {
    }

    @Override
    protected void renderListSeparators(@NotNull GuiGraphics graphics) {
    }

    @Override
    protected void renderSelection(@NotNull GuiGraphics graphics, int y, int rowWidth, int itemHeight, int borderColor, int innerColor) {
    }

    private void syncSelectedIdFromSelected() {
        Entry sel = this.getSelected();
        this.selectedId = sel == null ? null : sel.id;
    }

    private static boolean debugDiscoverEnabled() {
        return CommonConfig.INSTANCE.hiddenMode.get() && CommonConfig.INSTANCE.debugMode.get();
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
        graphics.fill(cbX, cbY, cbX + DEBUG_CB_SIZE, cbY + DEBUG_CB_SIZE, DEBUG_CB_BG);

        graphics.fill(cbX, cbY, cbX + DEBUG_CB_SIZE, cbY + 1, DEBUG_CB_FRAME);
        graphics.fill(cbX, cbY + DEBUG_CB_SIZE - 1, cbX + DEBUG_CB_SIZE, cbY + DEBUG_CB_SIZE, DEBUG_CB_FRAME);
        graphics.fill(cbX, cbY, cbX + 1, cbY + DEBUG_CB_SIZE, DEBUG_CB_FRAME);
        graphics.fill(cbX + DEBUG_CB_SIZE - 1, cbY, cbX + DEBUG_CB_SIZE, cbY + DEBUG_CB_SIZE, DEBUG_CB_FRAME);

        graphics.fill(cbX + 2, cbY + 4, cbX + DEBUG_CB_SIZE - 2, cbY + 5, DEBUG_CB_CHECK);
        graphics.fill(cbX + 4, cbY + 2, cbX + 5, cbY + DEBUG_CB_SIZE - 2, DEBUG_CB_CHECK);
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

            if (selected) {
                graphics.fill(x0, y, x1, y1, SEL_BG);

                graphics.fill(x0, y, x1, y + 1, SEL_BORDER);
                graphics.fill(x0, y1 - 1, x1, y1, SEL_BORDER);
                graphics.fill(x0, y, x0 + 1, y1, SEL_BORDER);
                graphics.fill(x1 - 1, y, x1, y1, SEL_BORDER);
            }

            boolean hiddenMode = CommonConfig.INSTANCE.hiddenMode.get();
            boolean discovered = isDiscovered(hiddenMode);

            boolean showDebug = showDebugDiscover(discovered);

            int textRightCut = showDebug ? (DEBUG_CB_SIZE + DEBUG_CB_PAD * 2) : 2;

            int color = selected ? INK_COLOR_SELECTED : INK_COLOR;

            int textX = x0 + TEXT_PAD_X;
            int textY = y + ((rowHeight - MobListWidget.this.minecraft.font.lineHeight) / 2) + TEXT_NUDGE_Y;

            int clipX1 = x1 - textRightCut;

            int availW = Math.max(1, clipX1 - textX);

            String s = discovered ? this.name.getString() : HIDDEN_LABEL;
            int textW = MobListWidget.this.minecraft.font.width(s);

            graphics.enableScissor(textX, y, clipX1, y1);
            try {
                if (!selected || textW <= availW) {
                    graphics.drawString(MobListWidget.this.minecraft.font, s, textX, textY, color, false);
                } else {
                    int travel = (textW - availW) + MARQUEE_GAP_PX;
                    int phase = this.id.toString().hashCode();
                    float off = marqueeOffset(System.currentTimeMillis(), travel, phase);

                    int baseX = textX - Math.round(off);
                    graphics.drawString(MobListWidget.this.minecraft.font, s, baseX, textY, color, false);
                }
            } finally {
                graphics.disableScissor();
            }

            if (showDebug) {
                int cbX = x1 - DEBUG_CB_PAD - DEBUG_CB_SIZE;
                int cbY = y + ((rowHeight - DEBUG_CB_SIZE) / 2);
                renderDebugCheckbox(graphics, cbX, cbY);
            }
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            int barLeft = MobListWidget.this.getScrollbarPosition();
            if (mouseX >= barLeft) return false;

            boolean hiddenMode = CommonConfig.INSTANCE.hiddenMode.get();
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
            return true;
        }
    }
}
