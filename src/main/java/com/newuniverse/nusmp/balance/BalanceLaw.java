package com.newuniverse.nusmp.balance;

import com.newuniverse.nusmp.NUConfig;
import com.newuniverse.nusmp.skill.rules.UltimateClass;
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

    /** The EP scale taken in by the skill's mastery 0..1 (owner request: EP only fully counts once the skill is mastered; SERVER config epNeedsMastery, off = {@link #epScale(LivingEntity)}). */
    public static double epScale(LivingEntity caster, double masteryFrac) {
        return EpMastery.ramp(epScale(caster), masteryFrac, NUConfig.EP_NEEDS_MASTERY.get());
    }

    /**
     * 3.90: the late-game scaling ({@link LateGameScaling}) from a player caster's max magicule (mobs and bosses: x1), x1 to x6 by default (SERVER config balance lateGameScaling; off = 1). Applied once per hit, only in
     * {@link #damage(LivingEntity, LivingEntity, float, double)}; PvP hits still end at the pvp hit cap.
     */
    public static double lateGameScale(LivingEntity caster) {
        if (!(caster instanceof Player)) return 1.0;      // players only: a boss or mob caster keeps its old damage
        double mm = 0;
        try { mm = EnergyHelper.getMaxMagicule(caster); } catch (Throwable ignored) {}
        return LateGameScaling.factor(NUConfig.LATE_GAME_SCALING.get(), mm, NUConfig.LATE_GAME_BASELINE.get(), NUConfig.LATE_GAME_CAP.get());
    }

    /** 3.90: the grimoire spell damage multiplier (SERVER config grimoireDamageMultiplier, 1.75 by default; 1 = the old damage), applied in GrimoireBook.hurtAs / hurtDirect. */
    public static float grimoireMult() {
        double m = NUConfig.GRIMOIRE_DAMAGE_MULT.get();
        return m >= 0 && Double.isFinite(m) ? (float) m : 1f;
    }

    /**
     * Damage of an addon spell or weapon technique.
     * @param caster         who casts it (its EP scales the hit)
     * @param raw            the spell's base damage (HP)
     * @param masteryFrac    0..1 mastery of the casting skill
     */
    public static float damage(LivingEntity caster, LivingEntity target, float raw, double masteryFrac) {
        double m = Math.max(0, Math.min(1, masteryFrac));
        double r = raw * epScale(caster, m) * lateGameScale(caster) * (0.85 + 0.3 * m) * com.newuniverse.nusmp.NUGameRules.spellDamage(target.level()) * com.newuniverse.nusmp.mana.ManaLaw.powerMult(caster);   // 1.5.0: stage, Mana Zone, negative mana
        r += DamageSliver.of(raw, target.getMaxHealth(), isBoss(target));    // 1.105.0: no percentage-of-max-health damage for weapon-scale hits or bosses (it made a 400 hit take 40% of a boss, see DamageSliver)
        return cap(target, (float) r);
    }

    /**
     * Ultimate classes: the EP scaling capped by the class's epCap (SERVER config ultimateClasses.&lt;class&gt;.epCap). A null class (every unclassified caller) is the old {@link #epScale(LivingEntity)}.
     */
    public static double epScale(LivingEntity caster, UltimateClass cls) {
        if (cls == null) return epScale(caster);
        double ep = 0;
        try { ep = EnergyHelper.getMaxEP(caster); } catch (Throwable ignored) {}
        return UltimateClass.epScale(ep, NUConfig.ucEpCap(cls));
    }

    /**
     * The whole damage scaling of a classified hit: {@link #epScale(LivingEntity, UltimateClass)} times the late-game factor, never above epCap x lateGameShare (x24 Lord, x30 King, x36 God / Dragon
     * by default; an unclassified hit can reach x72 = 12 x 6). The late-game factor is the same curve {@link #lateGameScale} reads, applied once per hit (a hit takes this path or the plain one).
     */
    public static double classScale(LivingEntity caster, UltimateClass cls) {
        double ep = 0, late = 1.0;
        try { ep = EnergyHelper.getMaxEP(caster); } catch (Throwable ignored) {}
        if (caster instanceof Player) {
            double mm = 0;
            try { mm = EnergyHelper.getMaxMagicule(caster); } catch (Throwable ignored) {}
            late = LateGameScaling.factor(NUConfig.LATE_GAME_SCALING.get(), mm, NUConfig.LATE_GAME_BASELINE.get(), NUConfig.LATE_GAME_CAP.get());
        }
        return UltimateClass.totalScale(ep, NUConfig.ucEpCap(cls), late, NUConfig.ucLateShare());
    }

    /**
     * Damage of a hit from a skill of Ultimate class {@code cls}: the same law as {@link #damage(LivingEntity, LivingEntity, float, double)} with {@link #classScale} in place of the EP and late-game
     * factors. A null class, or the SERVER setting ultimateClasses.classDamage off, is exactly the plain law.
     */
    public static float damage(LivingEntity caster, LivingEntity target, float raw, double masteryFrac, UltimateClass cls) {
        if (cls == null || !NUConfig.ucClassDamage()) return damage(caster, target, raw, masteryFrac);
        double m = Math.max(0, Math.min(1, masteryFrac));
        double r = raw * EpMastery.ramp(classScale(caster, cls), m, NUConfig.EP_NEEDS_MASTERY.get()) * (0.85 + 0.3 * m) * com.newuniverse.nusmp.NUGameRules.spellDamage(target.level()) * com.newuniverse.nusmp.mana.ManaLaw.powerMult(caster);
        r += DamageSliver.of(raw, target.getMaxHealth(), isBoss(target));
        return cap(target, (float) r);
    }

    /** The same without a known caster (old call sites): mastery scaling only, then the same caps. */
    public static float damage(LivingEntity target, float raw, double masteryFrac) {
        double m = Math.max(0, Math.min(1, masteryFrac));
        return cap(target, (float) (raw * (1 + m) * com.newuniverse.nusmp.NUGameRules.spellDamage(target.level())));
    }

    /** Players: at most pvpHitCap of their max health per hit. Mobs: the config multiplier. */
    public static float cap(LivingEntity target, float r) {
        double ceiling = DamageScaleConfig.softCeiling();                          // damage scale-down: no hit of the law passes the widest ceiling (docs/damage_scale_down.md); off = no limit
        if (target instanceof Player) return (float) Math.min(Math.min(r, target.getMaxHealth() * com.newuniverse.nusmp.NUGameRules.pvpHitCap(target.level())), ceiling);
        return (float) Math.min(r * NUConfig.BAL_MOB_DAMAGE_MULT.get(), ceiling);
    }

    public static boolean isBoss(LivingEntity e) {
        return e.getType().is(Tags.EntityTypes.BOSSES) || e.getMaxHealth() >= 300;
    }

    // ------------------------------------------------------------------ cost
    /** Magicule cost: percent of max magicule, never below the floor. */
    public static double cost(LivingEntity caster, double percent, double floor) {
        return Math.max(floor, EnergyHelper.getMaxMagicule(caster) * percent / 100.0) * NUConfig.DM_COST_MULT.get();
    }

    /**
     * Ultimate classes (master guide P1 #1): the price of an addon skill cast whose old flat price is {@code flat}, the {@link #cost} idea per cast: never below the flat price, and late game the
     * class's costPercent of max magicule for a cast of costReference (the others in proportion). A null class uses plainCostPercent / plainCostReference. The flat price already carries the
     * server's cost multiplier at its call site, so it is not multiplied again here.
     */
    public static double classCost(LivingEntity caster, UltimateClass cls, double flat) {
        double mm = 0;
        try { mm = EnergyHelper.getMaxMagicule(caster); } catch (Throwable ignored) {}
        return cls == null ? UltimateClass.price(flat, mm, NUConfig.ucPlainCostPct(), NUConfig.ucPlainCostRef())
                : UltimateClass.price(flat, mm, NUConfig.ucCostPct(cls), NUConfig.ucCostRef(cls));
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
