package de.coldfang.wildex.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class RightTabsWidget extends AbstractWidget {

    private static final int TAB_GAP = 0;

    private static final int TAB_PAD_X = 8;
    private static final int TEXT_PAD_Y_TOP = 2;
    private static final int TEXT_PAD_Y_BOTTOM = 1;

    private static final float MIN_TEXT_SCALE = 0.55f;

    private final WildexScreenState state;

    public RightTabsWidget(int x, int y, int w, int h, WildexScreenState state) {
        super(x, y, w, h, Component.empty());
        this.state = Objects.requireNonNull(state, "state");
    }

    @Override
    protected void updateWidgetNarration(@NotNull net.minecraft.client.gui.narration.NarrationElementOutput output) {
    }

    @Override
    public void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        WildexUiTheme.Palette theme = WildexUiTheme.current();
        boolean modern = WildexThemes.isModernLayout();
        int dividerColor = WildexThemes.isVintageLayout()
                ? theme.rowSeparator()
                : theme.frameInner();
        WildexTab[] tabs = WildexTab.values();
        if (tabs.length == 0) return;

        int tabWBase = Math.max(1, this.width / tabs.length);
        int tabH = this.height;

        int baseX = this.getX();
        int baseY = this.getY();

        var font = Minecraft.getInstance().font;

        for (int i = 0; i < tabs.length; i++) {
            WildexTab tab = tabs[i];

            int tx = baseX + i * (tabWBase + TAB_GAP);
            int tw = (i == tabs.length - 1) ? (this.getX() + this.width - tx) : tabWBase;

            boolean active = (tab == state.selectedTab());
            boolean hovered = mouseX >= tx && mouseX < tx + tw && mouseY >= baseY && mouseY < baseY + tabH;

            int bg = active
                    ? (modern ? 0xFF35F0FF : theme.selectionBg())
                    : theme.frameBg();
            graphics.fill(tx, baseY, tx + tw, baseY + tabH, bg);

            boolean drawLeftBorder = (i == 0);
            boolean drawRightBorder = (i == tabs.length - 1);

            graphics.fill(tx, baseY, tx + tw, baseY + 1, theme.frameOuter());

            if (drawLeftBorder) {
                graphics.fill(tx, baseY, tx + 1, baseY + tabH, theme.frameOuter());
            }
            if (drawRightBorder) {
                graphics.fill(tx + tw - 1, baseY, tx + tw, baseY + tabH, theme.frameOuter());
            }

            if (i > 0) {
                graphics.fill(tx, baseY + 1, tx + 1, baseY + tabH, dividerColor);
            }

            if (active || hovered) {
                graphics.fill(tx, baseY, tx + tw, baseY + 1, theme.frameOuter());

                if (!drawLeftBorder) {
                    graphics.fill(tx, baseY, tx + 1, baseY + tabH, theme.frameOuter());
                }
                if (!drawRightBorder) {
                    graphics.fill(tx + tw - 1, baseY, tx + tw, baseY + tabH, theme.frameOuter());
                }

                if (!active) {
                    graphics.fill(tx, baseY + tabH - 1, tx + tw, baseY + tabH, theme.frameOuter());
                }
            }

            if (modern && active) {
                int y0 = baseY + tabH - 2;
                int y1 = baseY + tabH;
                graphics.fill(tx + 1, y0, tx + tw - 1, y1, 0xFF0A1418);
            }

            Component label = tab.label();
            int color = active
                    ? (modern ? 0xFF000000 : theme.inkOnDark())
                    : theme.ink();

            int innerX0 = tx + 1;
            int innerY0 = baseY + 1;
            int innerX1 = tx + tw - 1;
            int innerY1 = baseY + tabH - 1;

            graphics.enableScissor(innerX0, innerY0, innerX1, innerY1);

            int padX = (tw <= 44) ? 4 : TAB_PAD_X;
            int padTop = (tabH <= 16) ? 1 : TEXT_PAD_Y_TOP;
            int padBottom = (tabH <= 16) ? 0 : TEXT_PAD_Y_BOTTOM;

            int innerH = tabH - 2;
            int availableH = Math.max(0, innerH - padTop - padBottom);
            int textYBase = baseY + 1 + padTop;

            String s = label.getString();
            int textW = font.width(s);
            int textH = font.lineHeight;

            int maxTextW = Math.max(1, tw - (padX * 2));

            float scaleW = (textW <= 0) ? 1.0f : ((float) maxTextW / (float) textW);
            float scaleH = (float) availableH / (float) textH;

            float ts = Math.min(1.0f, Math.min(scaleW, scaleH));
            if (ts < MIN_TEXT_SCALE) ts = MIN_TEXT_SCALE;

            float scaledW = textW * ts;
            float scaledH = textH * ts;

            float drawX = tx + (tw - scaledW) / 2.0f;
            float drawY = textYBase + Math.max(0.0f, (availableH - scaledH) / 2.0f);

            graphics.pose().pushPose();
            graphics.pose().translate(drawX, drawY, 0);
            graphics.pose().scale(ts, ts, 1.0f);
            graphics.drawString(font, s, 0, 0, color, false);
            graphics.pose().popPose();

            graphics.disableScissor();
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.active || !this.visible) return false;
        if (!this.isMouseOver(mouseX, mouseY)) return false;

        WildexTab[] tabs = WildexTab.values();
        if (tabs.length == 0) return false;

        int tabW = Math.max(1, this.width / tabs.length);

        int relX = (int) Math.floor(mouseX) - this.getX();
        int stride = tabW + TAB_GAP;
        int idx = relX / stride;

        if (idx < 0 || idx >= tabs.length) return false;

        int start = idx * stride;
        if (relX >= start + tabW) return false;

        state.setSelectedTab(tabs[idx]);
        return true;
    }
}
