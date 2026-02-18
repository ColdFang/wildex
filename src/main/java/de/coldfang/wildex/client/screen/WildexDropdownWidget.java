package de.coldfang.wildex.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public final class WildexDropdownWidget extends AbstractWidget {
    private static final int SCROLL_W = 6;
    private static final int SCROLL_MIN_THUMB_H = 10;

    private static final int TEXT_PAD_X = 5;

    private final List<String> options = new ArrayList<>();
    private int selected = -1;
    private boolean open = false;
    private boolean openUpwards = false;
    private int scrollOffset = 0;
    private int maxVisibleRows = 5;
    private String emptyText = "-";

    private boolean draggingScrollbar = false;
    private int scrollbarDragGrabY = 0;
    private Consumer<Boolean> onOpenChanged = null;

    public WildexDropdownWidget(int x, int y, int w, int h) {
        super(x, y, w, h, Component.empty());
    }

    public void setOptions(List<String> values, String preferredSelection) {
        options.clear();
        if (values != null) {
            for (String s : values) {
                if (s == null || s.isBlank()) continue;
                options.add(s);
            }
        }

        if (options.isEmpty()) {
            selected = -1;
            setMessage(Component.empty());
            open = false;
            scrollOffset = 0;
            return;
        }

        int idx = -1;
        if (preferredSelection != null && !preferredSelection.isBlank()) {
            for (int i = 0; i < options.size(); i++) {
                if (Objects.equals(options.get(i), preferredSelection)) {
                    idx = i;
                    break;
                }
            }
        }

        selected = idx;
        setMessage(Component.literal(selected >= 0 ? options.get(selected) : emptyText));
        ensureSelectedVisible();
    }

    public String selectedValue() {
        if (selected < 0 || selected >= options.size()) return "";
        return options.get(selected);
    }

    public boolean isOpen() {
        return open;
    }

    public void setOpenUpwards(boolean openUpwards) {
        this.openUpwards = openUpwards;
    }

    public void setMaxVisibleRows(int maxVisibleRows) {
        this.maxVisibleRows = Math.max(3, maxVisibleRows);
        clampScrollOffset();
    }

    public void setEmptyText(String emptyText) {
        this.emptyText = (emptyText == null || emptyText.isBlank()) ? "-" : emptyText;
        if (selected < 0) {
            setMessage(Component.literal(this.emptyText));
        }
    }

    public void closeList() {
        open = false;
        draggingScrollbar = false;
    }

    public void clearSelection() {
        selected = -1;
        setMessage(Component.literal(emptyText));
    }

    public void setOnOpenChanged(Consumer<Boolean> onOpenChanged) {
        this.onOpenChanged = onOpenChanged;
    }

    @Override
    protected void updateWidgetNarration(@NotNull net.minecraft.client.gui.narration.NarrationElementOutput output) {
    }

    @Override
    public void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        WildexUiTheme.Palette theme = WildexUiTheme.current();
        drawFrame(graphics, getX(), getY(), getWidth(), getHeight(), theme.frameBg(), theme);

        var font = Minecraft.getInstance().font;
        String text = selectedValue();
        if (text.isBlank()) text = emptyText;
        int tx = getX() + TEXT_PAD_X;
        int ty = getY() + (getHeight() - font.lineHeight) / 2;
        graphics.drawString(font, text, tx, ty, theme.ink(), false);
        graphics.drawString(font, open ? (openUpwards ? "^" : "v") : ">", getX() + getWidth() - 8, ty, theme.ink(), false);

        if (!open || options.isEmpty()) return;

        graphics.pose().pushPose();
        graphics.pose().translate(0.0f, 0.0f, 300.0f);
        int rowH = getHeight();
        int visible = visibleRows();
        int listY0 = listStartY();
        int listH = visible * rowH;
        int listX = getX();
        int listW = getWidth();

        drawFrame(graphics, listX, listY0, listW, listH, theme.listBg(), theme);

        boolean drawScrollbar = needsScrollbar();
        int textRight = listX + listW - 2 - (drawScrollbar ? SCROLL_W + 2 : 0);

        for (int i = 0; i < visible; i++) {
            int optionIdx = scrollOffset + i;
            if (optionIdx >= options.size()) break;

            int ry = listY0 + (i * rowH);
            boolean hovered = mouseX >= listX && mouseX < listX + listW && mouseY >= ry && mouseY < ry + rowH;
            if (hovered) {
                graphics.fill(listX + 1, ry + 1, listX + listW - 1, ry + rowH - 1, theme.rowHover());
            }

            if (i > 0) {
                graphics.fill(listX + 1, ry, listX + listW - 1, ry + 1, theme.rowSeparator());
            }

            int rty = ry + (rowH - font.lineHeight) / 2;
            String rowText = options.get(optionIdx);
            int maxTextW = Math.max(8, textRight - (listX + TEXT_PAD_X));
            rowText = font.plainSubstrByWidth(rowText, maxTextW);
            graphics.drawString(font, rowText, listX + TEXT_PAD_X, rty, theme.ink(), false);
        }

        if (drawScrollbar) {
            int trackX0 = listX + listW - SCROLL_W - 1;
            int trackX1 = listX + listW - 1;
            int trackY0 = listY0 + 1;
            int trackY1 = listY0 + listH - 1;
            graphics.fill(trackX0, trackY0, trackX1, trackY1, theme.scrollTrack());

            int thumbY0 = scrollbarThumbY(trackY0, trackY1);
            int thumbY1 = thumbY0 + scrollbarThumbHeight(trackY0, trackY1);
            graphics.fill(trackX0, thumbY0, trackX1, thumbY1, theme.scrollThumb());
        }
        graphics.pose().popPose();
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        int mx = (int) Math.floor(mouseX);
        int my = (int) Math.floor(mouseY);
        boolean inHead = mx >= getX() && mx < getX() + getWidth() && my >= getY() && my < getY() + getHeight();
        return inHead || isInOpenList(mx, my);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!active || !visible || button != 0) return false;

        int mx = (int) Math.floor(mouseX);
        int my = (int) Math.floor(mouseY);

        boolean inHead = mx >= getX() && mx < getX() + getWidth() && my >= getY() && my < getY() + getHeight();
        if (inHead) {
            open = !open;
            draggingScrollbar = false;
            if (onOpenChanged != null) onOpenChanged.accept(open);
            return true;
        }

        if (!open || options.isEmpty()) return false;

        if (isInScrollbar(mx, my)) {
            int trackY0 = listStartY() + 1;
            int trackY1 = listStartY() + (visibleRows() * getHeight()) - 1;
            int thumbY0 = scrollbarThumbY(trackY0, trackY1);
            int thumbH = scrollbarThumbHeight(trackY0, trackY1);
            if (my >= thumbY0 && my < thumbY0 + thumbH) {
                draggingScrollbar = true;
                scrollbarDragGrabY = my - thumbY0;
            } else {
                jumpScrollToMouse(my, trackY0, trackY1, thumbH);
            }
            return true;
        }

        int rowH = getHeight();
        int listY0 = listStartY();
        int listY1 = listY0 + (visibleRows() * rowH);
        boolean inList = mx >= getX() && mx < getX() + getWidth() && my >= listY0 && my < listY1;
        if (!inList) {
            open = false;
            draggingScrollbar = false;
            return false;
        }

        int idx = scrollOffset + ((my - listY0) / rowH);
        if (idx < 0 || idx >= options.size()) {
            open = false;
            draggingScrollbar = false;
            return false;
        }

        selected = idx;
        setMessage(Component.literal(options.get(selected)));
        open = false;
        draggingScrollbar = false;
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (!draggingScrollbar || button != 0 || !open || !needsScrollbar()) return false;
        int my = (int) Math.floor(mouseY);
        int trackY0 = listStartY() + 1;
        int trackY1 = listStartY() + (visibleRows() * getHeight()) - 1;
        int thumbH = scrollbarThumbHeight(trackY0, trackY1);
        int trackSpan = Math.max(1, (trackY1 - trackY0) - thumbH);
        int maxOffset = maxScrollOffset();
        int desiredThumbY = my - scrollbarDragGrabY;
        int clampedThumbY = Math.max(trackY0, Math.min(trackY1 - thumbH, desiredThumbY));
        int rel = clampedThumbY - trackY0;
        scrollOffset = (rel * maxOffset) / trackSpan;
        clampScrollOffset();
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) draggingScrollbar = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (!open || !visible || !active || options.isEmpty()) return false;
        int mx = (int) Math.floor(mouseX);
        int my = (int) Math.floor(mouseY);
        if (!isInOpenList(mx, my)) return false;
        return scrollByWheel(deltaY);
    }

    public boolean scrollByWheel(double deltaY) {
        if (!open || !visible || !active || options.isEmpty()) return false;
        if (deltaY > 0) scrollOffset--;
        else if (deltaY < 0) scrollOffset++;
        else return false;
        clampScrollOffset();
        return true;
    }

    private int visibleRows() {
        return Math.min(options.size(), Math.max(3, maxVisibleRows));
    }

    private boolean needsScrollbar() {
        return options.size() > visibleRows();
    }

    private int maxScrollOffset() {
        return Math.max(0, options.size() - visibleRows());
    }

    private void clampScrollOffset() {
        int max = maxScrollOffset();
        if (scrollOffset < 0) scrollOffset = 0;
        if (scrollOffset > max) scrollOffset = max;
    }

    private void ensureSelectedVisible() {
        if (selected < 0) return;
        int visible = visibleRows();
        if (selected < scrollOffset) scrollOffset = selected;
        if (selected >= scrollOffset + visible) scrollOffset = selected - visible + 1;
        clampScrollOffset();
    }

    private int listStartY() {
        int rowH = getHeight();
        int visible = visibleRows();
        if (!openUpwards) return getY() + rowH;
        return getY() - (visible * rowH);
    }

    private boolean isInOpenList(int mx, int my) {
        if (!open || options.isEmpty()) return false;
        int y0 = listStartY();
        int y1 = y0 + (visibleRows() * getHeight());
        return mx >= getX() && mx < getX() + getWidth() && my >= y0 && my < y1;
    }

    private boolean isInScrollbar(int mx, int my) {
        if (!needsScrollbar() || !isInOpenList(mx, my)) return false;
        int listY0 = listStartY();
        int listH = visibleRows() * getHeight();
        int x0 = getX() + getWidth() - SCROLL_W - 1;
        int x1 = getX() + getWidth() - 1;
        return mx >= x0 && mx < x1 && my >= listY0 + 1 && my < listY0 + listH - 1;
    }

    private int scrollbarThumbHeight(int trackY0, int trackY1) {
        int trackH = Math.max(1, trackY1 - trackY0);
        int visible = visibleRows();
        int total = Math.max(1, options.size());
        int h = (trackH * visible) / total;
        return Math.max(SCROLL_MIN_THUMB_H, h);
    }

    private int scrollbarThumbY(int trackY0, int trackY1) {
        int maxOffset = maxScrollOffset();
        if (maxOffset <= 0) return trackY0;
        int thumbH = scrollbarThumbHeight(trackY0, trackY1);
        int trackSpan = Math.max(1, (trackY1 - trackY0) - thumbH);
        return trackY0 + (scrollOffset * trackSpan) / maxOffset;
    }

    private void jumpScrollToMouse(int my, int trackY0, int trackY1, int thumbH) {
        int maxOffset = maxScrollOffset();
        if (maxOffset <= 0) return;
        int trackSpan = Math.max(1, (trackY1 - trackY0) - thumbH);
        int desiredThumbY = my - (thumbH / 2);
        int clamped = Math.max(trackY0, Math.min(trackY1 - thumbH, desiredThumbY));
        int rel = clamped - trackY0;
        scrollOffset = (rel * maxOffset) / trackSpan;
        clampScrollOffset();
    }

    private static void drawFrame(GuiGraphics g, int x, int y, int w, int h, int bg, WildexUiTheme.Palette theme) {
        int x1 = x + w;
        int y1 = y + h;

        g.fill(x + 1, y + 1, x1 - 1, y1 - 1, bg);

        g.fill(x, y, x1, y + 1, theme.frameOuter());
        g.fill(x, y1 - 1, x1, y1, theme.frameOuter());
        g.fill(x, y, x + 1, y1, theme.frameOuter());
        g.fill(x1 - 1, y, x1, y1, theme.frameOuter());

        g.fill(x + 1, y + 1, x1 - 1, y + 2, theme.frameInner());
        g.fill(x + 1, y1 - 2, x1 - 1, y1 - 1, theme.frameInner());
        g.fill(x + 1, y + 1, x + 2, y1 - 1, theme.frameInner());
        g.fill(x1 - 2, y + 1, x1 - 1, y1 - 1, theme.frameInner());
    }
}
