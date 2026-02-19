package de.coldfang.wildex.client.screen;

import de.coldfang.wildex.client.WildexClientConfigView;
import de.coldfang.wildex.client.WildexNetworkClient;
import de.coldfang.wildex.client.data.WildexDiscoveryCache;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public final class WildexShareOverlayController {

    private static final Component SHARE_CLAIM_PAYOUTS_LABEL = Component.translatable("gui.wildex.share.claim_payouts");
    private static final Component SHARE_SINGLEPLAYER_NOTICE = Component.translatable("gui.wildex.share.singleplayer_only");
    private static final Component SHARE_SEND_LABEL = Component.translatable("gui.wildex.share.send_offer");
    private static final Component SHARE_SEND_HEADING_LABEL = Component.translatable("gui.wildex.share.send_offer_heading");
    private static final Component SHARE_ACCEPT_OFFERS_LABEL = Component.translatable("gui.wildex.share.accept_offers");
    private static final Component SHARE_SELECT_PLAYER_LABEL = Component.translatable("gui.wildex.share.choose_player");
    private static final Component SHARE_PRICE_LABEL = Component.translatable("gui.wildex.share.price");
    private static final Component SHARE_OPEN_TO_OFFERS_TOOLTIP = Component.translatable("tooltip.wildex.share_accept_offers");

    private static final Component SHARE_TOOLTIP = Component.translatable("tooltip.wildex.share_entry");
    private static final Component SHARE_CLAIM_PAYOUTS_TOOLTIP = Component.translatable("tooltip.wildex.share_claim_payouts");

    private final WildexScreen host;
    private final Font font;
    private final Component shareButtonLabel;
    private final int styleButtonW;
    private final int styleButtonH;
    private final int styleButtonMargin;
    private final int styleButtonYOffset;
    private final BooleanSupplier singleplayerShareBlockedSupplier;
    private final BooleanSupplier shareEligibleSupplier;
    private final Supplier<String> selectedMobIdSupplier;

    private WildexStyleButton shareEntryButton;
    private WildexStyleButton shareClaimPayoutsButton;
    private WildexDiscoveredOnlyCheckbox shareOpenToOffersCheckbox;
    private WildexDropdownWidget sharePlayersDropdown;
    private WildexSearchBox sharePriceInput;
    private WildexPanelButton shareSendOfferButton;
    private MobPreviewResetButton shareCloseOverlayButton;
    private boolean sharePanelOpen = false;
    private int shareSingleplayerNoticeTicks = 0;
    private List<String> lastSharePlayerOptions = List.of();
    private final Map<String, UUID> shareCandidateByName = new HashMap<>();
    private boolean suppressShareAcceptOffersUpdate = false;

    public WildexShareOverlayController(
            WildexScreen host,
            Font font,
            Component shareButtonLabel,
            int styleButtonW,
            int styleButtonH,
            int styleButtonMargin,
            int styleButtonYOffset,
            BooleanSupplier singleplayerShareBlockedSupplier,
            BooleanSupplier shareEligibleSupplier,
            Supplier<String> selectedMobIdSupplier
    ) {
        this.host = host;
        this.font = font;
        this.shareButtonLabel = shareButtonLabel;
        this.styleButtonW = styleButtonW;
        this.styleButtonH = styleButtonH;
        this.styleButtonMargin = styleButtonMargin;
        this.styleButtonYOffset = styleButtonYOffset;
        this.singleplayerShareBlockedSupplier = singleplayerShareBlockedSupplier;
        this.shareEligibleSupplier = shareEligibleSupplier;
        this.selectedMobIdSupplier = selectedMobIdSupplier;
    }

    public void initWidgets(WildexScreenLayout layout) {
        if (!WildexClientConfigView.hiddenMode() || !WildexClientConfigView.shareOffersEnabled()) {
            this.shareEntryButton = null;
            this.shareClaimPayoutsButton = null;
            this.shareOpenToOffersCheckbox = null;
            this.sharePlayersDropdown = null;
            this.sharePriceInput = null;
            this.shareSendOfferButton = null;
            this.shareCloseOverlayButton = null;
            this.sharePanelOpen = false;
            this.lastSharePlayerOptions = List.of();
            return;
        }

        WildexScreenLayout.Area leftAction = layout.leftActionArea();
        int leftRefSize = Math.max(8, leftAction.h());
        int shareCheckboxSize = Math.max(12, leftRefSize);
        int styleW = scaledStyleButtonW();
        int styleH = scaledStyleButtonH();
        int styleMargin = scaledStyleButtonMargin();
        int styleYOffset = scaledStyleButtonYOffset();
        int shareButtonH = styleH;
        int shareButtonW = styleW;
        int claimButtonW = shareButtonW;
        WildexScreenLayout.Area shareTopArea = layout.shareEntryButtonArea(
                styleW, styleH, styleMargin, styleYOffset, shareButtonW, shareButtonH
        );
        WildexScreenLayout.Area claimTopArea = layout.shareClaimButtonArea(
                styleW, styleH, styleMargin, styleYOffset, shareButtonW, shareButtonH, claimButtonW
        );

        this.shareEntryButton = new WildexStyleButton(
                shareTopArea.x(),
                shareTopArea.y(),
                shareButtonW,
                shareButtonH,
                shareButtonLabel,
                () -> {
                    if (singleplayerShareBlockedSupplier.getAsBoolean()) {
                        sharePanelOpen = false;
                        shareSingleplayerNoticeTicks = 80;
                        updateWidgetsVisibility();
                        if (this.shareEntryButton != null) this.shareEntryButton.setFocused(false);
                        this.host.setFocused(null);
                        return;
                    }
                    sharePanelOpen = !sharePanelOpen;
                    if (!sharePanelOpen) {
                        resetShareOfferSelection();
                    }
                    refreshSharePlayerOptions();
                    updateWidgetsVisibility();
                    if (this.shareEntryButton != null) this.shareEntryButton.setFocused(false);
                    this.host.setFocused(null);
                }
        );
        this.host.addShareWidget(this.shareEntryButton);

        this.shareClaimPayoutsButton = new WildexStyleButton(
                claimTopArea.x(),
                claimTopArea.y(),
                claimButtonW,
                shareButtonH,
                SHARE_CLAIM_PAYOUTS_LABEL,
                WildexNetworkClient::claimSharePayouts,
                () -> new ItemStack(resolveShareCurrencyItem())
        );
        this.host.addShareWidget(this.shareClaimPayoutsButton);

        WildexScreenLayout.Area panel = layout.sharePanelArea();
        float fitScale = Math.max(0.65f, Math.min(1.0f, panel.h() / 170.0f));
        float scaleNorm = Math.max(0.55f, resolveShareOverlayScale() * fitScale);
        int pad = Math.max(3, Math.round(8 * scaleNorm));
        int rowTextMin = WildexUiText.lineHeight(font) + Math.max(6, Math.round(6 * scaleNorm));
        int rowH = Math.max(rowTextMin, Math.round(20 * scaleNorm));
        int ddW = Math.max(80, panel.w() - (pad * 2) - 1);

        int offersCbX = panel.x() + pad;
        int offersCbY = panel.y() + pad;
        this.shareOpenToOffersCheckbox = new WildexDiscoveredOnlyCheckbox(
                offersCbX,
                offersCbY,
                shareCheckboxSize,
                false,
                checked -> {
                    if (suppressShareAcceptOffersUpdate) return;
                    WildexNetworkClient.setShareAcceptOffers(checked);
                },
                SHARE_OPEN_TO_OFFERS_TOOLTIP
        );
        this.host.addShareWidget(this.shareOpenToOffersCheckbox);

        int dividerGap = Math.max(2, Math.round(6 * scaleNorm));
        int headingGap = Math.max(4, Math.round(12 * scaleNorm));
        int dropdownTopGap = Math.max(2, Math.round(6 * scaleNorm));

        int sendH = Math.max(rowTextMin, Math.round(20 * scaleNorm));
        int sendBottomLift = Math.max(2, Math.round(4 * scaleNorm));
        int sendY = panel.y() + panel.h() - pad - sendH - sendBottomLift;
        int priceRowH = Math.max(WildexUiText.lineHeight(font) + Math.max(6, Math.round(6 * scaleNorm)), Math.round(18 * scaleNorm));
        int priceToSendGap = Math.max(2, Math.round(8 * scaleNorm));
        int priceY = sendY - priceToSendGap - priceRowH;
        int priceLabelGap = Math.max(1, Math.round(4 * scaleNorm));
        int priceLabelY = priceY - WildexUiText.lineHeight(font) - priceLabelGap;

        int headingY = offersCbY + shareCheckboxSize + dividerGap + headingGap;
        int minBetweenDropdownAndPriceLabel = Math.max(2, Math.round(8 * scaleNorm));
        int ddYMax = priceLabelY - minBetweenDropdownAndPriceLabel - rowH;
        int ddYMin = offersCbY + shareCheckboxSize + dividerGap + 1;
        int ddYFromPrice = priceLabelY - rowH - Math.max(2, Math.round(6 * scaleNorm));
        int ddYWanted = Math.max(ddYMin, Math.max(ddYFromPrice, headingY + WildexUiText.lineHeight(font) + dropdownTopGap));
        int ddY = Math.max(ddYMin, Math.min(ddYWanted, ddYMax));

        this.sharePlayersDropdown = new WildexDropdownWidget(panel.x() + pad, ddY, ddW, rowH);
        this.sharePlayersDropdown.setOpenUpwards(false);
        int rowsFit = Math.max(3, Math.min(5, (priceLabelY - ddY - minBetweenDropdownAndPriceLabel) / rowH));
        this.sharePlayersDropdown.setMaxVisibleRows(rowsFit);
        this.sharePlayersDropdown.setEmptyText(SHARE_SELECT_PLAYER_LABEL.getString());
        this.sharePlayersDropdown.setOnOpenChanged(open -> {
            if (open) WildexNetworkClient.requestShareCandidates();
        });

        int iconSize = Math.max(10, Math.round(12 * scaleNorm));
        int iconGap = 3;
        int priceW = Math.max(40, (ddW / 2) - iconSize - iconGap);
        this.sharePriceInput = new WildexSearchBox(
                this.font,
                panel.x() + pad,
                priceY,
                priceW,
                priceRowH,
                SHARE_PRICE_LABEL
        );
        this.sharePriceInput.setMaxLength(9);
        WildexUiTheme.Palette theme = WildexUiTheme.current();
        this.sharePriceInput.setTextColor(theme.ink());
        this.sharePriceInput.setTextColorUneditable(theme.ink());
        this.sharePriceInput.setBordered(false);
        this.sharePriceInput.setTextShadow(false);
        int textNudgeY = Math.max(1, (priceRowH - WildexUiText.lineHeight(font)) / 2);
        this.sharePriceInput.setTextNudge(2, textNudgeY);
        this.sharePriceInput.setResponder(raw -> {
            if (raw == null) return;
            String digits = raw.replaceAll("[^0-9]", "");
            int max = Math.max(0, WildexClientConfigView.shareOfferMaxPrice());
            if (!digits.isEmpty()) {
                try {
                    int parsed = Integer.parseInt(digits);
                    if (parsed > max) {
                        digits = Integer.toString(max);
                    }
                } catch (NumberFormatException ignored) {
                    digits = Integer.toString(max);
                }
            }
            if (!digits.equals(raw)) {
                this.sharePriceInput.setValue(digits);
            }
        });
        this.sharePriceInput.setValue("0");
        this.host.addShareWidget(this.sharePriceInput);

        this.shareSendOfferButton = new WildexPanelButton(
                panel.x() + pad,
                sendY,
                ddW,
                sendH,
                SHARE_SEND_LABEL,
                () -> {
                    String selectedName = this.sharePlayersDropdown == null ? "" : this.sharePlayersDropdown.selectedValue();
                    UUID target = selectedName == null ? null : this.shareCandidateByName.get(selectedName);
                    ResourceLocation mobId = ResourceLocation.tryParse(this.selectedMobIdSupplier.get());
                    if (target == null || mobId == null) return;

                    int price = 0;
                    if (this.sharePriceInput != null) {
                        String raw = this.sharePriceInput.getValue();
                        if (!raw.isBlank()) {
                            try {
                                price = Integer.parseInt(raw);
                            } catch (NumberFormatException ignored) {
                                price = 0;
                            }
                        }
                    }
                    WildexNetworkClient.sendShareOffer(target, mobId, price);
                }
        );
        this.host.addShareWidget(this.shareSendOfferButton);

        WildexScreenLayout.Area previewAreaForMargins = layout.rightPreviewArea();
        WildexScreenLayout.Area previewReset = layout.previewResetButtonArea();
        int closeSize = Math.max(10, previewReset.w());
        int resetRightMargin = Math.max(0, (previewAreaForMargins.x() + previewAreaForMargins.w()) - (previewReset.x() + previewReset.w()));
        int resetBottomMargin = Math.max(0, (previewAreaForMargins.y() + previewAreaForMargins.h()) - (previewReset.y() + previewReset.h()));
        int closeX = (panel.x() + panel.w()) - closeSize - resetRightMargin;
        int closeY = panel.y() + resetBottomMargin
                + (WildexThemes.isModernLayout() ? 5 : 0);
        this.shareCloseOverlayButton = new MobPreviewResetButton(
                closeX,
                closeY,
                closeSize,
                closeSize,
                "X",
                () -> {
                    closeOverlayIfOpen();
                    updateWidgetsVisibility();
                }
        );
        this.host.addShareWidget(this.shareCloseOverlayButton);
        this.host.addShareWidget(this.sharePlayersDropdown);

        refreshSharePlayerOptions();
        WildexNetworkClient.requestShareCandidates();
        WildexNetworkClient.requestSharePayoutStatus();
    }

    public void onShareCandidatesUpdated() {
        refreshSharePlayerOptions();
        updateWidgetsVisibility();
    }

    public void onSharePayoutStatusUpdated() {
        updateWidgetsVisibility();
    }

    public void refreshVisibility() {
        updateWidgetsVisibility();
    }

    public void onSelectionChanged() {
        closeOverlayIfOpen();
        updateWidgetsVisibility();
    }

    public void closeOverlayIfOpen() {
        if (!this.sharePanelOpen) return;
        this.sharePanelOpen = false;
        resetShareOfferSelection();
    }

    public boolean handleDropdownMouseScrolled(int mouseX, int mouseY, double scrollY, WildexScreenLayout layout) {
        if (this.sharePanelOpen && this.sharePlayersDropdown != null && this.sharePlayersDropdown.isOpen()) {
            WildexScreenLayout.Area share = layout.sharePanelArea();
            boolean inShare = mouseX >= share.x() && mouseX < share.x() + share.w() && mouseY >= share.y() && mouseY < share.y() + share.h();
            return inShare && this.sharePlayersDropdown.scrollByWheel(scrollY);
        }
        return false;
    }

    public boolean handleDropdownMouseClicked(double mouseX, double mouseY, int button, WildexScreenLayout layout) {
        if (this.sharePanelOpen && this.sharePlayersDropdown != null && this.sharePlayersDropdown.isOpen()) {
            if (this.sharePlayersDropdown.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
            int mx = (int) Math.floor(mouseX);
            int my = (int) Math.floor(mouseY);
            WildexScreenLayout.Area share = layout.sharePanelArea();
            boolean inShare = mx >= share.x() && mx < share.x() + share.w() && my >= share.y() && my < share.y() + share.h();
            if (inShare) {
                this.sharePlayersDropdown.closeList();
                return true;
            }
        }
        return false;
    }

    public void tick() {
        updateWidgetsVisibility();

        Minecraft minecraft = Minecraft.getInstance();
        if (this.sharePanelOpen && minecraft != null && minecraft.level != null) {
            if ((minecraft.level.getGameTime() % 20L) == 0L) {
                WildexNetworkClient.requestShareCandidates();
            }
        }
        if (shareSingleplayerNoticeTicks > 0) {
            shareSingleplayerNoticeTicks--;
        }

        if (minecraft != null && minecraft.level != null) {
            if ((minecraft.level.getGameTime() % 40L) == 0L) {
                WildexNetworkClient.requestSharePayoutStatus();
            }
        }
    }

    public void applyThemeToInputs(WildexUiTheme.Palette theme) {
        if (theme == null) return;
        if (this.sharePriceInput != null) {
            this.sharePriceInput.setTextColor(theme.ink());
            this.sharePriceInput.setTextColorUneditable(theme.ink());
        }
    }

    public void syncTopButtonsPosition(WildexScreenLayout layout) {
        if (layout == null || this.shareEntryButton == null || this.shareClaimPayoutsButton == null) return;
        if (!WildexClientConfigView.hiddenMode() || !WildexClientConfigView.shareOffersEnabled()) return;

        int styleW = scaledStyleButtonW();
        int styleH = scaledStyleButtonH();
        int styleMargin = scaledStyleButtonMargin();
        int styleYOffset = scaledStyleButtonYOffset();
        int shareButtonH = styleH;
        int shareButtonW = styleW;
        int claimButtonW = shareButtonW;
        WildexScreenLayout.Area shareTopArea = layout.shareEntryButtonArea(
                styleW, styleH, styleMargin, styleYOffset, shareButtonW, shareButtonH
        );
        WildexScreenLayout.Area claimTopArea = layout.shareClaimButtonArea(
                styleW, styleH, styleMargin, styleYOffset, shareButtonW, shareButtonH, claimButtonW
        );

        this.shareEntryButton.setX(shareTopArea.x());
        this.shareEntryButton.setY(shareTopArea.y());
        this.shareEntryButton.setWidth(shareButtonW);
        this.shareEntryButton.setHeight(shareButtonH);
        this.shareClaimPayoutsButton.setX(claimTopArea.x());
        this.shareClaimPayoutsButton.setY(claimTopArea.y());
        this.shareClaimPayoutsButton.setWidth(claimButtonW);
        this.shareClaimPayoutsButton.setHeight(shareButtonH);
    }

    public void relayout(WildexScreenLayout layout) {
        if (layout == null) return;
        if (!WildexClientConfigView.hiddenMode() || !WildexClientConfigView.shareOffersEnabled()) return;
        syncTopButtonsPosition(layout);

        WildexScreenLayout.Area panel = layout.sharePanelArea();
        WildexScreenLayout.Area leftAction = layout.leftActionArea();
        int leftRefSize = Math.max(8, leftAction.h());
        int shareCheckboxSize = Math.max(12, leftRefSize);

        float fitScale = Math.max(0.65f, Math.min(1.0f, panel.h() / 170.0f));
        float scaleNorm = Math.max(0.55f, resolveShareOverlayScale() * fitScale);
        int pad = Math.max(3, Math.round(8 * scaleNorm));
        int rowTextMin = WildexUiText.lineHeight(font) + Math.max(6, Math.round(6 * scaleNorm));
        int rowH = Math.max(rowTextMin, Math.round(20 * scaleNorm));
        int ddW = Math.max(80, panel.w() - (pad * 2) - 1);

        int offersCbX = panel.x() + pad;
        int offersCbY = panel.y() + pad;
        if (this.shareOpenToOffersCheckbox != null) {
            this.shareOpenToOffersCheckbox.setX(offersCbX);
            this.shareOpenToOffersCheckbox.setY(offersCbY);
            this.shareOpenToOffersCheckbox.setWidth(shareCheckboxSize);
            this.shareOpenToOffersCheckbox.setHeight(shareCheckboxSize);
        }

        int dividerGap = Math.max(2, Math.round(6 * scaleNorm));
        int headingGap = Math.max(4, Math.round(12 * scaleNorm));
        int dropdownTopGap = Math.max(2, Math.round(6 * scaleNorm));

        int sendH = Math.max(rowTextMin, Math.round(20 * scaleNorm));
        int sendBottomLift = Math.max(2, Math.round(4 * scaleNorm));
        int sendY = panel.y() + panel.h() - pad - sendH - sendBottomLift;
        int priceRowH = Math.max(WildexUiText.lineHeight(font) + Math.max(6, Math.round(6 * scaleNorm)), Math.round(18 * scaleNorm));
        int priceToSendGap = Math.max(2, Math.round(8 * scaleNorm));
        int priceY = sendY - priceToSendGap - priceRowH;
        int priceLabelGap = Math.max(1, Math.round(4 * scaleNorm));
        int priceLabelY = priceY - WildexUiText.lineHeight(font) - priceLabelGap;

        int headingY = offersCbY + shareCheckboxSize + dividerGap + headingGap;
        int minBetweenDropdownAndPriceLabel = Math.max(2, Math.round(8 * scaleNorm));
        int ddYMax = priceLabelY - minBetweenDropdownAndPriceLabel - rowH;
        int ddYMin = offersCbY + shareCheckboxSize + dividerGap + 1;
        int ddYFromPrice = priceLabelY - rowH - Math.max(2, Math.round(6 * scaleNorm));
        int ddYWanted = Math.max(ddYMin, Math.max(ddYFromPrice, headingY + WildexUiText.lineHeight(font) + dropdownTopGap));
        int ddY = Math.max(ddYMin, Math.min(ddYWanted, ddYMax));

        if (this.sharePlayersDropdown != null) {
            this.sharePlayersDropdown.setX(panel.x() + pad);
            this.sharePlayersDropdown.setY(ddY);
            this.sharePlayersDropdown.setWidth(ddW);
            this.sharePlayersDropdown.setHeight(rowH);
            int rowsFit = Math.max(3, Math.min(5, (priceLabelY - ddY - minBetweenDropdownAndPriceLabel) / rowH));
            this.sharePlayersDropdown.setMaxVisibleRows(rowsFit);
        }

        int iconSize = Math.max(10, Math.round(12 * scaleNorm));
        int iconGap = 3;
        int priceW = Math.max(40, (ddW / 2) - iconSize - iconGap);
        if (this.sharePriceInput != null) {
            this.sharePriceInput.setX(panel.x() + pad);
            this.sharePriceInput.setY(priceY);
            this.sharePriceInput.setWidth(priceW);
            this.sharePriceInput.setHeight(priceRowH);
            int textNudgeY = Math.max(1, (priceRowH - WildexUiText.lineHeight(font)) / 2);
            this.sharePriceInput.setTextNudge(2, textNudgeY);
        }

        if (this.shareSendOfferButton != null) {
            this.shareSendOfferButton.setX(panel.x() + pad);
            this.shareSendOfferButton.setY(sendY);
            this.shareSendOfferButton.setWidth(ddW);
            this.shareSendOfferButton.setHeight(sendH);
        }

        WildexScreenLayout.Area previewAreaForMargins = layout.rightPreviewArea();
        WildexScreenLayout.Area previewReset = layout.previewResetButtonArea();
        int closeSize = Math.max(10, previewReset.w());
        int resetRightMargin = Math.max(0, (previewAreaForMargins.x() + previewAreaForMargins.w()) - (previewReset.x() + previewReset.w()));
        int resetBottomMargin = Math.max(0, (previewAreaForMargins.y() + previewAreaForMargins.h()) - (previewReset.y() + previewReset.h()));
        int closeX = (panel.x() + panel.w()) - closeSize - resetRightMargin;
        int closeY = panel.y() + resetBottomMargin + (WildexThemes.isModernLayout() ? 5 : 0);

        if (this.shareCloseOverlayButton != null) {
            this.shareCloseOverlayButton.setX(closeX);
            this.shareCloseOverlayButton.setY(closeY);
            this.shareCloseOverlayButton.setWidth(closeSize);
            this.shareCloseOverlayButton.setHeight(closeSize);
        }
    }

    public boolean isPanelOpen() {
        return sharePanelOpen;
    }

    public boolean isPanelVisible() {
        return this.sharePanelOpen && WildexClientConfigView.hiddenMode() && WildexClientConfigView.shareOffersEnabled();
    }

    public boolean shouldShowNotice() {
        return shareSingleplayerNoticeTicks > 0
                && WildexClientConfigView.hiddenMode()
                && WildexClientConfigView.shareOffersEnabled()
                && singleplayerShareBlockedSupplier.getAsBoolean()
                && shareEligibleSupplier.getAsBoolean();
    }

    public boolean blocksRightInfo() {
        return isPanelVisible() || shouldShowNotice();
    }

    public void renderSingleplayerNotice(GuiGraphics graphics, WildexScreenLayout layout) {
        WildexUiTheme.Palette theme = WildexUiTheme.current();
        WildexScreenLayout.Area info = layout.rightInfoArea();
        int maxW = Math.max(20, info.w() - 16);
        float textScale = WildexUiScale.get();
        int logicalMaxW = Math.max(1, Math.round(maxW / Math.max(0.001f, textScale)));
        List<FormattedCharSequence> lines = this.font.split(SHARE_SINGLEPLAYER_NOTICE, logicalMaxW);
        int totalH = lines.size() * WildexUiText.lineHeight(font);
        int maxLineW = 0;
        for (FormattedCharSequence line : lines) {
            maxLineW = Math.max(maxLineW, WildexUiText.width(font, line));
        }

        int x = info.x() + Math.max(4, (info.w() - maxLineW) / 2);
        int maxX = info.x() + info.w() - maxLineW - 4;
        if (x > maxX) x = maxX;
        if (x < info.x() + 4) x = info.x() + 4;
        int y = info.y() + Math.max(4, (info.h() - totalH) / 2);
        WildexScissor.enablePhysical(graphics, info.x() + 2, info.y() + 2, info.x() + info.w() - 2, info.y() + info.h() - 2);
        for (FormattedCharSequence line : lines) {
            WildexUiText.draw(graphics, this.font, line, x, y, theme.ink(), false);
            y += WildexUiText.lineHeight(font);
        }
        graphics.disableScissor();
    }

    public void renderPanel(GuiGraphics graphics, WildexScreenLayout layout) {
        WildexUiTheme.Palette theme = WildexUiTheme.current();
        WildexScreenLayout.Area panel = layout.sharePanelArea();
        float fitScale = Math.max(0.65f, Math.min(1.0f, panel.h() / 170.0f));
        float scaleNorm = Math.max(0.55f, resolveShareOverlayScale() * fitScale);
        int pad = Math.max(3, Math.round(8 * scaleNorm));
        int headingOffset = Math.max(3, Math.round(6 * scaleNorm));
        int dividerOffset = Math.max(2, Math.round(4 * scaleNorm));
        int priceLabelOffset = Math.max(1, Math.round(1 * scaleNorm));

        if (this.shareOpenToOffersCheckbox != null && this.shareOpenToOffersCheckbox.visible) {
            int lx = this.shareOpenToOffersCheckbox.getX() + this.shareOpenToOffersCheckbox.getWidth() + 5;
            int ly = this.shareOpenToOffersCheckbox.getY()
                    + ((this.shareOpenToOffersCheckbox.getHeight() - WildexUiText.lineHeight(font)) / 2);
            int textRightLimit = panel.x() + panel.w() - pad;
            if (this.shareCloseOverlayButton != null && this.shareCloseOverlayButton.visible) {
                textRightLimit = Math.min(textRightLimit, this.shareCloseOverlayButton.getX() - 4);
            }
            int maxLabelWidth = Math.max(0, textRightLimit - lx);
            if (maxLabelWidth > 0) {
                String label = ellipsizeToWidth(SHARE_ACCEPT_OFFERS_LABEL.getString(), maxLabelWidth);
                WildexUiText.draw(graphics, this.font, label, lx, ly, theme.ink(), false);
            }

            int lineY;
            if (this.sharePlayersDropdown != null && this.sharePlayersDropdown.visible) {
                int headingY = this.sharePlayersDropdown.getY() - WildexUiText.lineHeight(font) - headingOffset;
                lineY = headingY - dividerOffset;
            } else {
                lineY = this.shareOpenToOffersCheckbox.getY() + this.shareOpenToOffersCheckbox.getHeight() + 6;
            }
            int x0 = panel.x() + pad;
            int x1 = panel.x() + panel.w() - pad;
            graphics.fill(x0, lineY, x1, lineY + 1, theme.frameOuter());
            graphics.fill(x0, lineY + 1, x1, lineY + 2, theme.frameInner());
        }

        if (this.sharePlayersDropdown != null && this.sharePlayersDropdown.visible) {
            int headingY = this.sharePlayersDropdown.getY() - WildexUiText.lineHeight(font) - headingOffset;
            int minimumHeadingY = panel.y() + pad;
            if (headingY >= minimumHeadingY) {
                WildexUiText.draw(graphics, this.font, SHARE_SEND_HEADING_LABEL, panel.x() + pad, headingY, theme.heading(), false);
            }
        }

        if (WildexClientConfigView.shareOffersPaymentEnabled()) {
            if (this.sharePriceInput != null) {
                int priceLabelY = this.sharePriceInput.getY() - WildexUiText.lineHeight(font) - priceLabelOffset;
                WildexUiText.draw(graphics, this.font, SHARE_PRICE_LABEL, this.sharePriceInput.getX(), priceLabelY, theme.ink(), false);
                int iconX = this.sharePriceInput.getX() + this.sharePriceInput.getWidth() + 2;
                int iconSize = Math.max(10, Math.round(12 * scaleNorm));
                int iconY = this.sharePriceInput.getY() + ((this.sharePriceInput.getHeight() - iconSize) / 2);
                Item currency = resolveShareCurrencyItem();
                drawScaledItem(graphics, new ItemStack(currency), iconX, iconY, iconSize);
                int maxPrice = Math.max(0, WildexClientConfigView.shareOfferMaxPrice());
                Component maxText = Component.translatable("gui.wildex.share.max_price", maxPrice);
                int maxX = iconX + iconSize + 2;
                int maxY = this.sharePriceInput.getY() + ((this.sharePriceInput.getHeight() - WildexUiText.lineHeight(font)) / 2);
                int textRightLimit = panel.x() + panel.w() - pad;
                int maxTextW = Math.max(0, textRightLimit - maxX);
                if (maxTextW > 0) {
                    String clipped = ellipsizeToWidth(maxText.getString(), maxTextW);
                    WildexUiText.draw(graphics, this.font, clipped, maxX, maxY, theme.ink(), false);
                }
            }
        }
    }

    public boolean isShareEntryButtonHovered() {
        return this.shareEntryButton != null && this.shareEntryButton.isHovered();
    }

    public boolean isShareClaimButtonHovered() {
        return this.shareClaimPayoutsButton != null && this.shareClaimPayoutsButton.isHovered();
    }

    public boolean isShareOpenOffersHovered() {
        return this.shareOpenToOffersCheckbox != null && this.shareOpenToOffersCheckbox.isHovered();
    }

    public Component shareTooltip() {
        return SHARE_TOOLTIP;
    }

    public Component shareClaimTooltip() {
        return SHARE_CLAIM_PAYOUTS_TOOLTIP;
    }

    public Component shareOpenOffersTooltip() {
        return SHARE_OPEN_TO_OFFERS_TOOLTIP;
    }

    private void refreshSharePlayerOptions() {
        if (this.sharePlayersDropdown == null) return;
        this.shareCandidateByName.clear();
        ArrayList<String> names = new ArrayList<>();
        for (WildexNetworkClient.ShareCandidate c : WildexNetworkClient.shareCandidates()) {
            if (c == null || c.playerId() == null || c.playerName() == null || c.playerName().isBlank()) continue;
            names.add(c.playerName());
            this.shareCandidateByName.put(c.playerName(), c.playerId());
        }
        names.sort(Comparator.naturalOrder());
        List<String> next = List.copyOf(names);
        if (next.equals(this.lastSharePlayerOptions)) return;
        this.lastSharePlayerOptions = next;
        this.sharePlayersDropdown.setOptions(next, this.sharePlayersDropdown.selectedValue());
    }

    private void updateWidgetsVisibility() {
        boolean enabled = WildexClientConfigView.hiddenMode() && WildexClientConfigView.shareOffersEnabled();
        boolean shareEligible = shareEligibleSupplier.getAsBoolean();
        if (!shareEligible && this.sharePanelOpen) {
            closeOverlayIfOpen();
        }
        boolean notice = enabled && shareEligible && singleplayerShareBlockedSupplier.getAsBoolean() && shareSingleplayerNoticeTicks > 0;
        boolean panel = enabled && shareEligible && this.sharePanelOpen;
        boolean paymentEnabled = WildexClientConfigView.shareOffersPaymentEnabled();

        if (this.shareEntryButton != null) {
            this.shareEntryButton.visible = enabled && shareEligible;
            this.shareEntryButton.active = enabled && shareEligible;
        }
        if (this.shareClaimPayoutsButton != null) {
            boolean hasPayouts = WildexNetworkClient.pendingSharePayoutTotal() > 0;
            Minecraft mc = Minecraft.getInstance();
            boolean singleplayerDebug = mc != null
                    && mc.hasSingleplayerServer()
                    && mc.getCurrentServer() == null
                    && WildexClientConfigView.debugMode();
            this.shareClaimPayoutsButton.visible = enabled && (hasPayouts || singleplayerDebug);
            this.shareClaimPayoutsButton.active = enabled && (hasPayouts || singleplayerDebug);
        }
        if (this.shareOpenToOffersCheckbox != null) {
            this.shareOpenToOffersCheckbox.visible = panel && !notice;
            this.shareOpenToOffersCheckbox.active = panel && !notice;
            suppressShareAcceptOffersUpdate = true;
            this.shareOpenToOffersCheckbox.setChecked(WildexNetworkClient.selfAcceptingOffers(), false);
            suppressShareAcceptOffersUpdate = false;
        }

        if (this.sharePlayersDropdown != null) {
            this.sharePlayersDropdown.visible = panel && !notice;
            this.sharePlayersDropdown.active = panel && !notice;
        }
        if (this.sharePriceInput != null) {
            this.sharePriceInput.visible = panel && paymentEnabled && !notice;
            this.sharePriceInput.setEditable(panel && paymentEnabled && !notice);
            if (!(panel && paymentEnabled && !notice)) {
                this.sharePriceInput.setFocused(false);
            }
        }
        if (this.shareSendOfferButton != null) {
            this.shareSendOfferButton.visible = panel && !notice;
            boolean hasTarget = this.sharePlayersDropdown != null
                    && !this.sharePlayersDropdown.selectedValue().isBlank()
                    && this.shareCandidateByName.containsKey(this.sharePlayersDropdown.selectedValue());
            this.shareSendOfferButton.active = panel && hasTarget && !notice;
        }
        if (this.shareCloseOverlayButton != null) {
            this.shareCloseOverlayButton.visible = panel && !notice;
            this.shareCloseOverlayButton.active = panel && !notice;
        }
    }

    private void resetShareOfferSelection() {
        if (this.sharePlayersDropdown != null) {
            this.sharePlayersDropdown.closeList();
            this.sharePlayersDropdown.clearSelection();
        }
    }

    private String ellipsizeToWidth(String raw, int maxWidth) {
        if (raw == null || raw.isEmpty() || maxWidth <= 0) return "";
        if (WildexUiText.width(font, raw) <= maxWidth) return raw;
        String dots = "...";
        int dotsWidth = WildexUiText.width(font, dots);
        if (dotsWidth >= maxWidth) return this.font.plainSubstrByWidth(raw, maxWidth);
        String head = this.font.plainSubstrByWidth(raw, maxWidth - dotsWidth);
        return head + dots;
    }

    private static Item resolveShareCurrencyItem() {
        String raw = WildexClientConfigView.shareOfferCurrencyItem();
        ResourceLocation rl = ResourceLocation.tryParse(raw == null ? "" : raw.trim());
        if (rl == null) return Items.EMERALD;
        Item it = BuiltInRegistries.ITEM.getOptional(rl).orElse(Items.EMERALD);
        if (it == Items.AIR) return Items.EMERALD;
        return it;
    }

    private static float resolveShareOverlayScale() {
        float s = WildexUiScale.get();
        if (s <= 1.00f) return 0.90f;
        if (s <= 1.50f) return 1.05f;
        if (s <= 2.00f) return 1.20f;
        if (s <= 3.00f) return 1.35f;
        return 1.50f;
    }

    private static void drawScaledItem(GuiGraphics graphics, ItemStack stack, int x, int y, int size) {
        if (size == 16) {
            graphics.renderItem(stack, x, y);
            return;
        }
        float s = size / 16.0f;
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0.0f);
        graphics.pose().scale(s, s, 1.0f);
        graphics.renderItem(stack, 0, 0);
        graphics.pose().popPose();
    }

    private int scaledStyleButtonW() {
        return Math.max(20, Math.round(this.styleButtonW * WildexUiScale.get()));
    }

    private int scaledStyleButtonH() {
        return Math.max(10, Math.round(this.styleButtonH * WildexUiScale.get()));
    }

    private int scaledStyleButtonMargin() {
        return Math.max(2, Math.round(this.styleButtonMargin * WildexUiScale.get()));
    }

    private int scaledStyleButtonYOffset() {
        return Math.round(this.styleButtonYOffset * WildexUiScale.get());
    }
}



