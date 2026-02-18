package de.coldfang.wildex.server;

import de.coldfang.wildex.config.CommonConfig;
import de.coldfang.wildex.network.S2CSharePayoutStatusPayload;
import de.coldfang.wildex.util.WildexMobFilters;
import de.coldfang.wildex.world.WildexWorldPlayerDiscoveryData;
import de.coldfang.wildex.world.WildexWorldPlayerSharePayoutData;
import de.coldfang.wildex.world.WildexWorldPlayerSharePrefsData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

@SuppressWarnings("resource")
public final class WildexShareOfferService {

    private static final long OFFER_TTL_TICKS = 20L * 60L;
    private static final long SEND_COOLDOWN_TICKS = 10L;
    private static final int MAX_OFFERS_PER_TARGET = 32;
    private static final int MAX_OFFERS_PER_SENDER = 3;
    private static final AtomicLong NEXT_OFFER_ID = new AtomicLong(1L);

    private static final Map<UUID, List<Offer>> OFFERS_BY_TARGET = new HashMap<>();
    private static final Map<UUID, Long> NEXT_SEND_TICK = new HashMap<>();

    private WildexShareOfferService() {
    }

    public static List<Candidate> listAcceptingCandidates(ServerPlayer requester) {
        if (requester == null) return List.of();
        if (!(requester.level() instanceof ServerLevel level)) return List.of();
        cleanupExpired(level);

        WildexWorldPlayerSharePrefsData prefs = WildexWorldPlayerSharePrefsData.get(level);
        List<Candidate> out = new ArrayList<>();
        for (ServerPlayer p : level.getServer().getPlayerList().getPlayers()) {
            if (p == null || p.getUUID().equals(requester.getUUID())) continue;
            if (!prefs.isAcceptingOffers(p.getUUID())) continue;
            out.add(new Candidate(p.getUUID(), p.getGameProfile().getName()));
        }
        out.sort(Comparator.comparing(Candidate::playerName, String.CASE_INSENSITIVE_ORDER));
        return List.copyOf(out);
    }

    public static boolean isAcceptingOffers(ServerPlayer player) {
        if (player == null) return false;
        if (!(player.level() instanceof ServerLevel level)) return false;
        return WildexWorldPlayerSharePrefsData.get(level).isAcceptingOffers(player.getUUID());
    }

    public static void setAcceptingOffers(ServerPlayer player, boolean accepting) {
        if (player == null) return;
        if (!(player.level() instanceof ServerLevel level)) return;
        WildexWorldPlayerSharePrefsData.get(level).setAcceptingOffers(player.getUUID(), accepting);
    }

