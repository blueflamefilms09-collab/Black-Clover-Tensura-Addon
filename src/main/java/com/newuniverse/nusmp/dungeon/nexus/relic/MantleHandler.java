package com.newuniverse.nusmp.dungeon.nexus.relic;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.dungeon.wild.Wildlife;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * The Mantle of the Scavenger (docs/living_dungeon.md section 7). Worn as a cape and attuned, it registers the wearer as a Tier 1 Scavenger:
 * a creature of the roster of tier 3 (dungeon.wild.Wildlife, the tag roster) cannot take the wearer as its target, and one that already
 * has is made to forget them, twice a second in a bounded sweep. Its curse, Prey's Weakness: the wearer's physical blows do nothing to
 * tier 2 and 3 creatures (spells still do), and taking the mantle off brands the wearer with the Pheromone Mark ({@link Pheromone}): for
 * thirty seconds every roster predator of tier 2 and up within 48 blocks comes for them. The mark ends by itself, and on death, relog and
 * a change of dimension; while it lasts the mantle's ignoring is switched off.
 */
final class MantleHandler {
    private MantleHandler() {}

    private static final org.slf4j.Logger LOG = LogUtils.getLogger();
    /** The most creatures one sweep makes forget the wearer. */
    private static final int FORGET_PER_SWEEP = 32;
    private static final double IGNORE_RADIUS = 40.0;

    // ---------------------------------------------------------------- the ignoring
    /** A tier 3 creature is about to take the wearer as its target: it does not. */
    static void onChangeTarget(LivingChangeTargetEvent e) {
        if (!(e.getNewAboutToBeSetTarget() instanceof ServerPlayer p) || !(e.getEntity() instanceof Mob m)) return;
        int tier = Wildlife.tierOf(m);
        if (tier < MantleRules.IGNORED_FROM) return;
        long now = p.level().getGameTime();
        if (MantleRules.ignores(tier, RelicState.marked(p.getUUID(), now)) && Worn.has(p, Relic.MANTLE_OF_THE_SCAVENGER)) e.setCanceled(true);
    }

