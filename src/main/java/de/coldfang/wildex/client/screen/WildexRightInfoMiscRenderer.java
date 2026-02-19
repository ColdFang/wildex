package de.coldfang.wildex.client.screen;

import de.coldfang.wildex.client.data.extractor.WildexEntityTypeTags;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

import java.util.List;
import java.util.function.Predicate;

final class WildexRightInfoMiscRenderer {

    private static final int COLOR_TRUE = 0xFF2ECC71;
    private static final int COLOR_FALSE = 0xFFE74C3C;
    private static final int DIVIDER = 0x22301E14;
    private static final int COL_GAP = 6;

    private record TraitLine(String display, List<TagKey<EntityType<?>>> tags, Predicate<EntityType<?>> directCheck) {
    }

    private static final List<TraitLine> INFO_TRAITS = List.of(
            new TraitLine("gui.wildex.trait.immune_fire", List.of(), EntityType::fireImmune),
            new TraitLine("gui.wildex.trait.immune_drowning", List.of(WildexEntityTypeTags.CAN_BREATHE_UNDER_WATER), null),
            new TraitLine("gui.wildex.trait.immune_fall_damage", List.of(WildexEntityTypeTags.FALL_DAMAGE_IMMUNE), null),
            new TraitLine("gui.wildex.trait.weak_bane_of_arthropods", List.of(WildexEntityTypeTags.SENSITIVE_TO_BANE_OF_ARTHROPODS), null),
            new TraitLine("gui.wildex.trait.weak_impaling", List.of(WildexEntityTypeTags.SENSITIVE_TO_IMPALING), null),
            new TraitLine("gui.wildex.trait.weak_smite", List.of(WildexEntityTypeTags.SENSITIVE_TO_SMITE), null)
    );

    void render(
            GuiGraphics g,
            Font font,
            WildexScreenLayout.Area area,
            WildexScreenState state,
            int inkColor,
            int screenOriginX,
            int screenOriginY,
            float scale
    ) {
        int x = area.x() + WildexRightInfoRenderer.PAD_X;
        int y = area.y() + WildexRightInfoRenderer.PAD_Y;

        int rightLimitX = area.x() + area.w() - WildexRightInfoRenderer.PAD_RIGHT;
        int maxY = area.y() + area.h() - WildexRightInfoRenderer.PAD_Y;

        int line = Math.max(10, WildexUiText.lineHeight(font) + 3);

        EntityType<?> type = resolveSelectedType(state.selectedMobId());
        if (type == null) return;

        String tick = "\u2714";
        String cross = "\u2716";

        int markW = Math.max(WildexUiText.width(font, tick), WildexUiText.width(font, cross));
        int markColW = Math.max(10, markW + 2);
        int markColX0 = rightLimitX - markColW;
        int dividerX = markColX0 - Math.max(2, COL_GAP / 2);
        if (dividerX > x + 8) {
            g.fill(dividerX, y, dividerX + 1, maxY, DIVIDER);
        }
        int labelMaxW = Math.max(1, (dividerX - 2) - x);

        for (int i = 0; i < INFO_TRAITS.size(); i++) {
            if (y + WildexUiText.lineHeight(font) > maxY) break;

            TraitLine t = INFO_TRAITS.get(i);
            boolean has = evalTrait(type, t);

            String mark = has ? tick : cross;
            int markColor = has ? COLOR_TRUE : COLOR_FALSE;

            WildexRightInfoTabUtil.drawMarqueeIfNeeded(
                    g, font, WildexRightInfoTabUtil.tr(t.display()), x, y, labelMaxW, inkColor, screenOriginX, screenOriginY, scale
            );

            int markX = markColX0 + Math.max(0, (markColW - WildexUiText.width(font, mark)) / 2);
            WildexUiText.draw(g, font, mark, markX, y, markColor, false);

            int dividerY = y + WildexUiText.lineHeight(font) + 1;
            if (i < INFO_TRAITS.size() - 1 && dividerY + 1 < maxY) {
                g.fill(x, dividerY, rightLimitX, dividerY + 1, DIVIDER);
            }

            y += line;
        }
    }

    private static boolean evalTrait(EntityType<?> type, TraitLine t) {
        if (t.directCheck() != null) {
            return t.directCheck().test(type);
        }
        if (t.tags() == null || t.tags().isEmpty()) return false;
        return WildexEntityTypeTags.isAny(type, t.tags());
    }

    private static EntityType<?> resolveSelectedType(String selectedMobId) {
        if (selectedMobId == null || selectedMobId.isBlank()) return null;
        ResourceLocation rl = ResourceLocation.tryParse(selectedMobId);
        if (rl == null) return null;
        return BuiltInRegistries.ENTITY_TYPE.getOptional(rl).orElse(null);
    }
}



