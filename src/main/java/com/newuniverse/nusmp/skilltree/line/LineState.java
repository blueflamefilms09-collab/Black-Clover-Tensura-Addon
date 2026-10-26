package com.newuniverse.nusmp.skilltree.line;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * Why: the line engine has to remember small things per player between events: when an art is ready again, which stance is on, when a passive may fire again, how many hits are in a combo, when the last
 * fight was. The owner's rule is that this state is never held in a static map (a static map is shared by every player and survives the world it belonged to) but in the player's OWN persistent data,
 * the same place the older passives keep their rests ({@code HkPassives}). It sits under one compound, {@code nusmp_line}, so it cannot clash with any other system's keys, and every value is a game
 * time or a counter, so a stale entry (from before a death, a respec, a reload) is simply in the past and does nothing. Nothing here is gameplay: it is the thin typed access to those few keys, so
 * the 51 executors do not each spell a tag name. Server thread only; the compound is the live one, so a write is a save.
 */
public final class LineState {
    private LineState() {}

    static final String ROOT = "nusmp_line";
    static final String COOLDOWN = "cd.", REST = "rest.", COUNT = "n.", STAMP = "t.", STANCE = "stance", COMBAT = "combat", CASTS = "casts.", MODS = "mods", EXHAUST = "exhaust";
    /** How long after a blow dealt or taken the player counts as "in combat". */
    public static final int COMBAT_TICKS = 100;

    /** The player's own compound for this engine (created on first use). The returned tag is live. */
    public static CompoundTag root(LivingEntity e) {
        CompoundTag d = e.getPersistentData();
        if (!d.contains(ROOT, Tag.TAG_COMPOUND)) d.put(ROOT, new CompoundTag());
        return d.getCompound(ROOT);
    }

    // ---------------------------------------------------------------- generic keyed values
    public static long getLong(LivingEntity e, String key) { return root(e).getLong(key); }

    public static void putLong(LivingEntity e, String key, long v) { root(e).putLong(key, v); }

    public static int getInt(LivingEntity e, String key) { return root(e).getInt(key); }

    public static void putInt(LivingEntity e, String key, int v) { root(e).putInt(key, v); }

    public static double getDouble(LivingEntity e, String key) { return root(e).getDouble(key); }

    public static void putDouble(LivingEntity e, String key, double v) { root(e).putDouble(key, v); }

    public static String getString(LivingEntity e, String key) { return root(e).getString(key); }

    public static void putString(LivingEntity e, String key, String v) { root(e).putString(key, v); }

    public static void remove(LivingEntity e, String key) { root(e).remove(key); }

    // ---------------------------------------------------------------- the readiness of an art
    /** Game time at which the art {@code key} ({@code route#node}) may be used again (0 when it never was used). */
    public static long cooldownEnd(LivingEntity e, String key) { return getLong(e, COOLDOWN + key); }

    public static void setCooldownEnd(LivingEntity e, String key, long gameTime) { putLong(e, COOLDOWN + key, gameTime); }

    /** How many times the art was used (a small number that stands for practice and feeds the damage law's mastery term). */
    public static int casts(LivingEntity e, String key) { return getInt(e, CASTS + key); }

    public static void addCast(LivingEntity e, String key) {
        int n = casts(e, key);
        if (n < 100_000) putInt(e, CASTS + key, n + 1);
    }

    /** The mastery fraction 0..1 handed to the balance law: 100 uses is a master. */
    public static double mastery(LivingEntity e, String key) { return Math.min(1.0, casts(e, key) / 100.0); }

    // ---------------------------------------------------------------- the rest of a passive
    /** True when the passive {@code key} may act now, and starts its rest of {@code restTicks} ticks. False while it is still resting. */
    public static boolean tryRest(LivingEntity e, String key, long now, long restTicks) {
        if (now < getLong(e, REST + key)) return false;
        putLong(e, REST + key, now + Math.max(0L, restTicks));
        return true;
    }

    // ---------------------------------------------------------------- stance
    /** The key ({@code route#node}) of the stance that is on, or an empty string. */
    public static String stance(LivingEntity e) { return getString(e, STANCE); }

    public static void setStance(LivingEntity e, String key) {
        if (key == null || key.isEmpty()) remove(e, STANCE);
        else putString(e, STANCE, key);
    }

    // ---------------------------------------------------------------- evolution (3.26.0)
    /** The progress counter of a skill's evolution requirement ({@code evo.<skill id>}), the carried fraction of damage ({@code evoc.}) and the last time the client mirror was sent ({@code evom.}). */
    static final String EVO = "evo.", EVO_CARRY = "evoc.", EVO_MIRROR = "evom.";

    /** The evolution counter of this skill, 0 when it never counted (never negative). */
    public static int evolution(LivingEntity e, String skillId) { return Math.max(0, getInt(e, EVO + skillId)); }

    public static void setEvolution(LivingEntity e, String skillId, int value) { putInt(e, EVO + skillId, Math.max(0, value)); }

    // ---------------------------------------------------------------- combat
    /** Stamps "a blow was dealt or taken now". */
    public static void markCombat(LivingEntity e, long now) { putLong(e, COMBAT, now); }

    /** True within {@link #COMBAT_TICKS} ticks of the last stamp. */
    public static boolean inCombat(ServerPlayer p) {
        long last = getLong(p, COMBAT);
        long now = p.serverLevel().getGameTime();
        return last > 0 && now >= last && now - last <= COMBAT_TICKS;
    }
}