    public static void createOffer(ServerPlayer sender, UUID targetId, ResourceLocation mobId, int requestedPrice) {
        if (sender == null || targetId == null || mobId == null) return;
        if (!(sender.level() instanceof ServerLevel level)) return;
        cleanupExpired(level);

        if (!CommonConfig.INSTANCE.hiddenMode.get() || !CommonConfig.INSTANCE.shareOffersEnabled.get()) {
            sender.displayClientMessage(Component.translatable("message.wildex.share.offers_disabled"), false);
            return;
        }

        if (!WildexMobFilters.isTrackable(mobId)) {
            sender.displayClientMessage(Component.translatable("message.wildex.share.invalid_mob"), false);
            return;
        }

        if (targetId.equals(sender.getUUID())) {
            sender.displayClientMessage(Component.translatable("message.wildex.share.cannot_offer_self"), false);
            return;
        }

        ServerPlayer target = level.getServer().getPlayerList().getPlayer(targetId);
        if (target == null) {
            sender.displayClientMessage(Component.translatable("message.wildex.share.target_offline"), false);
            return;
        }

        WildexWorldPlayerDiscoveryData discoveryData = WildexWorldPlayerDiscoveryData.get(level);
        if (!discoveryData.isDiscovered(sender.getUUID(), mobId)) {
            sender.displayClientMessage(Component.translatable("message.wildex.share.not_discovered_sender"), false);
            return;
        }
        if (discoveryData.isDiscovered(target.getUUID(), mobId)) {
            sender.displayClientMessage(Component.translatable("message.wildex.share.target_already_discovered"), false);
            return;
        }

        WildexWorldPlayerSharePrefsData prefs = WildexWorldPlayerSharePrefsData.get(level);
        if (!prefs.isAcceptingOffers(target.getUUID())) {
            sender.displayClientMessage(Component.translatable("message.wildex.share.target_not_accepting"), false);
            return;
        }

        long now = level.getGameTime();
        long nextAllowed = NEXT_SEND_TICK.getOrDefault(sender.getUUID(), 0L);
        if (now < nextAllowed) {
            sender.displayClientMessage(Component.translatable("message.wildex.share.sending_too_fast"), true);
            return;
        }
        NEXT_SEND_TICK.put(sender.getUUID(), now + SEND_COOLDOWN_TICKS);

        int maxPrice = Math.max(0, CommonConfig.INSTANCE.shareOfferMaxPrice.get());
        int price;
        if (!CommonConfig.INSTANCE.shareOffersPaymentEnabled.get()) {
            price = 0;
        } else {
            price = Math.max(0, Math.min(requestedPrice, maxPrice));
        }

        List<Offer> offers = OFFERS_BY_TARGET.computeIfAbsent(target.getUUID(), ignored -> new ArrayList<>());
        int senderOpenCount = countOpenOffersBySender(sender.getUUID());
        if (senderOpenCount >= MAX_OFFERS_PER_SENDER) {
            sender.displayClientMessage(Component.translatable("message.wildex.share.sender_offer_limit"), false);
            return;
        }
        if (offers.size() >= MAX_OFFERS_PER_TARGET) {
            sender.displayClientMessage(Component.translatable("message.wildex.share.target_queue_full"), false);
            return;
        }

        long offerId = NEXT_OFFER_ID.getAndIncrement();
        ResourceLocation currencyId = resolveCurrencyItemId();
        offers.add(new Offer(
                offerId,
                sender.getUUID(),
                sender.getGameProfile().getName(),
                target.getUUID(),
                mobId,
                price,
                currencyId,
                now + OFFER_TTL_TICKS
        ));

        sendIncomingOfferMessage(target, sender, mobId, price, currencyId, offerId);
        sender.displayClientMessage(
                Component.translatable("message.wildex.share.offer_sent", target.getGameProfile().getName()),
                false
        );
    }

    public static void respondToOffer(ServerPlayer receiver, long offerId, boolean accept) {
        if (receiver == null) return;
        if (!(receiver.level() instanceof ServerLevel level)) return;
        cleanupExpired(level);

        List<Offer> offers = OFFERS_BY_TARGET.get(receiver.getUUID());
        if (offers == null || offers.isEmpty()) {
            receiver.displayClientMessage(Component.translatable("message.wildex.share.offer_not_found"), false);
            return;
        }

        Offer offer = null;
        int idx = -1;
        for (int i = 0; i < offers.size(); i++) {
            Offer o = offers.get(i);
            if (o.offerId == offerId) {
                offer = o;
                idx = i;
                break;
            }
        }
        if (offer == null) {
            receiver.displayClientMessage(Component.translatable("message.wildex.share.offer_not_found"), false);
            return;
        }

        offers.remove(idx);
        if (offers.isEmpty()) OFFERS_BY_TARGET.remove(receiver.getUUID());

        if (offer.expiresAtTick <= level.getGameTime()) {
            receiver.displayClientMessage(Component.translatable("message.wildex.share.offer_expired"), false);
            return;
        }

        ServerPlayer sender = level.getServer().getPlayerList().getPlayer(offer.senderId);
        if (sender == null) {
            receiver.displayClientMessage(Component.translatable("message.wildex.share.sender_offline"), false);
            return;
        }

        WildexWorldPlayerDiscoveryData discoveryData = WildexWorldPlayerDiscoveryData.get(level);
        if (!discoveryData.isDiscovered(sender.getUUID(), offer.mobId)) {
            receiver.displayClientMessage(Component.translatable("message.wildex.share.offer_failed"), false);
            sender.displayClientMessage(Component.translatable("message.wildex.share.offer_failed"), false);
            return;
        }

        if (!accept) {
            receiver.displayClientMessage(Component.translatable("message.wildex.share.offer_declined"), false);
            sender.displayClientMessage(
                    Component.translatable("message.wildex.share.offer_declined_by_target", receiver.getGameProfile().getName()),
                    false
            );
            return;
        }

        if (discoveryData.isDiscovered(receiver.getUUID(), offer.mobId)) {
            receiver.displayClientMessage(Component.translatable("message.wildex.share.target_already_discovered"), false);
            return;
        }

        Item currencyItem = BuiltInRegistries.ITEM.getOptional(offer.currencyItemId).orElse(Items.EMERALD);
        if (currencyItem == Items.AIR) currencyItem = Items.EMERALD;

        if (offer.price > 0) {
            int count = countItem(receiver, currencyItem);
            if (count < offer.price) {
                receiver.displayClientMessage(Component.translatable("message.wildex.share.not_enough_currency"), false);
                sender.displayClientMessage(
                        Component.translatable("message.wildex.share.offer_failed_target_no_currency", receiver.getGameProfile().getName()),
                        false
                );
                return;
            }
        }

        if (offer.price > 0) {
            removeItem(receiver, currencyItem, offer.price);
            ResourceLocation currencyId = BuiltInRegistries.ITEM.getKey(currencyItem);
            WildexWorldPlayerSharePayoutData.get(level).add(sender.getUUID(), currencyId, offer.price);
            sender.displayClientMessage(Component.translatable("message.wildex.share.payout_queued"), false);
            PacketDistributor.sendToPlayer(sender, new S2CSharePayoutStatusPayload(getPendingPayoutTotal(sender)));
        }

        boolean discovered = WildexDiscoveryService.discover(receiver, offer.mobId, WildexDiscoveryService.DiscoverySource.SHARE);
        if (!discovered) {
            receiver.displayClientMessage(Component.translatable("message.wildex.share.offer_failed"), false);
            return;
        }

        receiver.displayClientMessage(
                Component.translatable("message.wildex.share.offer_accepted"),
                false
        );
        sender.displayClientMessage(
                Component.translatable("message.wildex.share.offer_accepted_by_target", receiver.getGameProfile().getName()),
                false
        );
    }

