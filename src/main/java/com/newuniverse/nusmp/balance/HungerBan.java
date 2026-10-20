package com.newuniverse.nusmp.balance;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Why: the game-side half of the owner's hunger ban (the numbers are {@link HungerLaw}). The ban itself is enforced by REMOVING the writers (nothing in the mod
 * calls {@code causeFoodExhaustion}, lowers the food level or applies the Hunger effect to a player; {@code NoHungerDrainTest} scans the source for it), so this
 * class has no tick handler and no event: it is the shared question the effect-by-id choke points ask ({@link #refuses}) and the guard the self-test
 * {@code dungeon.nohunger} uses ({@link #guard}): the first call arms a snapshot of the player's food bar, the next call compares the bar with it, writes back anything
 * our code made worse and says whether the ban held. Server thread only; a snapshot is dropped by {@link #disarm}.
 */
public final class HungerBan {
    private HungerBan() {}

    private static final Map<UUID, HungerLaw.Bar> ARMED = new ConcurrentHashMap<>();

    /** True when {@code effectId} is the Hunger effect and {@code target} is a player: such an effect is never applied. A mob may carry it (it has no food bar). */
    public static boolean refuses(String effectId, LivingEntity target) {
        return target instanceof Player && HungerLaw.isHungerEffectId(effectId);
    }

    /** Like {@link #refuses(String, LivingEntity)} for an effect holder. */
    public static boolean refuses(Holder<MobEffect> effect, LivingEntity target) {
        return target instanceof Player && effect != null && (effect.equals(MobEffects.HUNGER) || effect.value() == MobEffects.HUNGER.value());
    }

    /** One reading of a player's food bar. */
    public static HungerLaw.Bar read(Player p) {
        FoodData f = p.getFoodData();
        return new HungerLaw.Bar(f.getFoodLevel(), f.getSaturationLevel(), f.getExhaustionLevel());
    }

    /**
     * Arms the guard on the first call for this player (stores the bar, returns true). On every later call it compares the bar with the armed reading, writes
     * back anything that got worse, stores the new reading and returns whether the ban held (false = something lowered the food or saturation or raised the
     * exhaustion since the last call). Meant for tests that drive the old hunger triggers with no vanilla tick in between.
     */
    public static boolean guard(ServerPlayer p) {
        HungerLaw.Bar now = read(p);
        HungerLaw.Bar before = ARMED.put(p.getUUID(), now);
        if (before == null) return true;
        if (HungerLaw.violations(before, now).isEmpty()) return true;
        HungerLaw.Bar fixed = HungerLaw.repaired(before, now);
        FoodData f = p.getFoodData();
        f.setFoodLevel(fixed.food());
        f.setSaturation(fixed.saturation());
        f.setExhaustion(fixed.exhaustion());
        ARMED.put(p.getUUID(), fixed);
        return false;
    }

    /** The violations since the guard was last armed or checked, without repairing or re-arming (empty when the guard is not armed). */
    public static java.util.List<String> peek(ServerPlayer p) {
        HungerLaw.Bar before = ARMED.get(p.getUUID());
        return before == null ? java.util.List.of() : HungerLaw.violations(before, read(p));
    }

    /** Drops the snapshot of this player. */
    public static void disarm(ServerPlayer p) { ARMED.remove(p.getUUID()); }
}
