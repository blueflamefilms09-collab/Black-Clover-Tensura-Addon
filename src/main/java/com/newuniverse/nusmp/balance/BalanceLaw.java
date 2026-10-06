package com.newuniverse.nusmp.balance;

import com.newuniverse.nusmp.NUConfig;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.Tags;

/**
 * The balance law shared by every addon spell: this mod is a side-grade to Tensura, never a second endgame.
 * - Damage vs players: 4..14 hearts depending on mastery. Mobs: raw x config multiplier.
 * - Costs: percent of max magicule with a flat floor (big pools can't spam).
 * - Hard control: one shared 12 s cooldown per caster, diminishing returns per target, short caps.
 * - Healing: 50% max-health cap per cast, 40% effectiveness if healed in the last 20 s.
 */
public final class BalanceLaw {
    private BalanceLaw() {}

    // ------------------------------------------------------------------ damage
    /**
     * @param raw            the spell's base damage (HP)
     * @param masteryFrac    0..1 mastery of the casting skill
     */
    public static float damage(LivingEntity target, float raw, double masteryFrac) {
        if (target instanceof Player) {
            float capHearts = (float) (4 + 10 * Math.max(0, Math.min(1, masteryFrac)));
            return Math.min(raw, capHearts * 2f);
        }
        return (float) (raw * NUConfig.BAL_MOB_DAMAGE_MULT.get());
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