    public static int getPendingPayoutTotal(ServerPlayer player) {
        if (player == null) return 0;
        if (!(player.level() instanceof ServerLevel level)) return 0;
        return WildexWorldPlayerSharePayoutData.get(level).total(player.getUUID());
    }

    public static void claimPendingPayouts(ServerPlayer player) {
        if (player == null) return;
        if (!(player.level() instanceof ServerLevel level)) return;
        Map<ResourceLocation, Integer> payouts = WildexWorldPlayerSharePayoutData.get(level).takeAll(player.getUUID());
        if (payouts.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.wildex.share.no_payouts"), false);
            return;
        }

        int grantedTotal = 0;
        for (Map.Entry<ResourceLocation, Integer> e : payouts.entrySet()) {
            ResourceLocation itemId = e.getKey();
            int amount = Math.max(0, e.getValue() == null ? 0 : e.getValue());
            if (itemId == null || amount <= 0) continue;
            Item item = BuiltInRegistries.ITEM.getOptional(itemId).orElse(Items.AIR);
            if (item == Items.AIR) continue;
            giveItem(player, item, amount);
            grantedTotal += amount;
        }

        if (grantedTotal > 0) {
            player.displayClientMessage(Component.translatable("message.wildex.share.payout_claimed", grantedTotal), false);
        } else {
            player.displayClientMessage(Component.translatable("message.wildex.share.no_payouts"), false);
        }
    }

    private static void sendIncomingOfferMessage(
            ServerPlayer target,
            ServerPlayer sender,
            ResourceLocation mobId,
            int price,
            ResourceLocation currencyId,
            long offerId
    ) {
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(mobId).orElse(null);
        Component mobName = type == null ? Component.literal(mobId.toString()) : type.getDescription();
        Item currencyItem = BuiltInRegistries.ITEM.getOptional(currencyId).orElse(Items.EMERALD);
        Component currencyName = currencyItem.getDescription();

        target.displayClientMessage(
                Component.translatable(
                        "message.wildex.share.offer_received",
                        sender.getGameProfile().getName(),
                        mobName,
                        price,
                        currencyName
                ),
                false
        );

        MutableComponent acceptBtn = Component.literal("[Accept]")
                .withStyle(style -> style
                        .withColor(ChatFormatting.GREEN)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/wildex offer accept " + offerId))
                );

        MutableComponent declineBtn = Component.literal("[Decline]")
                .withStyle(style -> style
                        .withColor(ChatFormatting.RED)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/wildex offer decline " + offerId))
                );

        target.displayClientMessage(Component.empty().append(acceptBtn).append(Component.literal(" ")).append(declineBtn), false);
    }

