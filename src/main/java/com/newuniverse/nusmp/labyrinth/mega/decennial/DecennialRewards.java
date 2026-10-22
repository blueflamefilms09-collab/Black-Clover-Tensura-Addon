package com.newuniverse.nusmp.labyrinth.mega.decennial;

import com.newuniverse.nusmp.labyrinth.mega.guard.LabyrinthGuardianEntity;
import com.newuniverse.nusmp.relics.GradeDamageItems;
import com.newuniverse.nusmp.relics.RelicCatalog;
import com.newuniverse.nusmp.relics.RelicDef;
import com.newuniverse.nusmp.relics.Relics;
import com.newuniverse.nusmp.relics.order.UniqueLedger;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Why: the game side of the loot hook. {@link DecennialLoot} decides WHICH item a boss pays (a pure ranking); this class turns the real relic catalog into the candidates it ranks, asks the Order's
 * ledger for the item (a Unique-or-higher relic exists once in the world, so the ledger issues it and refuses a second copy), drops it at the corpse where nothing can burn or despawn it, tells the
 * party, and remembers that the hall has paid. It is the second owner-requested exception to "Unique-or-higher relics come only from Crucibles" (the first is the Metal Vessel of the Holy
 * Guardians) and it is switched by the config {@code bossRelic}. A boss an admin called pays nothing, the same rule the Metal Vessel follows, and a boss whose grade has no relic left pays nothing
 * and tells the killer so (never an exception, never a duplicate).
 */
final class DecennialRewards {
    private DecennialRewards() {}

    /** The relics of the catalog as candidates: what may be paid at all (no consumable, no half of a pair) with its kind, weapon type and grade. */
    static List<DecennialLoot.Candidate> catalog() {
        List<DecennialLoot.Candidate> out = new ArrayList<>();
        for (RelicDef d : Relics.all()) {
            if (d.isPairHalf() || !d.grade().rollable()) continue;                       // EX-Singularity items are never paid by a roll
            String kind = DecennialLoot.kindOfSlot(d.slot().name());
            if (kind.isEmpty()) continue;
            out.add(new DecennialLoot.Candidate(d.id(), d.cardName(), kind, kind.equals(DecennialLoot.WEAPON) ? GradeDamageItems.weaponTypeOf(d) : "", d.grade().ordinal()));
        }
        return out;
    }

    /** Pays the boss's item for a death (the caller has checked the switch and that the boss was not called by an admin). Never throws. */
    static void award(LabyrinthGuardianEntity g, ServerLevel sl, DamageSource source, DecennialProfile p, String siteKey) {
        try {
            if (!DecennialConfig.get(DecennialConfig.BOSS_RELIC)) return;
            MinecraftServer server = sl.getServer();
            if (server == null) return;
            DecennialData data = DecennialData.get(server);
            String memory = siteKey.isEmpty() ? "l" + p.level() : siteKey;
            if (data.paid(memory)) return;
            ServerPlayer killer = killerOf(g, source, sl);
            if (killer == null) return;                                                   // nobody to credit: the hall keeps its prize for a death that has a killer
            Set<String> taken = new HashSet<>(UniqueLedger.liveIds(server));
            List<DecennialLoot.Candidate> ranked = DecennialLoot.rank(p, catalog(), taken);
            for (DecennialLoot.Candidate c : ranked) {
                RelicDef def = Relics.byId(c.id());
                if (def == null) continue;
                ItemStack stack = RelicCatalog.newStack(def);
                if (stack.isEmpty() || !UniqueLedger.issue(sl, def.id(), stack)) continue;
                ItemEntity drop = new ItemEntity(sl, g.getX(), g.getY() + 0.3, g.getZ(), stack);
                drop.setUnlimitedLifetime();
                drop.setInvulnerable(true);
                drop.setGlowingTag(true);
                sl.addFreshEntity(drop);
                data.claimLoot(memory);
                Component line = Component.translatableWithFallback("message.nusmp.mega.decennial.loot", "%1$s lets go of %2$s.", g.getName(), stack.getHoverName()).withStyle(ChatFormatting.GOLD);
                for (ServerPlayer q : sl.players()) if (q.distanceToSqr(g) <= 64.0 * 64.0) q.sendSystemMessage(line);
                return;
            }
            killer.sendSystemMessage(Component.translatableWithFallback("message.nusmp.mega.decennial.loot_none", "The boss has no %1$s item left to give: every one is already in the world.", p.loot().grade())
                    .withStyle(ChatFormatting.GRAY));
        } catch (RuntimeException e) {
            com.mojang.logging.LogUtils.getLogger().warn("[nusmp] The loot of a Decennial boss could not be paid: {}", e.toString());
        }
    }

    /** The player a kill is credited to: the attacker, the credited player, else the nearest living player within 64 blocks. */
    private static ServerPlayer killerOf(LabyrinthGuardianEntity dead, DamageSource source, ServerLevel sl) {
        Entity cause = source == null ? null : source.getEntity();
        if (cause instanceof ServerPlayer p) return p;
        if (dead.getKillCredit() instanceof ServerPlayer credited) return credited;
        ServerPlayer best = null;
        double bestD = 64.0 * 64.0;
        for (ServerPlayer q : sl.players()) {
            if (q.isSpectator() || !q.isAlive()) continue;
            double d = q.distanceToSqr(dead);
            if (d <= bestD) {
                best = q;
                bestD = d;
            }
        }
        return best;
    }
}
