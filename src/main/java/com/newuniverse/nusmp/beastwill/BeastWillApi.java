package com.newuniverse.nusmp.beastwill;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Why: the reusable integration point of the owner's brief (3.45.0), the "direct inheritance" half of acquisition: a powerful beast that WANTS to hand its will over (a quest, a relationship, a scripted
 * encounter, another mod) calls {@link #offer} and the player gets exactly the same preview and the same checks as with a core, and nothing is taken or given until they say yes. No new code is
 * needed for a new beast: tag the creature with the scoreboard tag {@code nusmp_will_offer:<will id>} (and optionally {@code nusmp_will_rank:<C|B|A|S|SS>} and a custom name) and a SNEAKING
 * right-click with an empty main hand opens the offer; the offer is remembered per creature and player ({@code offer:<creature uuid>:<player uuid>:<will>}), so one beast gives one will to one player once.
 */
public final class BeastWillApi {
    private BeastWillApi() {}

    public static final String OFFER_TAG = "nusmp_will_offer:", RANK_TAG = "nusmp_will_rank:";

    private static final Map<UUID, Long> LAST = new ConcurrentHashMap<>();

    /** Shows the preview of a direct inheritance to the player. {@code key} identifies the offer (it is used up when the will is taken); empty = one-off. False when the will does not exist. */
    public static boolean offer(ServerPlayer p, String willId, String beastName, BeastRank rank, String key) {
        BeastWillDef def = BeastWillCatalog.get(willId);
        if (def == null) return false;
        return BeastWillAcquire.open(p, willId, beastName, rank == null ? def.minRank() : rank, BeastWillAcquire.OFFER, key == null ? "" : key);
    }

    /** Gives the will with no check at all (an operator command or a quest reward the caller has already earned). True when it was bound. */
    public static boolean grant(ServerPlayer p, String willId) {
        BeastWillDef def = BeastWillCatalog.get(willId);
        return def != null && BeastWillConfig.enabled() && BeastWillAcquire.grant(p, def, true);
    }

    // ------------------------------------------------------------------ the tagged creature
    public static void onInteract(PlayerInteractEvent.EntityInteract e) {
        if (e.getHand() != InteractionHand.MAIN_HAND || e.getLevel().isClientSide || !(e.getEntity() instanceof ServerPlayer p) || !p.isShiftKeyDown() || !p.getMainHandItem().isEmpty()) return;
        if (!(e.getTarget() instanceof LivingEntity beast) || beast.getTags().isEmpty() || !BeastWillConfig.enabled()) return;
        String willId = null;
        BeastRank rank = null;
        for (String t : beast.getTags()) {
            if (t.startsWith(OFFER_TAG)) willId = t.substring(OFFER_TAG.length());
            else if (t.startsWith(RANK_TAG)) rank = BeastRank.byName(t.substring(RANK_TAG.length()));
        }
        if (willId == null) return;
        long now = p.level().getGameTime();
        Long last = LAST.get(p.getUUID());
        e.setCancellationResult(InteractionResult.SUCCESS);
        e.setCanceled(true);
        if (last != null && now - last < 20 && now >= last) return;
        LAST.put(p.getUUID(), now);
        String name = beast.hasCustomName() ? beast.getCustomName().getString() : beast.getType().getDescription().getString();
        offer(p, willId, name.length() > 60 ? name.substring(0, 60) : name, rank, "offer:" + beast.getUUID() + ":" + p.getUUID() + ":" + willId);
    }

    static void forget(UUID player) { LAST.remove(player); }
}
