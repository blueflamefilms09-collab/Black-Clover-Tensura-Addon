package com.newuniverse.nusmp.ultimate;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

/**
 * Why: the per-player state of Ultimate Magic that has to survive a restart: the long cooldown after a release, the short lockout after a broken cast, and a counter. Stored in the persisted player tag under its own key
 * ({@code nusmp_ultimate}), keyed by the player (rule 9: never a static "current player"); times are absolute game ticks. The running cast itself is NOT saved (a cast does not survive a restart: nothing was spent).
 */
public final class UltimatePlayer {
    private UltimatePlayer() {}

    static final String KEY = "nusmp_ultimate";

    public static CompoundTag data(Player p) {
        CompoundTag pd = p.getPersistentData();
        if (!pd.contains(Player.PERSISTED_NBT_TAG, 10)) pd.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        CompoundTag root = pd.getCompound(Player.PERSISTED_NBT_TAG);
        if (!root.contains(KEY, 10)) root.put(KEY, new CompoundTag());
        return root.getCompound(KEY);
    }

    public static long cooldownUntil(Player p) { return data(p).getLong("CooldownUntil"); }

    public static void setCooldownUntil(Player p, long tick) { data(p).putLong("CooldownUntil", tick); }

    public static long lockUntil(Player p) { return data(p).getLong("LockUntil"); }

    public static void setLockUntil(Player p, long tick) { data(p).putLong("LockUntil", tick); }

    public static int released(Player p) { return data(p).getInt("Released"); }

    public static void countRelease(Player p) { data(p).putInt("Released", Math.min(1_000_000, data(p).getInt("Released") + 1)); }

    /** Ticks of cooldown left at game time {@code now} (0 when ready). A cooldown set in a clock that was later reset (a restored world) is cut to the maximum. */
    public static long cooldownLeft(Player p, long now) {
        long left = cooldownUntil(p) - now;
        return left <= 0 ? 0 : Math.min(left, UltimateRules.MAX_COOLDOWN_TICKS);
    }
}