    /** Twice a second, for every wearer: the apex predators that still have them in their sights forget them. */
    static void sweepIgnore(ServerPlayer p) {
        if (!Worn.has(p, Relic.MANTLE_OF_THE_SCAVENGER) || RelicState.marked(p.getUUID(), p.level().getGameTime())) return;
        ServerLevel level = p.serverLevel();
        List<Mob> mobs = level.getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(IGNORE_RADIUS),
                m -> m.isAlive() && Wildlife.tierOf(m) >= MantleRules.IGNORED_FROM && (m.getTarget() == p || brainTarget(m) == p));
        int n = 0;
        for (Mob m : mobs) {
            if (n++ >= FORGET_PER_SWEEP) break;
            forget(m);
        }
    }

    private static LivingEntity brainTarget(Mob m) {
        try {
            return m.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null);
        } catch (RuntimeException | LinkageError otherBrain) {
            return null;
        }
    }

    private static void forget(Mob m) {
        try {
            m.setTarget(null);
            m.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
            m.getNavigation().stop();
        } catch (RuntimeException | LinkageError otherBrain) {
            // a brain of another shape: the plain target slot is already clear
        }
    }

    // ---------------------------------------------------------------- Prey's Weakness
    /** The wearer's physical blow on a tier 2 or 3 creature does nothing. */
    static void onOutgoing(LivingIncomingDamageEvent e) {
        if (!(e.getSource().getEntity() instanceof ServerPlayer p)) return;
        int tier = Wildlife.tierOf(e.getEntity());
        if (tier < MantleRules.NO_DAMAGE_FROM || !MantleRules.turnsAside(RelicRules.physical(e.getSource()), tier)) return;
        if (!Worn.has(p, Relic.MANTLE_OF_THE_SCAVENGER)) return;
        e.setCanceled(true);
        long now = p.level().getGameTime();
        if (RelicState.due(p.getUUID(), "mantle_weak", now, 60)) {
            p.displayClientMessage(Component.literal("Prey's Weakness: your blow does nothing to it. Use magic, or take the Mantle off.").withStyle(ChatFormatting.GRAY), true);
        }
    }

    // ---------------------------------------------------------------- taking it off: the Pheromone Mark
    /** Once a second per player: notices the Mantle coming off. */
    static void watch(ServerPlayer p) {
        UUID id = p.getUUID();
        boolean now = (Worn.mask(p, true) & Relic.MANTLE_OF_THE_SCAVENGER.bit()) != 0;
        Boolean was = RelicState.MANTLE.get(id);
        if (now) {
            RelicState.MANTLE.put(id, Boolean.TRUE);
            return;
        }
        RelicState.MANTLE.remove(id);
        if (was != null && was && p.isAlive() && !p.isSpectator()) startMark(p);
    }

    static void startMark(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        long now = level.getGameTime();
        RelicState.MARK.put(p.getUUID(), Pheromone.until(now));
        p.displayClientMessage(Component.literal("Pheromone Mark! Without the Mantle you reek of prey: every predator within " + Pheromone.RADIUS
                + " blocks is coming for you for 30 s. Run, or hide.").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD), true);
        level.playSound(null, p.blockPosition(), SoundEvents.WOLF_GROWL, SoundSource.HOSTILE, 1.6f, 0.6f);
        VfxSpawn.sendFollowing(level, VfxShape.KOTO_AURA, p, p.position().add(0, 0.2, 0), 0xFFB02030, 120, 1.5f);
        markSweep(p);
    }

    /** Every second while marked: predators in range are set on the wearer, nearest first, a bounded number at a time. */
    static void markSweep(ServerPlayer p) {
        UUID id = p.getUUID();
        ServerLevel level = p.serverLevel();
        long now = level.getGameTime();
        if (!RelicState.marked(id, now)) {
            RelicState.MARK.remove(id);
            return;
        }
        if (p.isCreative() || p.isSpectator()) return;
        List<Mob> near = level.getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(Pheromone.RADIUS),
                m -> m.isAlive() && Pheromone.aggroes(Wildlife.tierOf(m)) && m.getTarget() != p && !kept(m));
        near.sort(Comparator.comparingDouble(m -> m.distanceToSqr(p)));
        int n = 0;
        for (Mob m : near) {
            if (n++ >= Pheromone.MAX_PER_SWEEP) break;
            aim(m, p);
        }
        if (now % 100 < Pheromone.SWEEP) VfxSpawn.sendFollowing(level, VfxShape.KOTO_AURA, p, p.position().add(0, 0.2, 0), 0xFFB02030, 120, 1.5f);
    }

    /** Tamed, ridden or leashed creatures belong to somebody. */
    private static boolean kept(Entity e) {
        return (e instanceof TamableAnimal t && t.isTame()) || e.isVehicle() || e.isPassenger() || (e instanceof Mob mob && mob.isLeashed());
    }

    /** The form the Wild driver uses: the plain target slot and the brain's attack target. */
    private static void aim(Mob m, LivingEntity target) {
        try {
            m.setTarget(target);
            m.getBrain().setMemory(MemoryModuleType.ATTACK_TARGET, target);
        } catch (RuntimeException | LinkageError otherBrain) {
            LOG.debug("[nusmp] A creature could not be set on the marked player", otherBrain);
        }
    }

    // ---------------------------------------------------------------- the server tick (every 10 ticks)
    static void onServerTick(MinecraftServer server) {
        int tick = server.getTickCount();
        if (tick % 10 != 0) return;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (!p.isAlive()) continue;
            sweepIgnore(p);
            if (tick % Pheromone.SWEEP == 0 && RelicState.MARK.containsKey(p.getUUID())) markSweep(p);
        }
    }
}
