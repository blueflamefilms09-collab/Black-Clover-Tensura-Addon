package com.newuniverse.nusmp.entity.veldanava.skills;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.item.DemonSlayerSwordItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Why: every damaging press of the Judgment skills ends in one of a few acts (a true hit, an execution, stripping the good effects, marking a creature so it cannot heal, a Judgment Sigil),
 * and the rules around them (the Balance Law's per-hit limits, the player and boss settings, the one flag that tells the passives "this hit is already an ability") belong in one place.
 * Server thread only.
 *
 * <p>A true hit is the mod's {@code nusmp:conceptual_severance} damage: it bypasses armour, enchantments, effects, shields and the invulnerability frames. It is still an ordinary
 * {@code hurt} call, so it goes through the Balance Law first (a player takes at most the per-hit share of their maximum health) and through every other mod's listeners; only
 * Veldanava's own Michael barrier steps aside while {@link #trueActive()} is set.
 */
public final class JudgmentDamage {
    private JudgmentDamage() {}

    static final String NO_HEAL_UNTIL = "nusmp_no_heal_until", SIGIL_BY = "nusmp_sigil_by", SIGIL_UNTIL = "nusmp_sigil_until", OWNER_SIGIL = "nusmp_sigil_target";

    private static boolean trueActive;

    /** True while a true hit of this class is being dealt (the passives and the Michael barrier read it so an ability is not counted twice). */
    public static boolean trueActive() { return trueActive; }

    // ---------------------------------------------------------------- hits
    /** A true hit of exactly {@code amount} (no Balance Law). The caller has already limited it. Returns whether it landed. */
    public static boolean raw(LivingEntity owner, LivingEntity target, float amount) {
        if (owner.level().isClientSide || amount <= 0f || !target.isAlive()) return false;
        boolean before = trueActive;
        trueActive = true;
        try {
            target.invulnerableTime = 0;
            return target.hurt(DemonSlayerSwordItem.severance(owner), amount);
        } finally {
            trueActive = before;
        }
    }

    /** A true hit scaled the way every addon spell is (EP of the caster, Balance Law caps) and limited by the config's cap for one press. */
    public static boolean hit(LivingEntity owner, LivingEntity target, float baseDamage, double masteryFraction) {
        float scaled = BalanceLaw.damage(owner, target, baseDamage, masteryFraction);
        return raw(owner, target, (float) Math.min(JudgmentConfig.activeCap(), scaled));
    }

    /** A true hit worth a share of the target's maximum health, limited by the cap for one press and the Balance Law's cap on the target. */
    public static boolean share(LivingEntity owner, LivingEntity target, double share) {
        double want = Math.min(JudgmentConfig.activeCap(), target.getMaxHealth() * share);
        return raw(owner, target, BalanceLaw.cap(target, (float) want));
    }

    /** The detonation of Final Judgment: {@code damage} limited by the config's cap and the Balance Law's cap on the target. */
    public static boolean detonation(LivingEntity owner, LivingEntity target, double damage) {
        double want = Math.min(JudgmentConfig.finalCap(), damage);
        return raw(owner, target, BalanceLaw.cap(target, (float) want));
    }

    // ---------------------------------------------------------------- execution
    /** Whether an execution may kill this creature outright under the server's settings. */
    public static boolean mayExecute(LivingEntity target) {
        if (target instanceof Player) return JudgmentConfig.executePlayers();
        if (BalanceLaw.isBoss(target)) return JudgmentConfig.executeBosses();
        return true;
    }

    /**
     * Executes a creature: it dies at once, a Totem of Undying cannot stop it, and the kill is the owner's (experience and drops). When the settings forbid an outright kill of this
     * creature (a player, a boss) it takes a hit instead: the Balance Law's share for a player, 10% of its maximum health for a boss. Returns true when the creature is dead afterwards.
     */
    public static boolean execute(LivingEntity owner, LivingEntity target) {
        if (owner.level().isClientSide || !target.isAlive()) return false;
        if (!mayExecute(target)) {
            if (target instanceof Player) hit(owner, target, 400f, 1.0);
            else share(owner, target, 0.10);
            return !target.isAlive();
        }
        boolean before = trueActive;
        trueActive = true;
        try {
            target.invulnerableTime = 0;
            DamageSource kill = owner.damageSources().source(DamageTypes.GENERIC_KILL, owner);
            target.hurt(kill, Float.MAX_VALUE);
        } finally {
            trueActive = before;
        }
        return !target.isAlive();
    }

    // ---------------------------------------------------------------- not healing
    /** The creature cannot heal for {@code ticks} (the longer of this and what it already has). */
    public static void markNoHeal(LivingEntity e, int ticks) {
        if (e.level().isClientSide) return;
        CompoundTag d = e.getPersistentData();
        long until = e.level().getGameTime() + ticks;
        if (until > d.getLong(NO_HEAL_UNTIL)) d.putLong(NO_HEAL_UNTIL, until);
    }

    public static boolean noHeal(LivingEntity e) {
        CompoundTag d = e.getPersistentData();
        return d.contains(NO_HEAL_UNTIL) && d.getLong(NO_HEAL_UNTIL) > e.level().getGameTime();
    }

    // ---------------------------------------------------------------- the good effects
    /** The good effects of a creature, as they are now (a copy, so the caller can remove while it walks). */
    public static List<MobEffectInstance> beneficial(LivingEntity e) {
        List<MobEffectInstance> out = new ArrayList<>();
        for (MobEffectInstance i : e.getActiveEffects()) if (i.getEffect().value().getCategory() == MobEffectCategory.BENEFICIAL) out.add(i);
        return out;
    }

    /** Removes the good effects of a creature, at most {@code max}. Returns how many were removed. */
    public static int stripBeneficial(LivingEntity e, int max) {
        int n = 0;
        for (MobEffectInstance i : beneficial(e)) {
            if (n >= max) break;
            if (e.removeEffect(i.getEffect())) n++;
        }
        return n;
    }

    /** The good effects of {@code from}, listed by effect (used by the stealing skills that follow). */
    public static List<net.minecraft.core.Holder<MobEffect>> beneficialHolders(LivingEntity from) {
        List<net.minecraft.core.Holder<MobEffect>> out = new ArrayList<>();
        for (MobEffectInstance i : beneficial(from)) out.add(i.getEffect());
        return out;
    }

    // ---------------------------------------------------------------- the Judgment Sigil
    /** Marks {@code target} with the owner's Judgment Sigil (one sigil per owner: the old one is forgotten). */
    public static void mark(ServerPlayer owner, LivingEntity target) {
        long now = owner.level().getGameTime();
        owner.getPersistentData().putUUID(OWNER_SIGIL, target.getUUID());
        CompoundTag t = target.getPersistentData();
        t.putUUID(SIGIL_BY, owner.getUUID());
        t.putLong(SIGIL_UNTIL, now + JudgmentRules.SIGIL_TICKS);
    }

    /** The online owner whose sigil is still on this creature, or null (no sigil, expired, owner gone or the owner marked someone else since). */
    public static ServerPlayer sigilOwner(MinecraftServer server, LivingEntity target) {
        CompoundTag t = target.getPersistentData();
        if (!t.hasUUID(SIGIL_BY)) return null;
        if (t.getLong(SIGIL_UNTIL) <= target.level().getGameTime()) { clearSigil(target); return null; }
        UUID by = t.getUUID(SIGIL_BY);
        ServerPlayer owner = server.getPlayerList().getPlayer(by);
        if (owner == null || !owner.isAlive()) return null;
        CompoundTag od = owner.getPersistentData();
        if (!od.hasUUID(OWNER_SIGIL) || !od.getUUID(OWNER_SIGIL).equals(target.getUUID())) { clearSigil(target); return null; }
        return owner;
    }

    public static boolean hasSigil(LivingEntity target) { return target.getPersistentData().hasUUID(SIGIL_BY); }

    public static void clearSigil(LivingEntity target) {
        CompoundTag t = target.getPersistentData();
        t.remove(SIGIL_BY);
        t.remove(SIGIL_UNTIL);
    }
}
