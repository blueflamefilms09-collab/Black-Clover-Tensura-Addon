package com.newuniverse.nusmp.balance;

import com.newuniverse.nusmp.NUConfig;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.Tags;

/**
 * The balance law shared by every addon spell. 0.48 (the owner's call): addon magic stands level with Tensura's own magic and
 * skills, or above it, instead of being a side-grade.
 * - Damage scales with the caster's EP the way Tensura's does ({@link #epScale}: x1 at 1,000 EP up to x11 at 10M), plus a sliver of
 *   the target's max health so huge Tensura bodies still feel it. Against players one hit takes at most 40% of their max health
 *   (pvpHitCapPercent / gamerule nusmpPvpHitCapPercent): no one-shots. Mobs: x config multiplier.
 * - Costs: percent of max magicule with a flat floor (big pools can't spam).
 * - Hard control: one shared 12 s cooldown per caster, diminishing returns per target, short caps.
 * - Healing: 50% max-health cap per cast, 40% effectiveness if healed in the last 20 s.
 */
public final class BalanceLaw {
    private BalanceLaw() {}

    // ------------------------------------------------------------------ damage
    /**
     * 0.48: how hard the caster's existence hits, from their EP like Tensura's own scaling: x1 at 1,000 EP, x3.5 at 10k, x6 at 100k,
     * x8.5 at 1M, x11 at 10M (at most x12).
     */
    public static double epScale(LivingEntity caster) {
        double ep = 0;
        try { ep = EnergyHelper.getMaxEP(caster); } catch (Throwable ignored) {}
        return Math.min(12, 1 + Math.max(0, Math.log10(Math.max(1, ep)) - 3) * 2.5);
    }

    /**
     * Damage of an addon spell or weapon technique.
     * @param caster         who casts it (its EP scales the hit)
     * @param raw            the spell's base damage (HP)
     * @param masteryFrac    0..1 mastery of the casting skill
     */
    public static float damage(LivingEntity caster, LivingEntity target, float raw, double masteryFrac) {
        double m = Math.max(0, Math.min(1, masteryFrac));
        double r = raw * epScale(caster) * (0.85 + 0.3 * m) * com.newuniverse.nusmp.NUGameRules.spellDamage(target.level());
        r += target.getMaxHealth() * 0.01 * raw / 10.0;                      // a sliver of the target's max health
        return cap(target, (float) r);
    }

    /** The same without a known caster (old call sites): mastery scaling only, then the same caps. */
    public static float damage(LivingEntity target, float raw, double masteryFrac) {
        double m = Math.max(0, Math.min(1, masteryFrac));
        return cap(target, (float) (raw * (1 + m) * com.newuniverse.nusmp.NUGameRules.spellDamage(target.level())));
    }

    /** Players: at most pvpHitCap of their max health per hit. Mobs: the config multiplier. */
    public static float cap(LivingEntity target, float r) {
        if (target instanceof Player) return (float) Math.min(r, target.getMaxHealth() * com.newuniverse.nusmp.NUGameRules.pvpHitCap(target.level()));
        return (float) (r * NUConfig.BAL_MOB_DAMAGE_MULT.get());
    }

    public static boolean isBoss(LivingEntity e) {
        return e.getType().is(Tags.EntityTypes.BOSSES) || e.getMaxHealth() >= 300;
    }

    // ------------------------------------------------------------------ cost
    /** Magicule cost: percent of max magicule, never below the floor. */
    public static double cost(LivingEntity caster, double percent, double floor) {
        return Math.max(floor, EnergyHelper.getMaxMagicule(caster) * percent / 100.0) * NUConfig.DM_COST_MULT.get();
    }

    // ------------------------------------------------------------------ hard control
    private static final String ICD = "nusmp_hc_until", DR_T = "nusmp_dr_time", DR_N = "nusmp_dr_count";
    public static final int CONTROL_ICD_TICKS = 240;   // 12 s shared
    public static final int DR_WINDOW_TICKS = 600;     // 30 s

    /** True and starts the shared cooldown if the caster may apply hard control now. */
    public static boolean beginControl(LivingEntity caster) {
        CompoundTag d = caster.getPersistentData();
        long now = caster.level().getGameTime();
        if (now < d.getLong(ICD)) return false;
        d.putLong(ICD, now + CONTROL_ICD_TICKS);
        return true;
    }

    public static int controlCooldownLeft(LivingEntity caster) {
        return (int) Math.max(0, caster.getPersistentData().getLong(ICD) - caster.level().getGameTime());
    }

    /**
     * Final control duration for this target: capped (3 s players, 5 s mobs, 1 s bosses) and
     * halved / quartered when re-applied within 30 s.
     */
    public static int controlTicks(LivingEntity target, int baseTicks) {
        int cap = isBoss(target) ? 20 : target instanceof Player ? 60 : 100;
        int ticks = Math.min(baseTicks, cap);
        CompoundTag d = target.getPersistentData();
        long now = target.level().getGameTime();
        int n = now - d.getLong(DR_T) < DR_WINDOW_TICKS ? d.getInt(DR_N) + 1 : 0;
        d.putLong(DR_T, now);
        d.putInt(DR_N, n);
        return Math.max(5, ticks >> Math.min(n, 2));
    }

    // ------------------------------------------------------------------ healing
    private static final String HEAL_T = "nusmp_healed_at";

    /** Heals with the 50% cap and the 20-second decay. Returns the amount healed. */
    public static float heal(LivingEntity target, float amount) {
        long now = target.level().getGameTime();
        CompoundTag d = target.getPersistentData();
        if (now - d.getLong(HEAL_T) < 400) amount *= 0.4f;
        amount = Math.min(amount, target.getMaxHealth() * 0.5f);
        d.putLong(HEAL_T, now);
        float before = target.getHealth();
        target.heal(amount);
        return target.getHealth() - before;
    }
}
