package com.newuniverse.nusmp.manacore;

import com.newuniverse.nusmp.avatar.TensuraBridge;
import com.newuniverse.nusmp.boss.rules.BossIdentity;
import com.newuniverse.nusmp.skilltree.loot.LootVictims;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

import java.util.List;

/**
 * Why: where the rank Mana Cores come from (3.51.0), kept to the same rules as the Beast Will cores: a wild, grown, ownerless creature killed by a player, one chance per kill and one core at most, a gap
 * between two drops of the same player ({@code dropGapMinutes}, default 20), the creature's rank worked out from its Existence Points (a creature Tensura does not know counts 100 EP per health point). A normal
 * creature gives a STANDARD core (or a BEAST core, half as often, from a non-humanoid beast); only a boss or a cleared pocket rift gives an ANCIENT one. The core lies on the ground with no despawn timer. Cost: one
 * rank lookup and one random roll per kill by a player; nothing per tick.
 */
public final class ManaCoreDrops {
    private ManaCoreDrops() {}

    private static final String GAP_KEY = "nusmp_manacore_drop_at";

    /** The Existence Points a creature counts as for a drop. */
    static double effectiveEp(LivingEntity v) { return TensuraBridge.isTensuraMob(v) ? Math.max(TensuraBridge.maxEp(v), 0.0) : v.getMaxHealth() * 100.0; }

    static boolean wild(LivingEntity v) {
        if (v instanceof Player) return false;
        if (v instanceof TamableAnimal t && (t.isTame() || t.getOwnerUUID() != null)) return false;
        if (TensuraBridge.permanentOwner(v) != null) return false;
        return v.tickCount >= 1200;
    }

    /** True when the player may get a drop now (and starts the gap if so). */
    private static boolean gapOk(ServerPlayer k) {
        long now = k.level().getGameTime(), last = k.getPersistentData().getLong(GAP_KEY);
        if (now >= last && now - last < ManaCoreConfig.dropGapTicks() && last != 0) return false;
        return true;
    }

    private static void give(ServerLevel level, ServerPlayer k, double x, double y, double z, ManaCoreRules.Core core, java.util.Collection<ItemEntity> into) {
        ItemStack stack = ManaCoreItems.stack(core);
        if (stack.isEmpty()) return;
        k.getPersistentData().putLong(GAP_KEY, level.getGameTime());
        ItemEntity drop = new ItemEntity(level, x, y, z, stack);
        drop.setUnlimitedLifetime();
        drop.setGlowingTag(true);
        drop.setPickUpDelay(20);
        if (into != null) into.add(drop);
        else level.addFreshEntity(drop);
        k.displayClientMessage(net.minecraft.network.chat.Component.translatable("manacore.msg.dropped", net.minecraft.network.chat.Component.literal(core.name())), true);
    }

    public static void onDrops(LivingDropsEvent e) {
        LivingEntity v = e.getEntity();
        if (v.level().isClientSide || !ManaCoreConfig.enabled() || ManaCoreConfig.dropScale() <= 0) return;
        ServerPlayer k = LootVictims.killer(e.getSource(), v);
        if (k == null || k == v || !wild(v)) return;
        int rank = ManaCoreRules.victimRank(effectiveEp(v));
        if (rank < 0 || !gapOk(k)) return;
        boolean boss = BossIdentity.isBoss(v);
        int variant = boss ? 2 : (v.getRandom().nextBoolean() && !(v instanceof net.minecraft.world.entity.npc.Npc) ? 1 : 0);
        double chance = ManaCoreRules.DROP[rank] * ManaCoreConfig.dropScale() * (variant == 1 ? 0.5 : boss ? 2.0 : 1.0);
        if (v.getRandom().nextDouble() >= chance) return;
        give((ServerLevel) v.level(), k, v.getX(), v.getY() + 0.5, v.getZ(), ManaCoreRules.of(rank, variant), e.getDrops());
    }

    /** A cleared pocket rift: a small chance of an ANCIENT core for each player in it, of a rank their Existence Points allow (C or better). */
    public static void riftCleared(ServerLevel level, List<ServerPlayer> players, double chance) {
        if (!ManaCoreConfig.enabled() || ManaCoreConfig.dropScale() <= 0 || chance <= 0) return;
        for (ServerPlayer k : players) {
            if (!gapOk(k) || level.getRandom().nextDouble() >= chance * ManaCoreConfig.dropScale()) continue;
            int top = 2;
            for (int r = 2; r < ManaCoreRules.RANKS.length; r++) if (ManaCoreRules.mayUse(r, TensuraBridge.ep(k), true)) top = r;
            give(level, k, k.getX(), k.getY() + 0.5, k.getZ(), ManaCoreRules.of(2 + level.getRandom().nextInt(top - 1), 2), null);
        }
    }
}
