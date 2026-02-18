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
import de.coldfang.wildex.config.ClientConfig.DesignStyle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class WildexScreen extends Screen {

    private static final Component TITLE = Component.translatable("screen.wildex.title");
    private static final Component SEARCH_LABEL = Component.translatable("gui.wildex.search");
    private static final Component SHARE_TOOLTIP = Component.translatable("tooltip.wildex.share_entry");
    private static final Component SHARE_CLAIM_PAYOUTS_TOOLTIP = Component.translatable("tooltip.wildex.share_claim_payouts");
    private static final Component SHARE_BUTTON_LABEL = Component.translatable("gui.wildex.share.button");
    private static final Component SHARE_CLAIM_PAYOUTS_LABEL = Component.translatable("gui.wildex.share.claim_payouts");
    private static final Component SHARE_SINGLEPLAYER_NOTICE = Component.translatable("gui.wildex.share.singleplayer_only");
    private static final Component SHARE_SEND_LABEL = Component.translatable("gui.wildex.share.send_offer");
    private static final Component SHARE_SEND_HEADING_LABEL = Component.translatable("gui.wildex.share.send_offer_heading");
    private static final Component SHARE_ACCEPT_OFFERS_LABEL = Component.translatable("gui.wildex.share.accept_offers");
    private static final Component SHARE_SELECT_PLAYER_LABEL = Component.translatable("gui.wildex.share.choose_player");
    private static final Component SHARE_PRICE_LABEL = Component.translatable("gui.wildex.share.price");
    private static final Component SHARE_OPEN_TO_OFFERS_TOOLTIP = Component.translatable("tooltip.wildex.share_accept_offers");

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

    private static final int TIP_PAD = 4;
    private static final int TIP_LINE_GAP = 2;
    private static final int TIP_MAX_W = 170;

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
    private static final int PREVIEW_HINT_PAD_X = 6;
    private static final int PREVIEW_HINT_PAD_Y = 4;
    private static final int MODERN_PREVIEW_DECOUPLED_NUDGE_X = 4;
    private static final int MODERN_CONTROLS_EXTRA_SHIFT_X = 3;

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
    private Button shareEntryButton;
    private WildexPanelButton shareClaimPayoutsButton;
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

        int btnW = STYLE_BUTTON_W;
        int btnX = this.width - btnW - STYLE_BUTTON_MARGIN;
        int btnY = STYLE_BUTTON_MARGIN + STYLE_BUTTON_Y_OFFSET;

        this.clearWidgets();

        this.addRenderableWidget(new WildexStyleButton(btnX, btnY, btnW, STYLE_BUTTON_H, () -> {
            DesignStyle next = nextStyle(ClientConfig.INSTANCE.designStyle.get());
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
        int shareButtonH = STYLE_BUTTON_H;
        int shareButtonW = STYLE_BUTTON_W;
        int claimButtonW = Math.max(54, this.font.width(SHARE_CLAIM_PAYOUTS_LABEL) + 10);
        int btnX = this.width - STYLE_BUTTON_W - STYLE_BUTTON_MARGIN;
        int btnY = STYLE_BUTTON_MARGIN + STYLE_BUTTON_Y_OFFSET + STYLE_BUTTON_H + 2;
        int claimX = btnX + shareButtonW - claimButtonW;
        int claimY = btnY + shareButtonH + 2;

        this.shareEntryButton = new WildexStyleButton(
                btnX,
                btnY,
                shareButtonW,
                shareButtonH,
                SHARE_BUTTON_LABEL,
                () -> {
                    if (isSingleplayerShareBlocked()) {
                        sharePanelOpen = false;
                        shareSingleplayerNoticeTicks = 80;
                        updateShareWidgetsVisibility();
                        if (this.shareEntryButton != null) this.shareEntryButton.setFocused(false);
                        this.setFocused(null);
                        return;
                    }
                    sharePanelOpen = !sharePanelOpen;
                    if (!sharePanelOpen) {
                        resetShareOfferSelection();
                    }
                    refreshSharePlayerOptions();
                    updateShareWidgetsVisibility();
                    if (this.shareEntryButton != null) this.shareEntryButton.setFocused(false);
                    this.setFocused(null);
                }
        );
        this.addRenderableWidget(this.shareEntryButton);

        this.shareClaimPayoutsButton = new WildexPanelButton(
                claimX,
                claimY,
                claimButtonW,
                shareButtonH,
                SHARE_CLAIM_PAYOUTS_LABEL,
                WildexNetworkClient::claimSharePayouts
        );
        this.addRenderableWidget(this.shareClaimPayoutsButton);

        WildexScreenLayout.Area panel = sharePanelArea();
        float fitScale = Math.max(0.65f, Math.min(1.0f, panel.h() / 170.0f));
        float scaleNorm = Math.max(0.55f, resolveShareOverlayScale() * fitScale);
        int pad = Math.max(3, Math.round(8 * scaleNorm));
        int rowH = Math.max(11, Math.round(18 * scaleNorm));
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
        this.addRenderableWidget(this.shareOpenToOffersCheckbox);

        int dividerGap = Math.max(2, Math.round(6 * scaleNorm));
        int headingGap = Math.max(4, Math.round(12 * scaleNorm));
        int dropdownTopGap = Math.max(2, Math.round(6 * scaleNorm));

        int sendH = Math.max(11, Math.round(18 * scaleNorm));
        int sendY = panel.y() + panel.h() - pad - sendH;
        int priceRowH = Math.max(11, Math.round(16 * scaleNorm));
        int priceToSendGap = Math.max(2, Math.round(8 * scaleNorm));
        int priceY = sendY - priceToSendGap - priceRowH;
        int priceLabelGap = Math.max(1, Math.round(4 * scaleNorm));
        int priceLabelY = priceY - this.font.lineHeight - priceLabelGap;

        int headingY = offersCbY + shareCheckboxSize + dividerGap + headingGap;
        int minBetweenDropdownAndPriceLabel = Math.max(2, Math.round(8 * scaleNorm));
        int ddYMax = priceLabelY - minBetweenDropdownAndPriceLabel - rowH;
        int ddYMin = offersCbY + shareCheckboxSize + dividerGap + 1;
        int ddYFromPrice = priceLabelY - rowH - Math.max(2, Math.round(6 * scaleNorm));
        int ddYWanted = Math.max(ddYMin, Math.max(ddYFromPrice, headingY + this.font.lineHeight + dropdownTopGap));
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
        int textNudgeY = Math.max(1, (priceRowH - this.font.lineHeight) / 2);
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
        this.addRenderableWidget(this.sharePriceInput);

        this.shareSendOfferButton = new WildexPanelButton(
                panel.x() + pad,
                sendY,
                ddW,
                sendH,
                SHARE_SEND_LABEL,
                () -> {
                    String selectedName = this.sharePlayersDropdown == null ? "" : this.sharePlayersDropdown.selectedValue();
                    UUID target = selectedName == null ? null : this.shareCandidateByName.get(selectedName);
                    ResourceLocation mobId = ResourceLocation.tryParse(this.state.selectedMobId());
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
        this.addRenderableWidget(this.shareSendOfferButton);

        WildexScreenLayout.Area previewAreaForMargins = layout.rightPreviewArea();
        WildexScreenLayout.Area previewReset = layout.previewResetButtonArea();
        int closeSize = Math.max(10, previewReset.w());
        int resetRightMargin = Math.max(0, (previewAreaForMargins.x() + previewAreaForMargins.w()) - (previewReset.x() + previewReset.w()));
        int resetBottomMargin = Math.max(0, (previewAreaForMargins.y() + previewAreaForMargins.h()) - (previewReset.y() + previewReset.h()));
        int closeX = (panel.x() + panel.w()) - closeSize - resetRightMargin;
        int closeY = panel.y() + resetBottomMargin
                + (ClientConfig.INSTANCE.designStyle.get() == DesignStyle.MODERN ? 5 : 0);
        this.shareCloseOverlayButton = new MobPreviewResetButton(
                closeX,
                closeY,
                closeSize,
                closeSize,
                "X",
                () -> {
                    closeShareOverlayIfOpen();
                    updateShareWidgetsVisibility();
                }
        );
        this.addRenderableWidget(this.shareCloseOverlayButton);
        this.addRenderableWidget(this.sharePlayersDropdown);

        refreshSharePlayerOptions();
        WildexNetworkClient.requestShareCandidates();
        WildexNetworkClient.requestSharePayoutStatus();
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

    private void updateShareWidgetsVisibility() {
        boolean enabled = WildexClientConfigView.hiddenMode() && WildexClientConfigView.shareOffersEnabled();
        boolean shareEligible = isShareEligibleForSelectedMob();
        if (!shareEligible && this.sharePanelOpen) {
            closeShareOverlayIfOpen();
        }
        boolean notice = enabled && shareEligible && isSingleplayerShareBlocked() && shareSingleplayerNoticeTicks > 0;
        boolean panel = enabled && shareEligible && this.sharePanelOpen;
        boolean paymentEnabled = WildexClientConfigView.shareOffersPaymentEnabled();

        if (this.rightTabsWidget != null) this.rightTabsWidget.visible = !panel && !notice;
        if (this.previewResetButton != null) this.previewResetButton.visible = true;
        if (this.shareEntryButton != null) {
            this.shareEntryButton.visible = enabled && shareEligible;
            this.shareEntryButton.active = enabled && shareEligible;
        }
        if (this.shareClaimPayoutsButton != null) {
            boolean hasPayouts = WildexNetworkClient.pendingSharePayoutTotal() > 0;
            this.shareClaimPayoutsButton.visible = enabled && hasPayouts;
            this.shareClaimPayoutsButton.active = enabled && hasPayouts;
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

    private boolean isShareEligibleForSelectedMob() {
        ResourceLocation mobId = ResourceLocation.tryParse(this.state.selectedMobId());
        if (mobId == null) return false;

        if (!WildexClientConfigView.hiddenMode()) return true;
        return WildexDiscoveryCache.isDiscovered(mobId);
    }

    private void closeShareOverlayIfOpen() {
        if (!this.sharePanelOpen) return;
        this.sharePanelOpen = false;
        resetShareOfferSelection();
    }

    private void resetShareOfferSelection() {
        if (this.sharePlayersDropdown != null) {
            this.sharePlayersDropdown.closeList();
            this.sharePlayersDropdown.clearSelection();
        }
    }

    public void onShareCandidatesUpdated() {
        refreshSharePlayerOptions();
        updateShareWidgetsVisibility();
    }

    public void onSharePayoutStatusUpdated() {
        updateShareWidgetsVisibility();
    }

    public void onServerConfigUpdated() {
        updateShareWidgetsVisibility();
        if (WildexClientConfigView.hiddenMode()) {
            WildexNetworkClient.requestDiscoveredMobs();
        }
    }

    private WildexScreenLayout.Area sharePanelArea() {
        WildexScreenLayout.Area tabs = layout.rightTabsArea();
        WildexScreenLayout.Area info = layout.rightInfoArea();
        int x = tabs.x();
        int y = tabs.y();
        int w = Math.max(1, Math.max(tabs.w(), info.w()));
        int h = Math.max(1, (info.y() + info.h()) - y);
        return new WildexScreenLayout.Area(x, y, w, h);
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
        closeShareOverlayIfOpen();
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

        if (this.sharePanelOpen && this.sharePlayersDropdown != null && this.sharePlayersDropdown.isOpen()) {
            WildexScreenLayout.Area share = sharePanelArea();
            boolean inShare = mx >= share.x() && mx < share.x() + share.w() && my >= share.y() && my < share.y() + share.h();
            if (inShare && this.sharePlayersDropdown.scrollByWheel(scrollY)) {
                return true;
            }
        }

        if (mobPreviewRenderer.isMouseOverPreview(this.layout, mx, my)) {
            mobPreviewRenderer.adjustZoom(scrollY);
            return true;
        }

        if (this.layout != null && this.state.selectedTab() == WildexTab.STATS) {
            if (this.sharePanelOpen) return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
            if (rightInfoRenderer.scrollStats(mx, my, scrollY)) {
                return true;
            }
        }

        if (this.layout != null && this.state.selectedTab() == WildexTab.SPAWNS) {
            if (this.sharePanelOpen) return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
            WildexScreenLayout.Area a = this.layout.rightInfoArea();
            if (a != null && mouseX >= a.x() && mouseX < a.x() + a.w() && mouseY >= a.y() && mouseY < a.y() + a.h()) {
                rightInfoRenderer.scrollSpawn(scrollY);
                return true;
            }
        }
        if (this.layout != null && this.state.selectedTab() == WildexTab.LOOT) {
            if (this.sharePanelOpen) return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
            if (rightInfoRenderer.scrollLoot(mx, my, scrollY)) {
                return true;
            }
        }

        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.sharePanelOpen && this.sharePlayersDropdown != null && this.sharePlayersDropdown.isOpen()) {
            if (this.sharePlayersDropdown.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
            int mx = (int) Math.floor(mouseX);
            int my = (int) Math.floor(mouseY);
            WildexScreenLayout.Area share = sharePanelArea();
            boolean inShare = mx >= share.x() && mx < share.x() + share.w() && my >= share.y() && my < share.y() + share.h();
            if (inShare) {
                this.sharePlayersDropdown.closeList();
                return true;
            }
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
            if (this.sharePanelOpen) return false;
            int mx = (int) Math.floor(mouseX);
            int my = (int) Math.floor(mouseY);
            if (rightInfoRenderer.handleSpawnMouseClicked(mx, my, button, this.state)) {
                return true;
            }
        }
        if (this.layout != null && this.state.selectedTab() == WildexTab.STATS) {
            if (this.sharePanelOpen) return false;
            int mx = (int) Math.floor(mouseX);
            int my = (int) Math.floor(mouseY);
            if (rightInfoRenderer.handleStatsMouseClicked(mx, my, button)) {
                return true;
            }
        }
        if (this.layout != null && this.state.selectedTab() == WildexTab.LOOT) {
            if (this.sharePanelOpen) return false;
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
            if (this.sharePanelOpen) return false;
            int mx = (int) Math.floor(mouseX);
            int my = (int) Math.floor(mouseY);
            if (rightInfoRenderer.handleSpawnMouseDragged(mx, my, button)) {
                return true;
            }
        }
        if (this.layout != null && this.state.selectedTab() == WildexTab.STATS) {
            if (this.sharePanelOpen) return false;
            int mx = (int) Math.floor(mouseX);
            int my = (int) Math.floor(mouseY);
            if (rightInfoRenderer.handleStatsMouseDragged(mx, my, button)) {
                return true;
            }
        }
        if (this.layout != null && this.state.selectedTab() == WildexTab.LOOT) {
            if (this.sharePanelOpen) return false;
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
            if (this.sharePanelOpen) return false;
            if (rightInfoRenderer.handleSpawnMouseReleased(button)) {
                return true;
            }
        }
        if (this.layout != null && this.state.selectedTab() == WildexTab.STATS) {
            if (this.sharePanelOpen) return false;
            if (rightInfoRenderer.handleStatsMouseReleased(button)) {
                return true;
            }
        }
        if (this.layout != null && this.state.selectedTab() == WildexTab.LOOT) {
            if (this.sharePanelOpen) return false;
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
            int trophyBg = ClientConfig.INSTANCE.designStyle.get() == DesignStyle.VINTAGE ? TROPHY_BG_VINTAGE : TROPHY_BG_MODERN;
            drawDockedTrophyFrame(graphics, trophyArea, theme, 3, trophyBg, TROPHY_BG_EXTEND_LEFT, TROPHY_BG_EXTEND_RIGHT);

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
        drawScaledText(graphics, entriesText, entriesX, entriesY, entriesScale, theme.ink());

        if (WildexClientConfigView.hiddenMode()) {
            WildexScreenLayout.Area discArea = layout.leftDiscoveryCounterArea();

            int discovered = WildexDiscoveryCache.count();
            int total = mobIndex.totalCount();

            Component discText = Component.translatable("gui.wildex.discovered_count", discovered, total);
            float discScale = resolveEntriesTextScale();
            int discTextH = Math.max(1, Math.round(this.font.lineHeight * discScale));

            int discX = discArea.x();
            int discY = discArea.y() + ((discArea.h() - discTextH) / 2);
            drawScaledText(graphics, discText, discX, discY, discScale, theme.ink());
        }

        mobPreviewRenderer.render(graphics, layout, state, mouseX, mouseY, partialTick);
        WildexScreenLayout.Area previewArea = layout.rightPreviewArea();
        WildexScreenLayout.Area previewResetArea = layout.previewResetButtonArea();
        HintBounds previewHint = drawPreviewControlsHint(graphics, previewArea, previewResetArea);

        boolean showShareNotice = shareSingleplayerNoticeTicks > 0
                && WildexClientConfigView.hiddenMode()
                && WildexClientConfigView.shareOffersEnabled()
                && isSingleplayerShareBlocked()
                && isShareEligibleForSelectedMob();
        if (this.sharePanelOpen && WildexClientConfigView.hiddenMode() && WildexClientConfigView.shareOffersEnabled()) {
            drawPanelFrame(graphics, sharePanelArea(), theme);
            renderSharePanel(graphics);
        } else if (showShareNotice) {
            drawPanelFrame(graphics, layout.rightTabsArea(), theme);
            renderShareSingleplayerNotice(graphics);
        } else {
            drawPanelFrame(graphics, layout.rightTabsArea(), theme);
        }

        WildexMobData data = mobDataResolver.resolve(state.selectedMobId());

        rightHeaderRenderer.render(graphics, this.font, layout.rightHeaderArea(), state, data.header(), theme.ink());
        if (ClientConfig.INSTANCE.designStyle.get() == DesignStyle.VINTAGE) {
            drawRoundedPanelFrame(graphics, layout.rightHeaderArea(), theme, 3);
        }

        if (!(this.sharePanelOpen && WildexClientConfigView.hiddenMode() && WildexClientConfigView.shareOffersEnabled()) && !showShareNotice) {
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
            renderWildexTooltip(graphics, List.of(RESET_PREVIEW_TOOLTIP), mouseX, mouseY);
        }

        if (this.discoveredOnlyCheckbox != null && this.discoveredOnlyCheckbox.isHovered()) {
            Component tip = this.discoveredOnlyCheckbox.tooltip();
            if (tip != null) graphics.renderTooltip(this.font, tip, mouseX, mouseY);
        }

        if (trophyDrawn && isMouseOverRect(mouseX, mouseY, trophyX - TROPHY_FRAME_PAD_X, trophyY - TROPHY_FRAME_PAD_Y, trophyHitW, trophyHitH)) {
            renderWildexTooltip(graphics, TROPHY_TOOLTIP, mouseX, mouseY);
        }

        if (!mobPreviewRenderer.isDraggingPreview() && isMouseOverPreviewControlsHint(mouseX, mouseY, previewHint)) {
            renderWildexTooltip(graphics, PREVIEW_CONTROLS_TOOLTIP, mouseX, mouseY);
        }
        if (this.shareEntryButton != null && this.shareEntryButton.isHovered()) {
            renderWildexTooltip(graphics, List.of(SHARE_TOOLTIP), mouseX, mouseY);
        }
        if (this.shareClaimPayoutsButton != null && this.shareClaimPayoutsButton.isHovered()) {
            renderWildexTooltip(graphics, List.of(SHARE_CLAIM_PAYOUTS_TOOLTIP), mouseX, mouseY);
        }
        if (this.shareOpenToOffersCheckbox != null && this.shareOpenToOffersCheckbox.isHovered()) {
            renderWildexTooltip(graphics, List.of(SHARE_OPEN_TO_OFFERS_TOOLTIP), mouseX, mouseY);
        }
        if (this.testGuiScaleButton != null && this.testGuiScaleButton.isHovered()) {
            renderWildexTooltip(graphics, List.of(TEST_GUI_SCALE_TOOLTIP), mouseX, mouseY);
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
        if (this.sharePriceInput != null) {
            this.sharePriceInput.setTextColor(theme.ink());
            this.sharePriceInput.setTextColorUneditable(theme.ink());
        }
    }

    private void syncShareTopButtonsPosition() {
        if (this.layout == null || this.shareEntryButton == null || this.shareClaimPayoutsButton == null) return;
        if (!WildexClientConfigView.hiddenMode() || !WildexClientConfigView.shareOffersEnabled()) return;

        int shareButtonH = this.shareEntryButton.getHeight();
        int shareButtonW = this.shareEntryButton.getWidth();
        int claimButtonW = this.shareClaimPayoutsButton.getWidth();

        int btnX = this.width - STYLE_BUTTON_W - STYLE_BUTTON_MARGIN;
        int btnY = STYLE_BUTTON_MARGIN + STYLE_BUTTON_Y_OFFSET + STYLE_BUTTON_H + 2;
        int claimX = btnX + shareButtonW - claimButtonW;
        int claimY = btnY + shareButtonH + 2;

        this.shareEntryButton.setX(btnX);
        this.shareEntryButton.setY(btnY);
        this.shareClaimPayoutsButton.setX(claimX);
        this.shareClaimPayoutsButton.setY(claimY);
    }

    private void renderShareSingleplayerNotice(GuiGraphics graphics) {
        WildexUiTheme.Palette theme = WildexUiTheme.current();
        WildexScreenLayout.Area info = layout.rightInfoArea();
        int maxW = Math.max(20, info.w() - 16);
        List<FormattedCharSequence> lines = this.font.split(SHARE_SINGLEPLAYER_NOTICE, maxW);
        int totalH = lines.size() * this.font.lineHeight;
        int maxLineW = 0;
        for (FormattedCharSequence line : lines) {
            maxLineW = Math.max(maxLineW, this.font.width(line));
        }

        int x = info.x() + Math.max(4, (info.w() - maxLineW) / 2);
        int y = info.y() + Math.max(4, (info.h() - totalH) / 2);
        for (FormattedCharSequence line : lines) {
            graphics.drawString(this.font, line, x, y, theme.ink(), false);
            y += this.font.lineHeight;
        }
    }

    private void renderSharePanel(GuiGraphics graphics) {
        WildexUiTheme.Palette theme = WildexUiTheme.current();
        WildexScreenLayout.Area panel = sharePanelArea();
        float fitScale = Math.max(0.65f, Math.min(1.0f, panel.h() / 170.0f));
        float scaleNorm = Math.max(0.55f, resolveShareOverlayScale() * fitScale);
        int pad = Math.max(3, Math.round(8 * scaleNorm));
        int headingOffset = Math.max(3, Math.round(6 * scaleNorm));
        int dividerOffset = Math.max(2, Math.round(4 * scaleNorm));
        int priceLabelOffset = Math.max(1, Math.round(1 * scaleNorm));

        if (this.shareOpenToOffersCheckbox != null && this.shareOpenToOffersCheckbox.visible) {
            int lx = this.shareOpenToOffersCheckbox.getX() + this.shareOpenToOffersCheckbox.getWidth() + 5;
            int ly = this.shareOpenToOffersCheckbox.getY()
                    + ((this.shareOpenToOffersCheckbox.getHeight() - this.font.lineHeight) / 2);
            int textRightLimit = panel.x() + panel.w() - pad;
            if (this.shareCloseOverlayButton != null && this.shareCloseOverlayButton.visible) {
                textRightLimit = Math.min(textRightLimit, this.shareCloseOverlayButton.getX() - 4);
            }
            int maxLabelWidth = Math.max(0, textRightLimit - lx);
            if (maxLabelWidth > 0) {
                String label = ellipsizeToWidth(SHARE_ACCEPT_OFFERS_LABEL.getString(), maxLabelWidth);
                graphics.drawString(this.font, label, lx, ly, theme.ink(), false);
            }

            int lineY;
            if (this.sharePlayersDropdown != null && this.sharePlayersDropdown.visible) {
                int headingY = this.sharePlayersDropdown.getY() - this.font.lineHeight - headingOffset;
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
            int headingY = this.sharePlayersDropdown.getY() - this.font.lineHeight - headingOffset;
            int minimumHeadingY = panel.y() + pad;
            if (headingY >= minimumHeadingY) {
                graphics.drawString(this.font, SHARE_SEND_HEADING_LABEL, panel.x() + pad, headingY, theme.heading(), false);
            }
        }

        if (WildexClientConfigView.shareOffersPaymentEnabled()) {
            if (this.sharePriceInput != null) {
                int priceLabelY = this.sharePriceInput.getY() - this.font.lineHeight - priceLabelOffset;
                graphics.drawString(this.font, SHARE_PRICE_LABEL, this.sharePriceInput.getX(), priceLabelY, theme.ink(), false);
                int iconX = this.sharePriceInput.getX() + this.sharePriceInput.getWidth() + 2;
                int iconY = this.sharePriceInput.getY() + ((this.sharePriceInput.getHeight() - 16) / 2);
                Item currency = resolveShareCurrencyItem();
                graphics.renderItem(new ItemStack(currency), iconX, iconY);
                int maxPrice = Math.max(0, WildexClientConfigView.shareOfferMaxPrice());
                Component maxText = Component.translatable("gui.wildex.share.max_price", maxPrice);
                int maxX = iconX + 18;
                int maxY = this.sharePriceInput.getY() + ((this.sharePriceInput.getHeight() - this.font.lineHeight) / 2);
                int textRightLimit = panel.x() + panel.w() - pad;
                int maxTextW = Math.max(0, textRightLimit - maxX);
                if (maxTextW > 0) {
                    String clipped = ellipsizeToWidth(maxText.getString(), maxTextW);
                    graphics.drawString(this.font, clipped, maxX, maxY, theme.ink(), false);
                }
            }
        }
    }

    private String ellipsizeToWidth(String raw, int maxWidth) {
        if (raw == null || raw.isEmpty() || maxWidth <= 0) return "";
        if (this.font.width(raw) <= maxWidth) return raw;
        String dots = "...";
        int dotsWidth = this.font.width(dots);
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
        boolean modern = ClientConfig.INSTANCE.designStyle.get() == DesignStyle.MODERN;
        if (modern) {
            // Keep Controls anchored when preview is nudged independently in Modern.
            hintBaseX = hintBaseX - MODERN_PREVIEW_DECOUPLED_NUDGE_X + MODERN_CONTROLS_EXTRA_SHIFT_X;
        }
        if (modern && previewResetArea != null) {
            hintBaseY = previewResetArea.y() + Math.max(0, (previewResetArea.h() - scaledTextH) / 2);
        }
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
                WildexUiTheme.current().inkMuted(),
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
        graphics.drawString(this.font, this.versionLabel, Math.round(x * inv), Math.round(y * inv), WildexUiTheme.current().inkMuted(), false);
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

        WildexUiTheme.Palette theme = WildexUiTheme.current();
        g.fill(x0, y0, x1, y1, theme.tooltipBg());

        g.fill(x0, y0, x1, y0 + 1, theme.tooltipBorder());
        g.fill(x0, y1 - 1, x1, y1, theme.tooltipBorder());
        g.fill(x0, y0, x0 + 1, y1, theme.tooltipBorder());
        g.fill(x1 - 1, y0, x1, y1, theme.tooltipBorder());

        int tx = x0 + TIP_PAD;
        int ty = y0 + TIP_PAD;

        for (FormattedCharSequence line : wrapped) {
            g.drawString(this.font, line, tx, ty, theme.tooltipText(), false);
            ty += this.font.lineHeight + TIP_LINE_GAP;
        }
    }

    private static boolean isMouseOverRect(int mouseX, int mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }

    private static boolean isMouseOverPreviewControlsHint(int mouseX, int mouseY, HintBounds hint) {
        if (hint == null || hint.w() <= 0 || hint.h() <= 0) return false;
        return mouseX >= hint.x() && mouseX < hint.x() + hint.w() && mouseY >= hint.y() && mouseY < hint.y() + hint.h();
    }

    private record HintBounds(int x, int y, int w, int h) {
    }

    private static void drawPanelFrame(GuiGraphics graphics, WildexScreenLayout.Area a, WildexUiTheme.Palette theme) {
        if (a == null) return;

        int x0 = a.x();
        int y0 = a.y();
        int x1 = a.x() + a.w();
        int y1 = a.y() + a.h();

        graphics.fill(x0 + 2, y0 + 2, x1 - 2, y1 - 2, theme.frameBg());

        graphics.fill(x0, y0, x1, y0 + 1, theme.frameOuter());
        graphics.fill(x0, y1 - 1, x1, y1, theme.frameOuter());
        graphics.fill(x0, y0, x0 + 1, y1, theme.frameOuter());
        graphics.fill(x1 - 1, y0, x1, y1, theme.frameOuter());

        graphics.fill(x0 + 1, y0 + 1, x1 - 1, y0 + 2, theme.frameInner());
        graphics.fill(x0 + 1, y1 - 2, x1 - 1, y1 - 1, theme.frameInner());
        graphics.fill(x0 + 1, y0 + 1, x0 + 2, y1 - 1, theme.frameInner());
        graphics.fill(x1 - 2, y0 + 1, x1 - 1, y1 - 1, theme.frameInner());
    }

    private static void drawRoundedPanelFrame(
            GuiGraphics graphics,
            WildexScreenLayout.Area a,
            WildexUiTheme.Palette theme,
            int cornerCut
    ) {
        if (a == null) return;
        int x0 = a.x();
        int y0 = a.y();
        int x1 = a.x() + a.w();
        int y1 = a.y() + a.h();
        int c = Math.max(1, cornerCut);

        graphics.fill(x0 + 2, y0 + 2, x1 - 2, y1 - 2, theme.frameBg());

        graphics.fill(x0 + c, y0, x1 - c, y0 + 1, theme.frameOuter());
        graphics.fill(x0 + c, y1 - 1, x1 - c, y1, theme.frameOuter());
        graphics.fill(x0, y0 + c, x0 + 1, y1 - c, theme.frameOuter());
        graphics.fill(x1 - 1, y0 + c, x1, y1 - c, theme.frameOuter());

        graphics.fill(x0 + c, y0 + 1, x1 - c, y0 + 2, theme.frameInner());
        graphics.fill(x0 + c, y1 - 2, x1 - c, y1 - 1, theme.frameInner());
        graphics.fill(x0 + 1, y0 + c, x0 + 2, y1 - c, theme.frameInner());
        graphics.fill(x1 - 2, y0 + c, x1 - 1, y1 - c, theme.frameInner());

        drawFrameCornerChamfers(graphics, x0, y0, x1, y1, c, theme.frameOuter());
        drawFrameCornerChamfers(graphics, x0 + 1, y0 + 1, x1 - 1, y1 - 1, Math.max(1, c - 1), theme.frameInner());
    }

    private static void drawDockedTrophyFrame(
            GuiGraphics graphics,
            WildexScreenLayout.Area a,
            WildexUiTheme.Palette theme,
            int cornerCut,
            int bgColor,
            int extendLeft,
            int extendRight
    ) {
        if (a == null) return;
        int x0 = a.x();
        int y0 = a.y();
        int x1 = a.x() + a.w();
        int y1 = a.y() + a.h();
        int c = Math.max(1, cornerCut);
        int xl = x0 - Math.max(0, extendLeft);
        int xr = x1 + Math.max(0, extendRight);

        // Fill extends left/right so the icon remains visually centered.
        graphics.fill(xl + 2, y0 + 2, xr - 2, y1 - 2, bgColor);

        // No right border: keeps the "coming from behind" look.
        graphics.fill(xl + c, y0, xr, y0 + 1, theme.frameOuter());
        graphics.fill(xl + c, y1 - 1, x1, y1, theme.frameOuter());
        graphics.fill(xl, y0 + c, xl + 1, y1 - c, theme.frameOuter());

        graphics.fill(xl + c, y0 + 1, xr, y0 + 2, theme.frameInner());
        graphics.fill(xl + c, y1 - 2, x1, y1 - 1, theme.frameInner());
        graphics.fill(xl + 1, y0 + c, xl + 2, y1 - c, theme.frameInner());

        drawLeftFrameCornerChamfers(graphics, xl, y0, y1, c, theme.frameOuter());
        drawLeftFrameCornerChamfers(graphics, xl + 1, y0 + 1, y1 - 1, Math.max(1, c - 1), theme.frameInner());
    }

    private static void drawLeftFrameCornerChamfers(
            GuiGraphics graphics,
            int x0,
            int y0,
            int y1,
            int cut,
            int color
    ) {
        for (int i = 0; i < cut; i++) {
            int l = x0 + (cut - 1 - i);
            int t = y0 + i;
            int b = y1 - 1 - i;
            graphics.fill(l, t, l + 1, t + 1, color);
            graphics.fill(l, b, l + 1, b + 1, color);
        }
    }

    private static void drawFrameCornerChamfers(
            GuiGraphics graphics,
            int x0,
            int y0,
            int x1,
            int y1,
            int cut,
            int color
    ) {
        for (int i = 0; i < cut; i++) {
            int l = x0 + (cut - 1 - i);
            int r = x1 - (cut - i);
            int t = y0 + i;
            int b = y1 - 1 - i;
            graphics.fill(l, t, l + 1, t + 1, color);
            graphics.fill(r, t, r + 1, t + 1, color);
            graphics.fill(l, b, l + 1, b + 1, color);
            graphics.fill(r, b, r + 1, b + 1, color);
        }
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

    private float resolveShareOverlayScale() {
        Minecraft mc = Minecraft.getInstance();
        int opt = mc.options.guiScale().get();
        int gui = opt == 0 ? Math.max(1, mc.getWindow().calculateScale(0, mc.isEnforceUnicode())) : opt;
        if (gui >= 6) return 0.72f;
        if (gui == 5) return 0.80f;
        if (gui == 4) return 0.90f;
        return 1.0f;
    }

    private void drawScaledText(GuiGraphics graphics, Component text, int x, int y, float scale, int color) {
        if (scale >= 0.999f) {
            graphics.drawString(this.font, text, x, y, color, false);
            return;
        }
        float inv = 1.0f / scale;
        graphics.pose().pushPose();
        graphics.pose().scale(scale, scale, 1.0f);
        graphics.drawString(this.font, text, Math.round(x * inv), Math.round(y * inv), color, false);
        graphics.pose().popPose();
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

