package com.newuniverse.nusmp.relics.djinn;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.boss.rules.BossIdentity;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The canon Belial's SEVER hit, the one place every Belial blow goes through (the weapon's spirit_sever and exile acts, the Extreme Magic Belior Zauto). The scythe cuts the
 * target's spirit, not its body, so the hit goes through the {@code anime_multiverse:spirit_sever} damage type (tagged into {@code bypasses_armor, _enchantments, _effects,
 * _resistance, _shield, _cooldown} and {@code _invulnerability}: armour, enchantments, Resistance, shields, the hit cooldown and the invulnerable flag do not apply, and the mod's
 * own wards, which read those tags, step aside) and, if a listener of another mod still cancels or eats it, the missing part is taken straight off the health (a sever is not a
 * "magic" or "physical" blow, so no nullification, negation or magic immunity has anything to nullify). It is "broken" against defences and never against the balance of the
 * game: the amount is {@link BelialSeverRules}' (power plus a fraction of max health, much less against players and bosses), capped per hit and per second per target, and
 * it goes through {@link BalanceLaw#cap} (the PvP hit cap and the damage-scale ceiling) like every other hit of the mod.
 *
 * <p>Not covered, on purpose: a Totem of Undying does not stop a sever (the type bypasses invulnerability) but a boss takes at most 3% of its health per hit and 6% per second;
 * a target that is exiled ({@link BelialExile}) is out of reach, and a creative or spectator player is never hit.
 * <p>Cost: one registry lookup, one hurt and a map lookup per target; the per-target window table is swept every 40 ticks and capped at {@link BelialSeverRules#MAX_WINDOWS}.
 * Server thread only; keyed by target UUID.
 */
public final class BelialSever {
    private BelialSever() {}

    public static final ResourceKey<DamageType> TYPE = ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath("anime_multiverse", "spirit_sever"));

    private static final Map<UUID, BelialSeverRules.Window> WINDOWS = new HashMap<>();
    private static int depth;

    /** True while a sever hit is being dealt (other listeners may read it to step aside). */
    public static boolean active() { return depth > 0; }

    /** The kind of a target for the rules: a boss (this mod's or a big one), a player, or a mob. */
    public static BelialSeverRules.Kind kindOf(LivingEntity t) {
        if (BalanceLaw.isBoss(t) || BossIdentity.isBoss(t)) return BelialSeverRules.Kind.BOSS;
        return t instanceof Player ? BelialSeverRules.Kind.PLAYER : BelialSeverRules.Kind.MOB;
    }

    /** The damage source of a sever: the spirit-sever type with the caster as the cause (plain indirect magic if the data entry is missing). */
    static DamageSource source(ServerPlayer p) {
        var reg = p.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);
        Holder<DamageType> h = reg.getHolder(TYPE).orElse(null);
        return h != null ? new DamageSource(h, p, p) : p.damageSources().indirectMagic(p, p);
    }

    /**
     * Severs {@code t}: {@code power} plus {@code frac} of its max health, softened and capped by {@link BelialSeverRules}. Returns what was taken (0 if the target is not a valid foe,
     * is exiled, or has used up its second).
     */
    public static float sever(ServerPlayer p, LivingEntity t, double power, double frac) {
        if (p == null || t == null || t == p || p.level().isClientSide || !t.isAlive() || t.isAlliedTo(p)) return 0f;
        if (t instanceof Player o && (o.isCreative() || o.isSpectator())) return 0f;
        if (BelialExile.isExiled(t.getUUID())) return 0f;
        BelialSeverRules.Kind k = kindOf(t);
        double max = t.getMaxHealth();
        double want = BelialSeverRules.hit(k, power, max, frac);
        want = BalanceLaw.cap(t, (float) want);                                                  // the PvP hit cap and the damage-scale ceiling still apply
        long now = t.level().getGameTime();
        BelialSeverRules.Window w = WINDOWS.computeIfAbsent(t.getUUID(), id -> new BelialSeverRules.Window());
        double granted = w.grant(now, want, BelialSeverRules.secondCap(k, max));
        if (granted < BelialSeverRules.MIN_HIT) return 0f;
        float amount = (float) granted;
        DamageSource src = source(p);
        float before = t.getHealth() + t.getAbsorptionAmount();
        t.invulnerableTime = 0;
        depth++;
        try {
            t.hurt(src, amount);
        } finally {
            depth--;
        }
        float lost = before - (t.getHealth() + t.getAbsorptionAmount());
        if (t.isAlive() && lost < amount * 0.1f) direct(p, t, amount - Math.max(0f, lost), src);   // a listener of another mod ate the whole blow: the spirit is cut anyway
        else if (!t.isAlive()) lost = Math.max(lost, amount);
        t.setLastHurtByMob(p);
        return Math.max(lost, 0f);
    }

    /** Takes {@code amount} straight off the health, absorption ignored, and ends the target's life the way a blow would if it reaches zero. */
    static void direct(ServerPlayer p, LivingEntity t, float amount, DamageSource src) {
        if (amount <= 0f || !t.isAlive()) return;
        float hp = t.getHealth() - amount;
        t.setHealth(Math.max(0f, hp));
        t.hurtMarked = true;
        if (t.getHealth() <= 0f && !t.isRemoved()) t.die(src);
    }

    /** Every few ticks from the server tick: forgets the windows of the past. */
    static void sweep(long now) { BelialSeverRules.sweep(WINDOWS, now); }

    /** Forgets everything (the server stops). */
    static void clearAll() { WINDOWS.clear(); }

    static int windows() { return WINDOWS.size(); }
}
