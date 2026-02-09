package de.coldfang.wildex.client.data.extractor;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

import java.util.Collection;

public final class WildexEntityTypeTags {

    private WildexEntityTypeTags() {
    }

    public static final TagKey<EntityType<?>> CAN_BREATHE_UNDER_WATER = tagMinecraft("can_breathe_under_water");
    public static final TagKey<EntityType<?>> FALL_DAMAGE_IMMUNE = tagMinecraft("fall_damage_immune");
    public static final TagKey<EntityType<?>> SENSITIVE_TO_BANE_OF_ARTHROPODS = tagMinecraft("sensitive_to_bane_of_arthropods");
    public static final TagKey<EntityType<?>> SENSITIVE_TO_IMPALING = tagMinecraft("sensitive_to_impaling");
    public static final TagKey<EntityType<?>> SENSITIVE_TO_SMITE = tagMinecraft("sensitive_to_smite");

    public static boolean isAny(EntityType<?> type, Collection<TagKey<EntityType<?>>> tags) {
        if (type == null || tags == null || tags.isEmpty()) return false;
        for (TagKey<EntityType<?>> t : tags) {
            if (t != null && type.is(t)) return true;
        }
        return false;
    }

    private static TagKey<EntityType<?>> tagMinecraft(String path) {
        return TagKey.create(
                Registries.ENTITY_TYPE,
                ResourceLocation.fromNamespaceAndPath("minecraft", path)
        );
    }
}
