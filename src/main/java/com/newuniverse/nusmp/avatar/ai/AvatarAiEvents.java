package com.newuniverse.nusmp.avatar.ai;

import com.newuniverse.nusmp.avatar.AvatarAiConfig;
import com.newuniverse.nusmp.avatar.AvatarChatterEngine;
import com.newuniverse.nusmp.avatar.AvatarEntity;
import com.newuniverse.nusmp.avatar.AvatarPersonality;
import com.newuniverse.nusmp.avatar.ChatEvent;
import com.newuniverse.nusmp.avatar.FtbTeamHelper;
import com.newuniverse.nusmp.avatar.cast.SimulatedPlayerCaster;
import com.newuniverse.nusmp.avatar.yandere.YandereFrenzyHandler;
import com.newuniverse.nusmp.avatar.yandere.YandereMath;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.List;

/**
 * Why: the few places where the Avatar's behaviour must hook the rest of the game rather than live in a goal: the creator being hurt (frenzy and
 * intercepting), an Avatar picking a target (the COMBAT_START line, and never targeting its own creator or team), the damage of its simulated caster
 * (made the Avatar's own damage, and kept away from friends), and clean-up when an Avatar leaves the world. Every handler ignores the client and is
 * wrapped so a bug here can never break combat for everyone else.
 */
public final class AvatarAiEvents {
    private AvatarAiEvents() {}

    /** True while a double's hit is being re-dealt as the Avatar's hit, so the re-dealt hit is not redirected again. */
    private static boolean redirecting;