    private static void cleanupExpired(ServerLevel level) {
        long now = level.getGameTime();
        for (var entry : OFFERS_BY_TARGET.entrySet()) {
            List<Offer> offers = entry.getValue();
            if (offers == null || offers.isEmpty()) continue;
            Iterator<Offer> it = offers.iterator();
            while (it.hasNext()) {
                Offer offer = it.next();
                if (offer.expiresAtTick > now) continue;
                notifyExpiredOffer(level, offer);
                it.remove();
            }
        }
        OFFERS_BY_TARGET.entrySet().removeIf(e -> e.getValue() == null || e.getValue().isEmpty());
    }

    private static void notifyExpiredOffer(ServerLevel level, Offer offer) {
        if (level == null || offer == null) return;
        ServerPlayer sender = level.getServer().getPlayerList().getPlayer(offer.senderId);
        ServerPlayer receiver = level.getServer().getPlayerList().getPlayer(offer.receiverId);

        if (sender != null) {
            sender.displayClientMessage(Component.translatable("message.wildex.share.offer_expired_sender"), false);
        }
        if (receiver != null) {
            receiver.displayClientMessage(Component.translatable("message.wildex.share.offer_expired_receiver"), false);
        }
    }

    private static int countOpenOffersBySender(UUID senderId) {
        if (senderId == null) return 0;
        int count = 0;
        for (List<Offer> offers : OFFERS_BY_TARGET.values()) {
            for (Offer offer : offers) {
                if (senderId.equals(offer.senderId)) {
                    count++;
                }
            }
        }
        return count;
    }

    private static ResourceLocation resolveCurrencyItemId() {
        String raw = CommonConfig.INSTANCE.shareOfferCurrencyItem.get();
        ResourceLocation rl = ResourceLocation.tryParse(raw == null ? "" : raw.trim());
        if (rl == null) return BuiltInRegistries.ITEM.getKey(Items.EMERALD);
        Item it = BuiltInRegistries.ITEM.getOptional(rl).orElse(Items.EMERALD);
        if (it == Items.AIR) return BuiltInRegistries.ITEM.getKey(Items.EMERALD);
        return rl;
    }

    private static int countItem(ServerPlayer player, Item item) {
        int total = 0;
        for (ItemStack s : player.getInventory().items) {
            if (s.is(item)) total += s.getCount();
        }
        for (ItemStack s : player.getInventory().offhand) {
            if (s.is(item)) total += s.getCount();
        }
        return total;
    }

    private static void removeItem(ServerPlayer player, Item item, int amount) {
        int left = amount;
        for (ItemStack s : player.getInventory().items) {
            if (!s.is(item)) continue;
            int take = Math.min(left, s.getCount());
            s.shrink(take);
            left -= take;
            if (left <= 0) break;
        }
        if (left > 0) {
            for (ItemStack s : player.getInventory().offhand) {
                if (!s.is(item)) continue;
                int take = Math.min(left, s.getCount());
                s.shrink(take);
                left -= take;
                if (left <= 0) break;
            }
        }
        player.inventoryMenu.broadcastChanges();
    }

    private static void giveItem(ServerPlayer player, Item item, int amount) {
        int left = amount;
        int maxStack = item.getDefaultMaxStackSize();
        while (left > 0) {
            int give = Math.min(left, Math.max(1, maxStack));
            ItemStack stack = new ItemStack(item, give);
            boolean ok = player.addItem(stack);
            if (!ok && !stack.isEmpty()) {
                player.drop(stack, false);
            }
            left -= give;
        }
    }

    public record Candidate(UUID playerId, String playerName) {
    }

    private record Offer(
            long offerId,
            UUID senderId,
            String senderName,
            UUID receiverId,
            ResourceLocation mobId,
            int price,
            ResourceLocation currencyItemId,
            long expiresAtTick
    ) {
    }
}
