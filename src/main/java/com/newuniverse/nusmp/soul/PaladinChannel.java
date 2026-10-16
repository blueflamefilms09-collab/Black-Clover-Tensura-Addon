package com.newuniverse.nusmp.soul;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Why: the purification is a three-second channel, not an instant flip: the caster stands, a soul-washing light rises off the
 * creature, and the creature fights back for those three seconds, so damage to the caster, walking away, dying or the target dying
 * breaks it ({@link PaladinRules#interrupted}). The page casts once (the mana is spent then, so an interrupted or failed
 * purification costs the mana, as the owner asked), this class holds the one channel per caster, runs it from the server tick, and
 * when the time is up re-checks the rules on the creature as it is NOW (it may have become a boss's follower, or been healed) and
 * rolls once against {@link PaladinRules#chance}. Failure leaves the target with a short Weakness. Nothing here is saved: a channel
 * that is cut by a log-out or a restart simply never completes.
 */
public final class PaladinChannel {
    private PaladinChannel() {}

    private static final class Chan {
        final UUID target;
        final long start;
        final double mastery;
        final int cap;
        boolean hurt;

        Chan(UUID target, long start, double mastery, int cap) {
            this.target = target;
            this.start = start;
            this.mastery = mastery;
            this.cap = cap;
        }
    }

    private static final Map<UUID, Chan> ACTIVE = new HashMap<>();

    public static boolean active(UUID caster) { return ACTIVE.containsKey(caster); }

    /** Is someone already purifying this creature? */
    public static boolean targeted(UUID target) {
        for (Chan c : ACTIVE.values()) if (c.target.equals(target)) return true;
        return false;
    }

    /** Starts the channel. 'mastery' is 0..1; 'cap' the follower limit the caster will have if it takes. */
    public static void start(ServerPlayer caster, Mob target, double mastery, int cap) {
        ACTIVE.put(caster.getUUID(), new Chan(target.getUUID(), caster.level().getGameTime(), mastery, cap));
        if (caster.level() instanceof ServerLevel sl) PaladinFx.channelStart(sl, target);
        caster.displayClientMessage(Component.literal("Purifying the soul of " + target.getDisplayName().getString() + "...").withStyle(ChatFormatting.GOLD), true);
    }

    /** The caster took damage: the concentration breaks at the next tick. */
    public static void hurt(ServerPlayer caster) {
        Chan c = ACTIVE.get(caster.getUUID());
        if (c != null) c.hurt = true;
    }

    public static void cancel(UUID caster) { ACTIVE.remove(caster); }

    public static void clear() { ACTIVE.clear(); }

    /** Server tick: runs every channel one step. */
    static void tick(MinecraftServer server) {
        if (ACTIVE.isEmpty()) return;
        Iterator<Map.Entry<UUID, Chan>> it = ACTIVE.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Chan> en = it.next();
            Chan c = en.getValue();
            ServerPlayer caster = server.getPlayerList().getPlayer(en.getKey());
            if (caster == null) { it.remove(); continue; }
            Entity e = caster.serverLevel().getEntity(c.target);
            Mob target = e instanceof Mob m ? m : null;
            boolean sameLevel = target != null;
            double dist = target == null ? 0 : caster.distanceTo(target);
            if (PaladinRules.interrupted(dist, c.hurt, caster.isAlive(), target != null && target.isAlive(), sameLevel)) {
                it.remove();
                if (caster.isAlive()) caster.displayClientMessage(Component.literal("The purification is broken.").withStyle(ChatFormatting.RED), true);
                if (target != null) PaladinFx.failure(caster.serverLevel(), target);
                continue;
            }
            int t = (int) (caster.level().getGameTime() - c.start);
            if (t % 2 == 0) PaladinFx.channelTick(caster.serverLevel(), target, t);
            if (t >= PaladinRules.CHANNEL_TICKS) {
                it.remove();
                finish(caster, target, c);
            }
        }
    }

    private static void finish(ServerPlayer caster, Mob target, Chan c) {
        ServerLevel sl = caster.serverLevel();
        PaladinRules.Subject s = Paladins.subject(caster, target);
        PaladinRules.Verdict v = PaladinRules.verdict(s);
        if (v != PaladinRules.Verdict.OK) {
            caster.displayClientMessage(Component.literal(PaladinRules.refusal(v)).withStyle(ChatFormatting.RED), true);
            PaladinFx.failure(sl, target);
            return;
        }
        double chance = PaladinRules.chance(s.hpFraction(), s.targetEp(), s.casterEp(), s.resistance(), c.mastery, s.hostile());
        if (caster.getRandom().nextDouble() < chance && Paladins.convert(caster, target, c.cap)) {
            PaladinFx.success(sl, target);
        } else {
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, PaladinRules.failWeaknessTicks(c.mastery), 1));
            PaladinFx.failure(sl, target);
            caster.displayClientMessage(Component.literal("The soul resists.").withStyle(ChatFormatting.RED), true);
        }
    }
}