    // ---------------------------------------------------------------- the creator is hurt: frenzy
    public static void onDamagePost(LivingDamageEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer creator) || e.getNewDamage() <= 0) return;
        try {
            for (AvatarEntity a : avatarsOf(creator, 48, true)) {
                Entity culprit = e.getSource().getEntity();
                if (culprit instanceof AvatarEntity other && creator.getUUID().equals(other.creatorUuid())) continue;      // friendly fire is no cause for rage
                YandereFrenzyHandler.onCreatorHurt(a, e.getSource(), e.getNewDamage());
            }
        } catch (RuntimeException soft) { /* never break damage handling */ }
    }

    /** Avatars of this creator within {@code radius}, optionally only the Yandere ones. */
    static List<AvatarEntity> avatarsOf(ServerPlayer creator, double radius, boolean yandereOnly) {
        return creator.serverLevel().getEntitiesOfClass(AvatarEntity.class, creator.getBoundingBox().inflate(radius),
                a -> a.isAlive() && AvatarGates.loyal(a) && creator.getUUID().equals(a.creatorUuid()) && (!yandereOnly || a.has(AvatarPersonality.YANDERE)));
    }

    // ---------------------------------------------------------------- incoming damage
    public static void onIncoming(LivingIncomingDamageEvent e) {
        try {
            DamageSource src = e.getSource();
            LivingEntity victim = e.getEntity();
            if (victim.level().isClientSide) return;
            if (src.getEntity() instanceof SimulatedPlayerCaster sc) { redirect(e, sc); return; }
            if (src.getEntity() instanceof AvatarEntity av && victim instanceof Player p && av.has(AvatarPersonality.YANDERE) && !AvatarAiConfig.yandereLethal())
                e.setAmount(YandereMath.nonLethal(e.getAmount(), p.getHealth(), p.getAbsorptionAmount()));
            if (victim instanceof ServerPlayer creator) intercept(e, creator);
        } catch (RuntimeException soft) { /* never break damage handling */ }
    }

    /** A spell cast by a body double: it is the Avatar who hits, and friends are never hit. */
    private static void redirect(LivingIncomingDamageEvent e, SimulatedPlayerCaster sc) {
        if (redirecting) return;
        LivingEntity victim = e.getEntity();
        e.setCanceled(true);
        if (!(victim.level() instanceof ServerLevel sl) || !(sl.getEntity(sc.avatarId()) instanceof AvatarEntity avatar)) return;
        if (!mayHit(avatar, victim)) return;
        DamageSource src = e.getSource();
        Entity direct = src.getDirectEntity() == sc || src.getDirectEntity() == null ? avatar : src.getDirectEntity();
        redirecting = true;
        try { victim.hurt(new DamageSource(src.typeHolder(), direct, avatar), e.getAmount()); }
        finally { redirecting = false; }
    }

    /**
     * May this Avatar's spell hurt this entity? Not the Avatar itself, its creator, the creator's team, the creator's other Avatars and pets, nor
     * a villager (unless it is the Avatar's current target; a non-Yandere Avatar that deliberately targets someone, e.g. after a betrayal, may hit them): area pages hit "everyone but the caster", so the guard is here.
     */
    public static boolean mayHit(AvatarEntity avatar, Entity victim) {
        if (victim == avatar || victim == null) return false;
        if (victim == avatar.getTarget() && !avatar.has(AvatarPersonality.YANDERE)) return true;       // a deliberate target (a betraying Avatar's creator) is fair game
        if (victim.getUUID().equals(avatar.creatorUuid())) return false;
        if (victim instanceof AvatarEntity other && avatar.creatorUuid() != null && avatar.creatorUuid().equals(other.creatorUuid())) return false;
        if (victim instanceof OwnableEntity o && avatar.creatorUuid() != null && avatar.creatorUuid().equals(o.getOwnerUUID())) return false;
        if (victim instanceof LivingEntity l && FtbTeamHelper.isSquadmate(avatar, l)) return false;
        return !(victim instanceof AbstractVillager) || avatar.getTarget() == victim;
    }

    /** A Yandere Avatar steps into a hit on its creator and takes {@code yandereInterceptShare} of it (never dying of it). */
    private static void intercept(LivingIncomingDamageEvent e, ServerPlayer creator) {
        DamageSource src = e.getSource();
        if (src.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || e.getAmount() <= 0) return;
        double share = AvatarAiConfig.interceptShare();
        if (share <= 0) return;
        for (AvatarEntity a : avatarsOf(creator, 12, true)) {
            if (src.getEntity() == a) continue;
            float taken = YandereMath.interceptedPart(e.getAmount(), share, a.getHealth());
            if (taken <= 0f) continue;
            e.setAmount(e.getAmount() - taken);
            a.hurt(src, taken);
            return;
        }
    }

    // ---------------------------------------------------------------- targets
    public static void onChangeTarget(LivingChangeTargetEvent e) {
        if (!(e.getEntity() instanceof AvatarEntity avatar) || avatar.level().isClientSide) return;
        try {
            LivingEntity next = e.getNewAboutToBeSetTarget();
            if (next == null) return;
            // only a Yandere is hard-wired never to turn on its own side; other Avatars may betray (the outcome engine decides that, not this hook)
            if (avatar.has(AvatarPersonality.YANDERE) && (next.getUUID().equals(avatar.creatorUuid()) || FtbTeamHelper.isSquadmate(avatar, next)
                    || next instanceof AvatarEntity other && avatar.creatorUuid() != null && avatar.creatorUuid().equals(other.creatorUuid()))) {
                e.setCanceled(true);
                return;
            }
            if (avatar.getTarget() == null) AvatarChatterEngine.say(avatar, ChatEvent.COMBAT_START, next.getDisplayName().getString());
        } catch (RuntimeException soft) { /* never break targeting */ }
    }

    // ---------------------------------------------------------------- the frenzy clock
    /** Counts every Avatar's Yandere frenzy down. An entity tick (not a goal) so it also runs for an Avatar whose goals were rebuilt or removed. */
    public static void onEntityTick(EntityTickEvent.Post e) {
        if (e.getEntity() instanceof AvatarEntity a && !a.level().isClientSide) {
            try { YandereFrenzyHandler.tick(a); } catch (RuntimeException soft) { /* the clock must never break the tick */ }
        }
    }

    // ---------------------------------------------------------------- clean-up
    public static void onLeave(EntityLeaveLevelEvent e) {
        if (e.getEntity() instanceof AvatarEntity a && !e.getLevel().isClientSide) {
            SimulatedPlayerCaster.forget(a.getUUID());
            AvatarChatterEngine.forget(a);
        }
    }

    public static void onServerStopped(ServerStoppedEvent e) { SimulatedPlayerCaster.forgetAll(); }
}
